package trader;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import managers.BrokerManager_T;
import managers.DatabaseManager_T;
import managers.MarketDataManager_T;
import managers.TimeManager_T;

import marketdata.MarketData_T;
import marketdata.RTData_T;
import marketdata.Symbol_T;
import accounts.Account_T;

import com.ib.controller.OrderType;
import com.ib.controller.Types.Action;
import com.ib.controller.Types.SecType;

import com.ib.client.Execution;
import com.ib.client.Order;

import dayTrader.DayTrader_T;
import trader.TraderCalculator_T;

import util.Utilities_T;
import util.dtLogger_T;


public class Trader_T {
  /* {src_lang=Java}*/

    /** Minimum number of shares a security has to trade for us to buy. */
    public static final double MIN_TRADE_VOLUME = 10000;  //SALxx was int - needs to agree w/ DB definition
    /** Minimum price for a security for us to buy it. */
    public static final double MIN_BUY_PRICE = 0.50;
//TEST--   
    /** The maximum number of positions we want to buy. */
    public static final int MAX_BUY_POSITIONS = 25;
    /** The minimum account balance we want to have. */
    public static final int MIN_ACCOUNT_BALANCE = 25000;
    
    /** References to other classes we need */
    private BrokerManager_T     brokerManager;
    private DatabaseManager_T   databaseManager;
    private MarketDataManager_T marketDataManager;
    private TimeManager_T       timeManager;
    
    private TraderCalculator_T  tCalculator;
    
    private dtLogger_T Log;  // shorthand
    
    
    public Trader_T() {
        
        brokerManager     = (BrokerManager_T) DayTrader_T.getManager(BrokerManager_T.class);
	    databaseManager   = (DatabaseManager_T) DayTrader_T.getManager(DatabaseManager_T.class);
	    marketDataManager = (MarketDataManager_T) DayTrader_T.getManager(MarketDataManager_T.class);
	    timeManager       = (TimeManager_T) DayTrader_T.getManager(TimeManager_T.class);
	    
	    Log = DayTrader_T.dtLog;
	    
        tCalculator = new TraderCalculator_T(); 
    }

 
    /**
 	 *
     * Sell all remaining holdings at the end of the day and record in our
     * Holdings DB
     * 
     * The logic is based on the fact that when a holding is sold, the sell date
     * will be entered into the DB.  Any holding without a sell date needs to be sold
     */
    public void liquidateHoldings()
    {
	
    	// for development only
    	Date date;
    	if (!DayTrader_T.d_useSimulateDate.isEmpty())
    		date = timeManager.getCurrentTradeDate();
    	else
    		date = timeManager.TimeNow();
    	
    	//"SELECT symbolId, buy_price FROM $tableName where sell_date is NULL";
        Session session = databaseManager.getSessionFactory().openSession();
        
        Criteria criteria = session.createCriteria(Holding_T.class)
            .add(Restrictions.isNull("sellDate"));


        @SuppressWarnings("unchecked")
        List<Holding_T> holdingData = criteria.list();
        
        session.close();
        
        if (holdingData.size() == 0)
        {
        	Log.println("There are no remaining holdings to sell at the end of the day");
        	return;
        }
                
       Log.println("There are "+holdingData.size()+" remaining Holdings to sell at the end of the day:");
            
       Iterator<Holding_T> it = holdingData.iterator();
       while (it.hasNext()) {
           Holding_T holding = it.next();

           // this is where the sell should be executed
           // if we get immediate confirmation, use the actual sell price, not EOD price
           // if we dont get immediate confirmation that all share sells were executed, we'll
           // need alternate logic
           
           // update our Holdings DB
           Symbol_T symbol = holding.getSymbol();
           double price  = marketDataManager.getEODPrice(symbol);	// the price now
           Date sellDate = date;
           
           holding.setSellDate(sellDate);
           holding.setSellPrice(price);
           
           // for info
           double buyPrice = getBuyPrice(symbol.getId());
       	
           String net = "EVEN";
           if (price > buyPrice) { net = "GAIN"; }
           if (price < buyPrice) { net = "LOSS"; }

           double delta = price - buyPrice;
           delta = Utilities_T.round(delta);

           Log.println("*** SELL " +symbol.getId()+ " at a " +net+" of $"+delta+" ("+price+" : "+buyPrice+") at "+date.toString()+" ***"); 

           //holding.insertOrUpdate(); //SAL why dont either of these worK??? TODO
           // holding.update()
           
           // this does....
           holding.updateSellPosition(symbol.getId(), price, sellDate);

       }
       Log.newline();

    }

    /**
     * Determine if we should sell - trailing sell algorithm
     * 
     */
    public void shouldWeSell(List<RTData_T> rtData)
    {
    	// for development only
    	Date date;
    	if (!DayTrader_T.d_useSimulateDate.isEmpty())
    		date = timeManager.getCurrentTradeDate();
    	else
    		date = timeManager.TimeNow();

    	//for each holding
        Iterator<RTData_T> it = rtData.iterator();
        while (it.hasNext()) {
        	RTData_T data = it.next();
        	
        	String symbol = data.getSymbol();		// TODO change RT table to symId!!
        	Symbol_T sym = new Symbol_T(symbol);
        	
        	// TODO: we should check if its already sold
        	
        	double price = data.getPrice();			// current RT price
        	
        	double buyPrice = getBuyPrice(sym.getId());


			// determine if we should sell using a hard stop loss limit
			// and trailing sell algorithm to maximize gain while limiting
			// losses and executing market orders to ensure fulfillment
	 		double per_incr  = .02;
	 		double per_decr  = .02;

	 		// round real time price to 3 digits
	 		// price = int(price*1000 + 0.5)/1000;


	 		//hard stop to limit loss
	 		double lower_limit = buyPrice - (buyPrice * per_decr);

	 		// we'll use sell_price for now to indicate if the initial upper limit has been reached
	 		// it is NULL to start (not reached), then it contains the sell threshold after we've reached
	 		// the initial limit
	 		double initial_upper_limit = buyPrice + (buyPrice * per_incr);

	 		double upper_limit = getUpperSellLimit(sym.getId());
	 		if (upper_limit == 0) { upper_limit = initial_upper_limit; }


	 		boolean sell = false;

//Log.println("\n$symbol price= $price at $date  range for trail: $lower_limit - $upper_limit"; }

	 		// hard stop - limit losses
	 		if (price < lower_limit)
	 		{
	 			sell = true; 
	 		}

	 		// maximize profit by adjusting sell upwards, but sell if it falls below upper threshold     
	 		// this makes sure we at least fall within the minimum profit level
	 		else
	 		{
	 			// if the initial upper limit has already been reached, sell if it falls below, increase upper limit if above
	 			if (upper_limit != initial_upper_limit)
	 			{
	 				if (price >= upper_limit)
	 				{
//if ($_dbg) { print DBGFILE "\n$symbol: Adjusting upper price limit from $upper_limit to $price"; }
	 					setUpperSellLimit(sym.getId(), price);
	 				}
	 				else   // we'll tolerate a 1/2% deviation
	 				{
	 					double upper_threshhold = upper_limit - (upper_limit * .005);
	 					if (price <= upper_threshhold)
	 					{
//if ($_dbg) { print DBGFILE "\n**SELL (gain)** price $price fell below .005% tolerance: $upper_threshhold (initial: $initial_upper_limit)"; }
	 						sell = true;
	 					}
	 				}      
	 			}
	 
	 			//otherwise check if we've gone above the new upper limit, and adjust higher
	 			else if (price > upper_limit)
	 			{
//if ($_dbg) { print DBGFILE "\n$symbol: Adjusting upper price limit from $upper_limit to $price"; }
	 				setUpperSellLimit(sym.getId(), price);
	 			}
	 		}

	 		if (sell)
	 		{
	 			// TODO execute sell order

	 			//update the Holdings Table with this info
        		Holding_T h = new Holding_T();	// just a handle
	 			if (h.updateSellPosition(sym.getId(), price, date) != 0)
	 			{
	 				String net = "EVEN";
	 				if (price > buyPrice) { net = "GAIN"; }
	 				if (price < buyPrice) { net = "LOSS"; }

	 				double delta = price - buyPrice;
	 				delta = Utilities_T.round(delta);

	 				Log.println("*** SELL " +sym.getId()+ " at a " +net+" of $"+delta+" ("+price+" : "+buyPrice+") at "+date.toString()+" ***"); 

// print LOGFILE "\n".timeStamp().":<1> *** SELL $symbol at a $net of \$$delta ($price : $buy_price)  at $date ***";
//if ($_dbg) { print DBGFILE "\n".timeStamp().":<1>  *** SELL $symbol at a $net of \$$delta ($price : $buy_price) at $date ***"; }
//report the reason if its a gain
//if ($_dbg) { if ($net eq "GAIN") { print DBGFILE "\n**SELL (gain)** price $price fell below .005% tolerance: $upper_threshhold (initial: $initial_upper_limit)"; } }

	 			}  // only log if updated
	 		}  // if sold
        	
        }  // next holding

    }    

    /**
     * for Trailing Sell algorithm, get the max price so far
     */
    private double getUpperSellLimit(long symId)
    {
    	Date date = timeManager.getPreviousTradeDate();

    	//"SELECT sell_price FROM Holdings where symbol = \"$symbol\" and DATE(buy_date) = \"$date\"";
        Session session = DatabaseManager_T.getSessionFactory().openSession();
        Criteria criteria = session.createCriteria(Holding_T.class)
                .add(Restrictions.eq("symbolId", symId))
                .add(Restrictions.ge("buyDate", date));

        @SuppressWarnings("unchecked")
        List<Holding_T> holdingData = criteria.list();
                
        session.close();
        
        if (holdingData.size() != 1) {
           Log.println("ERROR: getUpperSellLimit() cant get upper limit for SymbolId:" + symId + " (" + holdingData.size() + ")");
           return 0;
        }

        Double price = holdingData.get(0).getSellPrice();
        if (price==null) price = 0.0;
        
        return price; 
    }
    
    /**
     * for Trailing Sell algorithm, set the max price so far
     * 
     * @return nrows updated (1 or 0)
     */
    private int setUpperSellLimit(long symId, double price)
    {
    	Date date = timeManager.getPreviousTradeDate();

    	//UPDATE Holdings SET sell_price = $price where symbol = \"$symbol\" and DATE(buy_date) = \"$date\"";
       	Session session = DatabaseManager_T.getSessionFactory().openSession();
        Transaction tx = null;
        
        int nrows = 0;
        
        try {
            tx = session.beginTransaction();
            
          	String hql = "UPDATE trader.Holding_T " +
           			"SET sellPrice = :price " +
           			"WHERE symbolId = :sym AND buyDate >= :date";
           	Query query = session.createQuery(hql);
           	query.setDouble("price", price);
           	query.setParameter("sym", symId);
           	query.setTimestamp("date", date);

        	nrows = query.executeUpdate();
        	
            tx.commit();
        } catch (HibernateException e) {
            //TODO: for now just print to stdout, we'll change this to a log file later
            e.printStackTrace();
            if (tx != null) tx.rollback();
            throw e;
        } finally {
            session.close();
        }
        
        return nrows;
    }
   
    
    /**
     * update our Holdings db with buy position of the the stocks we want to buy today
 	 * buy price, #of share, [total $ amt]
	 * calculate how much of each stock to buy
	 * volume is declared as INT so only full shares are bought
     */
    public synchronized void updateBuyPositions(List<Symbol_T> losers)
    {
    	// how much $$ do we have to work with?
    	// use equal dollar amount for each holding (rounded to full shares)
    	double totalCapital = tCalculator.getCapital();
    	double buyTotal = totalCapital/Trader_T.MAX_BUY_POSITIONS;

        Iterator<Symbol_T> it = losers.iterator();
        while (it.hasNext()) {
            Symbol_T symbol = it.next();
      
            double buyPrice = marketDataManager.getEODPrice(symbol);
            int buyVolume = (int)(buyTotal/buyPrice);
            double adjustedBuyTotal = buyVolume * buyPrice;
    	
            Session session = databaseManager.getSessionFactory().openSession();

            // updates must be within a transaction
            Transaction tx = null;
        
            try {
            	tx = session.beginTransaction();

            	String hql = "UPDATE trader.Holding_T " +
            			"SET buyPrice = :buyPrice, volume = :buyVolume " +
            			"WHERE symbolId = :sym AND buyDate >= :date";
            	Query query = session.createQuery(hql);
            	query.setDouble("buyPrice", buyPrice);
            	query.setInteger("buyVolume", buyVolume);    
            	query.setParameter("sym", symbol.getId());
            	query.setDate("date", timeManager.getCurrentTradeDate());

            	int n = query.executeUpdate();
                 
            	tx.commit();
            } catch (HibernateException e) {
            	//TODO: for now just print to stdout, we'll change this to a log file later
            	e.printStackTrace();
            	if (tx != null) tx.rollback();
            } finally {
            	session.close();
            }
            
        }
        
    }

    /**
    * get the buy price for this symbol from the Holdings Table.  Get the last
    * holdings by previous date
    */
    
    public Double getBuyPrice(long symbolId)
    {

    	// get the buy price from the Holdings table from the day before
      	Date date = timeManager.getPreviousTradeDate();
      	
      	//"SELECT buy_price FROM $tableName WHERE symbol = \"$symbol\" AND DATE(buy_date) = \"$date\"";
        Session session = databaseManager.getSessionFactory().openSession();
        
        Criteria criteria = session.createCriteria(Holding_T.class)
        	.add(Restrictions.eq("symbolId", symbolId))
        	.add(Restrictions.ge("buyDate", date));

        @SuppressWarnings("unchecked")
        List<Holding_T> holdingData = criteria.list();
        
        session.close();
        
        if (holdingData.size() != 1)
        {
        	Log.println("WARNING: getBuyPrice returns empty for SymbolId:" + symbolId  + " (" + holdingData.size() + ")");
        	return 0.00;
        }

        double buyPrice = holdingData.get(0).getBuyPrice();

        return buyPrice;
    }
    

    
    /***************** Trade Execution ************************/
    
    /**
     * Create a market buy or sell order for the Holding
     *  The contract and order are created and a new orderId is assigned
     * 
     * @param Holding - the Holding to buy or sell
     * @param String "BUY" or "SELL"
     * 
     * @return Holding - the holding with updated order data
     */
    public Holding_T createMarketOrder(Holding_T holding, String buyOrSell ) {
        
    	Account_T acct = brokerManager.getAccount();
    	// TODO: check for valid acct - updateAccount should be the first thing done
    	
        //get the next available orderID
        int orderId = brokerManager.reqNextValidId();
        // TODO - this is really bad
        if (orderId == 0)
        	System.out.println("BAD ERROR orderId is 0!");
 
        int existingOrderId = holding.getOrder().m_orderId;
        if (existingOrderId < orderId)
        {
        	System.out.println("Holding "+holding.getId()+" has already been placed with orderId "+existingOrderId);
        	// two options: resubmit (eg 'modify' with existing order Id, or return) We'll do A for now
        	orderId = existingOrderId;
        }
        
        Holding_T order = new Holding_T(orderId);
        
        // copy other Holdings fields that we'll need
        // TODO: create a Holdings copy constructor
        order.setId(holding.getId());
        order.setSymbol(holding.getSymbol());
        //order.setVolume(holding.getVolume());
        
        order.setClientId(acct.getClientId());
        
        // the actual time of this transaction
        // SALxx no? the date will be filled in when the order completes (per N)
        //  do we really need this... remaining = 0 should be the trigger
        // setting the date also means the trade is in progress
        Date time = new Date(System.currentTimeMillis());
        if (buyOrSell.equalsIgnoreCase(Action.BUY.toString()))
        	order.setBuyDate(time);
        else
        	order.setSellDate(time);

        
        //calculate the amount in dollars that we're allowed to buy
        // TODO: we should do this before we get here and have it stored in the DB
        double buyAmount = (acct.getBalance() - MIN_ACCOUNT_BALANCE) / MAX_BUY_POSITIONS;  

            
        // the order type and action
        order.getOrder().m_action = buyOrSell;
        
        //submit these orders as market-to-limit orders
        //order.getOrder().m_orderType = OrderType.MTL.toString();
        
        //SALxx - lets try market first
        order.getOrder().m_orderType = OrderType.MKT.toString();
        
        //get the latest price of this symbol - IB doesnt provide this for our acct!
        //brokerManager.reqSymbolSnapshot(symbol);
        //MarketData_T snapshot = brokerManager.getSymbolSnapshot();
        //order.getOrder().m_totalQuantity = (int) Math.floor(buyAmount / snapshot.getAskPrice());
        
        // so, we use whats in our Holdings, which was the most recent price from TD
        // SALxx should we be using ask price? (for market we dont need to specify a price at all
        order.getOrder().m_totalQuantity =  holding.getVolume();
        
//SALxx--TEST
        //order.getOrder().m_totalQuantity = 1;
//SALxx--TEST
        
        // SALxx are these others necessary?
        order.getOrder().m_transmit = true;
            
        // TODO: these should be the defaults
        order.getOrder().m_lmtPrice = 0;
        order.getOrder().m_auxPrice = 0;
        order.getOrder().m_allOrNone = false;
        order.getOrder().m_blockOrder = false;
        order.getOrder().m_outsideRth = false;
           
        // the contractId must be 0 (constructor default)
        // order.setContractId(0);
        order.getContract().m_symbol = holding.getSymbol().getSymbol();
        order.getContract().m_exchange = "SMART";
        order.getContract().m_primaryExch = "ISLAND";       
        //order.getContract().m_primaryExch = holding.getSymbol().getExchange();
        
        order.getContract().m_secType = SecType.STK.toString();  
        order.getContract().m_currency = "USD";
 
        return order;
    }


// TODO: not used
    /**
     * Get the list of order request Ids that are in submitted state
     * @return
     */
    public ArrayList<Integer> getSubmittedOrders() {
        
    	ArrayList<Integer> reqIds = new ArrayList<Integer>();
    	
    	List<Holding_T> holdings = databaseManager.getSubmittedOrders();
    	
    	Iterator<Holding_T> it = holdings.iterator();

        while (it.hasNext()) {
        	Holding_T holding = it.next();
        	reqIds.add((Integer)holding.getOrderId());
        }

    	
    	return reqIds;
    }
    
  

    public boolean getOutstandingOrders()
    {
    	// 1. get all holdings from the DB that have a status of Submitted or PreSubmitted
    	// 2. for each unfilled holding, see if the order was executed while we were away
    	// 3. reconcile with what IB reports as open and filled (executed)
    	// 4a. If the were filled, update the DB
    	// 4b. if they really are still open, put this info into our brokers holdings map first.
    	//     openOrder will confirm and update as necessary

    	// see which holdings in our DB are still in the 'not filled' state   	
    	//ArrayList<Integer> submittedOrders = getSubmittedOrders();
    	List<Holding_T> holdings = databaseManager.getSubmittedOrders();
    	
    	Iterator<Holding_T> hit = holdings.iterator();
    	while (hit.hasNext())
    	{
    		Holding_T holding = hit.next();
    		
    		int reqId = holding.getOrderId(); 		
    	
    		// see if any orders in the submitted state have been filled (executed)
    		List<Execution> executedOrders = brokerManager.reqExecutions(reqId, 2000);
    	
    		// do a sanity check - check the number of shares
    		// if its correct, update the DB as "Filled" (buy/sell?)
    		Iterator<Execution> it = executedOrders.iterator();
    		while (it.hasNext()) {
    			Execution executedOrder = it.next();
    			System.out.println("getOustandingOrders reqId: " + reqId + " exec price $"+executedOrder.m_price +"/$"+ executedOrder.m_avgPrice +
    				" shares: "+executedOrder.m_shares + " at "+executedOrder.m_time);
        
           		if (holding.getVolume() == executedOrder.m_shares) {
            		System.out.println("number of shares match woohoo");
            		
            		holding.setOrderStatus("Filled");
            		holding.setFilled(executedOrder.m_shares);
            		holding.setRemaining(0);
            		holding.setAvgFillPrice(executedOrder.m_price);  // if buy, actualBuyPrice will be updated when persisted
            		
            		holding.updateMarketPosition();
            	}
           		else {
           			// since its still not filled, it needs to be sent to the broker
           			// so it can be updated as necessary when OrderStatus is called
           			// (initiated below)
           	    	brokerManager.updateHoldings(holding);          			
           		}
            }  // next executed order       
        } //next holding
    	    	
    	// request all our open orders from IB
    	// this may be overkill... maybe we just need to request it
    	// and not care about the returned data (it will be updated asynchronously when orderStatus is called)
    	// but may be good to wait for initialization
    	System.out.println("Reqesting Open Orders...");
    	List<Order> openOrders = brokerManager.reqOpenOrders(2000);
 
    	// sanity check - the lists should agree TODO

        if (openOrders.size() > 0) {
        	Iterator<Order> oit = openOrders.iterator();
        	int n = openOrders.size();
            while (oit.hasNext()) {
                Order o = (Order) oit.next();
                System.out.println("Open Order: " +o.m_orderId+" "+o.m_action);
            }
        } else
            System.out.println("There are no currently open orders");
 	
    	return true;
    }
    
    


//TEST===========================
    public void TestCode() {
    	
if (1==0) {

		brokerManager.updateAccount();
    	Account_T acct = brokerManager.getAccount();
    		        
        
    	//Symbol_T s = new Symbol_T(12225);		//BIOF
    	Symbol_T s = new Symbol_T(7967);		// IBM
    	if (brokerManager.reqSymbolSnapshot(s))
    	{
    		MarketData_T md = brokerManager.getSymbolSnapshot();
    		System.out.println("data for "+s.getSymbol()+" = "+md.getLastPrice());          
        }
        else System.out.println("error getting IB snapshot");
}

if (1==0) {
		//TestBuyOrSell("BUY");
    	//getSubmittedOrders();    // from our DB
    	getOutstandingOrders();  // from IB

    	// catch any stragglers
    	try { Thread.sleep(10000); }
        catch (InterruptedException e) { e.printStackTrace(); }
}

    }


    public void TestBuyOrSell(String buyOrSell)
    {
    	System.out.println("***TEST BuyOrSell***");
    	
    	brokerManager.updateAccount();  // we need an account to start

        Long id = new Long(2501);		// get this holdingId from the DB (CRDChen)
        
        Session session = databaseManager.getSessionFactory().openSession();
        
        Criteria criteria = session.createCriteria(Holding_T.class)
            .add(Restrictions.eq("id", id));

        @SuppressWarnings("unchecked")
        List<Holding_T> holdingData = criteria.list();
        
        session.close();
        
        Holding_T holding = holdingData.get(0);

        Symbol_T symbol = holding.getSymbol();
        int existingOrderId = holding.getOrder().m_orderId;

       
        Holding_T order = createMarketOrder(holding, buyOrSell);
        brokerManager.placeOrder(order);
        
        /*** controlled test
    	brokerManager.orderStatus(18, "Filled", 20, 80,
    			13.14, 707, 1414, 13.18,
    			1, "no reason");
    	***/
    }
//TEST===============
    
}