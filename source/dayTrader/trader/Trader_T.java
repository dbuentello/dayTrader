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

import com.ib.controller.OrderStatus;
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
    public static final int MAX_BUY_POSITIONS = 5;				//SALxx TEST
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
     * Initialize the Trader class by getting our account information and
     * ensuring we have a valid next Order Id
     * 
     * @return true if successfully initialized - we cant do much more if its not!
     */
    public boolean init() {
    	
if (DayTrader_T.d_useIB) {
    	brokerManager.updateAccount();
    	
    	// make sure we have a valid NextOrderId
    	// if we still cant get it, we return false
    	if (!brokerManager.haveValidOrderId()) {
    		return (brokerManager.reqNextValidId() != 0);
    	}
}
		// we're good to go
		return true;

    }
 
    /**
 	 *
     * Sell all remaining holdings at the end of the day and record in our
     * Holdings DB.  Those in progress are not included
     * 
     * The logic is based on the fact that when as a holding is sold during the day
     * the sell date is entered into the DB.
     * Any holding without a sell date needs to be sold at the end of the day
     * 
     * @return the number of orders placed to sell.  We dont know if they
     *         were actually sold - we'll have to check the order status
     */
    public int liquidateHoldings()
    {
    	// simdate is for development only TODO: date or timestamp?
    	// SALxx is this necessary getCurrentTradeDate may return what we need
    	// but check it to make sure it returns timestamp and note just Date!!!
    	Date date;
    	if (!DayTrader_T.d_useSimulateDate.isEmpty())
    		date = timeManager.getCurrentTradeDate();
    	else
    		date = timeManager.TimeNow();
    	
    	Date buyDate = timeManager.getPreviousTradeDate();
    	
    	// retrieve unsold holdings
    	int nOrdersPlaced = 0;
    	
        Session session = databaseManager.getSessionFactory().openSession();

        Criteria criteria = session.createCriteria(Holding_T.class)
        	.add(Restrictions.between("buyDate", buyDate, Utilities_T.tomorrow(buyDate)))
            .add(Restrictions.isNull("sellDate"));

        @SuppressWarnings("unchecked")
        List<Holding_T> holdingData = criteria.list();
        
        session.close();
  
        
        if (holdingData.size() == 0)
        {
        	Log.println("There are no remaining holdings to sell at the end of the day");
        	return 0;
        }
                
        Log.println("There are "+holdingData.size()+" remaining Holdings to sell at the end of the day:");
            
        Iterator<Holding_T> it = holdingData.iterator();
        while (it.hasNext()) {
        	Holding_T holding = it.next();

        	// first, determine if we can sell this holding - the buy had to be complete
        	if (!holding.isOwned()) {
        		Log.println("[ATTENTION] Holding "+holding.getSymbolId()+" can not be sold "+
        				holding.getVolume()+"/"+holding.getFilled());
        		continue;
        	}
        	
        	// update these two fields... the date is a trigger field, desired price is for stats
        	// (and place order if we ever do a LMT order)
            double desiredPrice  = databaseManager.getCurrentBidPrice(holding.getSymbol());	// the price now
	           
            holding.setSellDate(date);
            holding.setSellPrice(desiredPrice);
            //holding.insertOrUpdate(); //SAL why dont either of these worK??? TODO
            // holding.update()
        
            // this does....
            if (holding.updateSellPosition(holding.getSymbolId(), desiredPrice, date) != 1)
	        	System.out.println("[ERROR] sell order date not updated in DB");


        	// this is where the sell is executed
            // if we get immediate confirmation, use the actual sell price, not EOD price
            // if we dont get immediate confirmation that all share sells were executed, we'll
            // need alternate logic
            
            Log.println("Placing SELL order for " + holding.getSymbolId() + " ("+holding.getSymbol().getSymbol()+") ");
        	
if (DayTrader_T.d_useIB) {
			// update the holding with the order and contract
			holding = createMarketOrder(holding, Action.SELL);

			Log.println("OrderId: "+holding.getOrderId2());
			
			if (holding.getOrderId2() == 0)
			{
				Log.println("[BAD ERROR] Sell order not placed - null OrderId");
   			    continue;
			}
   			
			// place a sell order
	        brokerManager.placeOrder(holding);



} else {
			// fake it... set the status to filled, etc order id will be -1 to 
			// indicate simulation, and other ids will be null
			holding.setOrderId(-1);
			holding.setOrderStatus("Filled");
	        holding.setRemaining(0);
	        holding.setAvgFillPrice(holding.getSellPrice());
	        
	        if (holding.updateOrderPosition() == 0) {
	        	System.out.println("[WARNING] order status not updated in DB");
	        	continue;
	        }
}

			nOrdersPlaced++;
			
        }  // next holding


        return nOrdersPlaced;
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
        	
 			// retrieve this holding
 			Holding_T holding = databaseManager.getCurrentHolding(sym.getId(), timeManager.getPreviousTradeDate());

        	// check if its already sold or sell has been initiated
        	if (holding.isSelling() || holding.isSold())
        		continue;
        	
        	//double price = data.getPrice();			// current RT price
        	double price = data.getBidPrice();			// current RT Bid (sell) price
        	        	
        	//TODO: use actualBuyPrice
        	// shouldnt this already be in Holdings_T?
        	double buyPrice = getBuyPrice(sym.getId()); // our holdings buy (ask) price


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
	        	// first, determine if we can sell this holding - the buy had to be complete
	        	if (!holding.isOwned()) {
	        		Log.println("[ATTENTION] Holding "+holding.getSymbolId()+" can not be sold "+
	        				holding.getVolume()+"/"+holding.getFilled());
	        		continue;
	        	}
	        	
	        	// update these two fields... the date is a trigger field, desired price is for stats
	            holding.setSellDate(date);
	            holding.setSellPrice(price);		// desired price - may be different when the trade is executed

	 			if (holding.updateSellPosition(sym.getId(), price, date) != 1) {
	 				Log.println("[ERROR] sell order date not updated in DB");
	 				continue;
	 			}
	
	        	// this is where the sell is executed
	            // if we get immediate confirmation, use the actual sell price, not EOD price
	            // if we dont get immediate confirmation that all share sells were executed, we'll
	            // need alternate logic
	            
	            Log.println("Placing SELL order for " + holding.getSymbolId() + " ("+holding.getSymbol().getSymbol()+") ");
	        	
if (DayTrader_T.d_useIB) {
				// update the holding with the order and contract
				holding = createMarketOrder(holding, Action.SELL);

				Log.println("OrderId: "+holding.getOrderId2());
				
				if (holding.getOrderId2() == 0)
				{
					// TODO:  real bad
					Log.println("[ERROR] Real bad - null order id!");
	   			    continue;
				}
				
	   			// add the desired sell price (so it will be persisted)
	   			holding.setSellPrice(price);
	   			
				// place a sell order
		        brokerManager.placeOrder(holding);

}
else
{
				// fake it... set the status to filled, etc order id will be -1 to 
				// indicate simulation, and other ids will be null (we could keep a simId variable in this class)
				holding.setOrderId2(-1);
				holding.setOrderStatus("Filled");
		        holding.setRemaining(0);
		        holding.setAvgFillPrice(holding.getSellPrice());

		        
		        if (holding.updateOrderPosition() == 0)
		        	System.out.println("[WARNING] order status not updated in DB");
		  
}
	 				
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
//SALxx                .add(Restrictions.ge("buyDate", date));
                .add(Restrictions.between("buyDate", date, Utilities_T.tomorrow(date)));

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
 	 * buy price, #of share, [from total $ amt]
	 * calculate how much of each stock to buy
	 * volume is declared as INT so only full shares are bought
	 * initialized filled to 0 (DB default) and remaining to volume
	 * (fill is counted up on buy, remaining is counted down on sell)
     */
    public synchronized void updateBuyPositions(List<Symbol_T> losers)
    {
    	// how much $$ do we have to work with?
    	// use equal dollar amount for each holding (rounded to full shares)
        
        //calculate the amount in dollars that we're allowed to buy
        // TODO: we should do this before we get here and have it stored in the DB
        //double buyAmount = (acct.getBalance() - MIN_ACCOUNT_BALANCE) / MAX_BUY_POSITIONS;  

 
    	double totalCapital = tCalculator.getCapital();
    	double buyTotal = totalCapital/Trader_T.MAX_BUY_POSITIONS;

        Iterator<Symbol_T> it = losers.iterator();
        while (it.hasNext()) {
            Symbol_T symbol = it.next();
      
            // get the current ask/buy price from EOD
            double buyPrice = databaseManager.getEODAskPrice(symbol);
            int buyVolume = (int)(buyTotal/buyPrice);
            double adjustedBuyTotal = buyVolume * buyPrice;
    	
            // TODO: put this as a Holding_T method updateBuyPosition(price, vol)
            Session session = databaseManager.getSessionFactory().openSession();

            // updates must be within a transaction
            Transaction tx = null;
        
            try {
            	tx = session.beginTransaction();

            	String hql = "UPDATE trader.Holding_T " +
            			"SET buyPrice = :buyPrice, volume = :buyVolume, " +
            			"remaining = :buyVolume " + 
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
     * Buy todays holdings
     * 
     * @return the number of holdings we placed orders for ((better be MAX_HOLDINGS)
     *         (we dont know if they actually were bought tho - we'll need to check the order status
     */
    public int buyHoldings() {
    	
    	List<Holding_T> holdings = databaseManager.getCurrentHoldings(timeManager.getCurrentTradeDate());
 
        Iterator<Holding_T> it = holdings.iterator();
        while (it.hasNext()) {
            Holding_T holding = it.next();

            Log.print("[DEBUG] Placing BUY order for " + holding.getSymbolId() + " ("+holding.getSymbol().getSymbol()+") ");
            
if (DayTrader_T.d_useIB) {
   			// update the holding with the order and contract
   			holding = createMarketOrder(holding, Action.BUY);
    		
   			Log.println("OrderId: "+holding.getOrderId());
   			
   			// place a sell order
   	        brokerManager.placeOrder(holding);   	    	
 	        
} else {
			// simulate the fields IB would fill in..
			holding.setOrderId(-1);
			holding.setActualBuyPrice(holding.getBuyPrice());
			holding.setFilled(holding.getVolume());
			holding.setOrderStatus("Filled");
			holding.updateOrderPosition();
	
			Log.newline();
}
            
        }  // next holding
        
        return holdings.size();
        
    }
    
    
    /**
    * get the buy price for this symbol from the Holdings Table.  Get the last
    * holdings by previous date
    */
    
    // TODO: change to actualBuyPrice
    public Double getBuyPrice(long symbolId)
    {

    	// get the buy price from the Holdings table from the day before
      	Date date = timeManager.getPreviousTradeDate();
      	
      	//"SELECT buy_price FROM $tableName WHERE symbol = \"$symbol\" AND DATE(buy_date) = \"$date\"";
        Session session = databaseManager.getSessionFactory().openSession();
        
        Criteria criteria = session.createCriteria(Holding_T.class)
        	.add(Restrictions.eq("symbolId", symbolId))
//SALxx        	.add(Restrictions.ge("buyDate", date));
        	.add(Restrictions.between("buyDate", date, Utilities_T.tomorrow(date)));
        
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
     * @return Holding - the holding with updated order/contract data
     */
    public Holding_T createMarketOrder(Holding_T holding, Action buyOrSell ) {
        
    	Account_T acct = brokerManager.getAccount();
    	
        //get the next available orderID
        int orderId = brokerManager.getNextOrderId();
    	// Major TODO - this is really bad and should never happen now
        // (except maybe if we get disconnected?)
        if (orderId == 0)
        	Log.println("[BAD ERROR] orderId is 0!");
 
    	Holding_T order = new Holding_T(holding);	

    	// complete the order info
    	order.getOrder().m_clientId = acct.getClientId();
        
        // the actual time of this transaction
        // the date will be modified as the order completes
        // setting the date also means the trade is in progress
    	// there are two distinct orderIds - one for buy, 2 for sell
    	// Holdings_T only has the current Order, but the DB has both
        Date time = new Date(System.currentTimeMillis());
        if (buyOrSell == Action.BUY) {
        	order.setBuyDate(time);
        	order.setOrderId(orderId);
        }
        else {
        	order.setSellDate(time);
        	order.setOrderId2(orderId);
        }

           
        // the order type and action
        order.getOrder().m_action = buyOrSell.toString();
        
        //submit these orders as market-to-limit orders
        //order.getOrder().m_orderType = OrderType.MTL.toString();
        
        // lets try market first
        order.getOrder().m_orderType = OrderType.MKT.toString();
        
        // we use whats in our Holdings, which was the most recent ask price from TD
        // (for market we dont need to specify a price at all)
        order.getOrder().m_totalQuantity =  order.getVolume();
        
       
        // SALxx are these others necessary?
        order.getOrder().m_transmit = true;
            
        // TODO: these should be the defaults
        order.getOrder().m_lmtPrice = 0;
        order.getOrder().m_auxPrice = 0;
        order.getOrder().m_allOrNone = false;
        order.getOrder().m_blockOrder = false;
        order.getOrder().m_outsideRth = false;
         
        // now complete the contract info
        // the contractId must be 0 (constructor default)
        // order.setContractId(0);
        order.getContract().m_symbol = order.getSymbol().getSymbol();
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
    
  
    /**
     * get all outstanding orders as defined as not filled in the DB
     * 
     * @return the number of remaining outstanding orders
     */
    public int getOutstandingOrders()
    {
    	// 1. get all holdings from the DB that have a status of Submitted or PreSubmitted
    	//       TODO: PendingSubmit...InActive?
    	// 2. for each unfilled holding, see if the order was executed while we were away
    	// 3. reconcile with what IB reports as open and filled (executed)
    	// 4a. If the were filled, update the DB
    	// 4b. if they really are still open, put this info into our brokers holdings map first.
    	//     openOrder will confirm and update as necessary

    	
    	// see which holdings in our DB are still in the 'not filled' state   	
    	//ArrayList<Integer> submittedOrders = getSubmittedOrders();
    	List<Holding_T> holdings = databaseManager.getSubmittedOrders();
    	
    	Log.println("[DEBUG] There are "+holdings.size() + " outstanding in DB");
    	if (holdings.size() != 0) { Log.println("...checking execution status..."); } else { Log.newline(); }
    	
    	// we'll return this number
    	int nOpen = holdings.size();
    	
		// see if any orders in the submitted state have been filled (executed)
    	
    	// request the executed orders from IB
    	// will contain all the executed orders for TODAY
		// can return multiples for partial fills
       	// TODO probably dont have to do this if there's nothing in the submitted state
    	// (particularly since the hit loop wont be executed)

    	int reqId = 1;		// an arbitraryId, wait 5 seconds
		List<Execution> executedOrders = brokerManager.reqExecutions(reqId, 5000);

    	Iterator<Holding_T> hit = holdings.iterator();
    	while (hit.hasNext())
    	{
    		Holding_T holding = hit.next();
    	
    		// match em with the executed orders
    		Iterator <Execution> e = executedOrders.iterator();
    		while (e.hasNext())
    		{
    			Execution executedOrder = e.next();    	
    		
    			// could be a buy or sell order
    			if (executedOrder.m_orderId == holding.getOrderId() ||
    				executedOrder.m_orderId == holding.getOrderId2())
    			{
    				// check the cumulative number of shares executed
    				// if its correct, update the DB as "Filled" (buy/sell?)

    				Log.println("[DEBUG] filled executed order for OrderId "+executedOrder.m_orderId+" (execId:"+executedOrder.m_execId+") "+
    						" exec price $"+executedOrder.m_price +"/$"+ executedOrder.m_avgPrice +
    						" shares: "+executedOrder.m_shares+"/"+executedOrder.m_cumQty + " at "+executedOrder.m_time);
        
    				if (holding.getVolume() == executedOrder.m_cumQty) {
    					
    					Log.println("[DEBUG] number of shares match woohoo...updating DB");
            		
    					holding.setOrderStatus(OrderStatus.Filled.toString());
    					// if its executed - its executed in its entirety (TOD0 - right?)
    					holding.setFilled(executedOrder.m_cumQty);
    					holding.setRemaining(0);
    					
    					// TODO: add these dates from execute
    					if (holding.isBuying() || holding.isOwned()) {
    						// holding.setBuyDate(executedOrder.m_time);
    						holding.setBuyDate(timeManager.TimeNow());
    						holding.setActualBuyPrice(executedOrder.m_avgPrice);
    					}
    					else {
    						// holding.setSellDate(executedOrder.m_time);
    						holding.setSellDate(timeManager.TimeNow());
    						holding.setAvgFillPrice(executedOrder.m_avgPrice);
    					}

    					holding.updateOrderPosition();	//persist
            		
    					nOpen--;						// one less to deal with
    				}
    				else {
    					// since its still not filled, it needs to be sent to the broker
    					// so it can be updated as necessary when OrderStatus is called
    					// (initiated below)
    					// its OK to add the same holding multiple times as the brokerMgr keeps
    					// this as a hashmap (only one entry per orderId)
    					brokerManager.updateHoldings(holding);          			
    				}
    			} // it was our holding's orderId
            }  // next executed order       
        } //next holding
    	    	
    	// request all our open orders from IB.. it should agree with our DB
    	
    	// TODO: this may be overkill... maybe we just need to request it to force
    	// OrderStaus to be called
    	// and not care about the returned data (it will be updated asynchronously when orderStatus is called)
    	// but may be good to wait for initialization
    	Log .println("[DEBUG] Reqesting Open Orders from IB...");
    	List<Order> openOrders = brokerManager.reqOpenOrders(2000);
 
    	// sanity check - the lists should agree TODO
    	if (openOrders.size() != nOpen)
    		Log.println("IB Open Orders and ours dont agree! "+openOrders.size()+"/"+nOpen);

        if (openOrders.size() > 0) {
        	Iterator<Order> oit = openOrders.iterator();
        	int n = openOrders.size();
            while (oit.hasNext()) {
                Order o = (Order) oit.next();
                Log.println("[DEBUG] IB Open Order: " +o.m_orderId+" "+o.m_action);
            }
        } else
            Log.println("[DEBUG] There are no currently open IB orders");
 	
    	return nOpen;
    }
    

    

    /**
     * After we place our buy orders, make sure all orders are filled
     * we do that by waiting a bit, and hopefully in the interrim
     * OrderStatuses have come in and the DB has been updated with Filled
     * 
     * 
     * @return number of unfilled holdings
     */
    public int allBought() {
 
    	Date date = timeManager.getCurrentTradeDate();

    	
    	// check the DB to be sure all of todays orders are filled
    	//"SELECT status FROM Holdings where buy_date is today";
        Session session = databaseManager.getSessionFactory().openSession();
        
        Criteria criteria = session.createCriteria(Holding_T.class)
            .add(Restrictions.ne("orderStatus", "Filled"))
            .add(Restrictions.between("buyDate", date, Utilities_T.tomorrow(date)));

        @SuppressWarnings("unchecked")
        List<Holding_T> holdingData = criteria.list();
        
        session.close();
  
        
        if (holdingData.size() == 0)
        {
        	Log.println("[DEBUG] Great! There are no unfilled BUY holdings remaining.");
        	return 0;
        }
                
        Log.println("[DEBUG] There are still "+holdingData.size()+" unfilled BUY Holdings remaining:");
            
        Iterator<Holding_T> it = holdingData.iterator();
        while (it.hasNext()) {
        	Holding_T holding = it.next();
    	    Log.println("[DEBUG]   "+ holding.getSymbol().getSymbol() + "("+holding.getSymbolId()+"): "+holding.getOrderStatus()+
    	    			" filled: "+holding.getFilled()+" out of "+holding.getVolume());
        }
    	
        return holdingData.size();
    }

    /**
     * After we placed our sell orders, make sure all orders are filled
     * we do that by waiting a bit, and hopefully in the interrim
     * OrderStatuses have come in and the DB has been updated with Filled
     * 
     * 
     * @return number of unfilled holdings
     */
    public int allSold() {
    	
    	Date date = timeManager.getPreviousTradeDate();
    	
    	// check the DB to be sure all of todays orders are filled
    	//"SELECT status FROM Holdings where buy_date is previous trade date";
        Session session = databaseManager.getSessionFactory().openSession();
        
        Criteria criteria = session.createCriteria(Holding_T.class)
            .add(Restrictions.ne("orderStatus", "Filled"))
            .add(Restrictions.between("buyDate", date, Utilities_T.tomorrow(date)));

        @SuppressWarnings("unchecked")
        List<Holding_T> holdingData = criteria.list();
        
        session.close();

        if (holdingData.size() == 0)
        {
        	Log.println("[DEBUG] Great! There are no SELL holdings remaining.");
        	return 0;
        }
                
        Log.println("[DEBUG] There are still "+holdingData.size()+" SELL Holdings remaining:");
            
        Iterator<Holding_T> it = holdingData.iterator();
        while (it.hasNext()) {
        	Holding_T holding = it.next();
    	    Log.println("[DEBUG]   "+ holding.getSymbol().getSymbol() + "("+holding.getSymbolId()+"): "+holding.getOrderStatus()+
    	    			" remaining: "+holding.getRemaining() +" out of "+holding.getVolume());
        }
    	
        return holdingData.size();
    }

    /**
     * Cancel any Buy Orders that didnt completely fill
     * @return the number of orders canceled
     */
    public int cancelBuyOrders() {
    	 
    	Date date = timeManager.getCurrentTradeDate();

    	
    	// check the DB to be sure all of todays orders are filled
        Session session = databaseManager.getSessionFactory().openSession();
        
        Criteria criteria = session.createCriteria(Holding_T.class)
            .add(Restrictions.ne("orderStatus", "Filled"))
            .add(Restrictions.between("buyDate", date, Utilities_T.tomorrow(date)));

        @SuppressWarnings("unchecked")
        List<Holding_T> holdingData = criteria.list();
        
        session.close();
  
        
        if (holdingData.size() == 0) return 0;

                
        Log.println("[DEBUG] Cancelling "+holdingData.size()+" unfilled BUY Holdings:");
            
        Iterator<Holding_T> it = holdingData.iterator();
        while (it.hasNext()) {
        	Holding_T holding = it.next();
    	    Log.println("[DEBUG]   "+ holding.getSymbol().getSymbol() + "("+holding.getSymbolId()+"): "+holding.getOrderStatus()+
    	    			" filled: "+holding.getFilled()+" out of "+holding.getVolume()+
    	    			" orderId: "+holding.getOrderId());
    	    
    	    // do it - and check for response
    	    //brokerManager.cancelOrder(holding.getOrderId());
    	    
    	    // update the volume
    	    holding.setVolume(holding.getFilled());
    	    //holding.updateVolume();
    	    
    	    // TODO: persist
        }
    	
        return holdingData.size();
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
    	try { Thread.sleep(10000); }
        catch (InterruptedException e) { e.printStackTrace(); }

    	// catch any stragglers (wait for 10 seconds)
    	try { Thread.sleep(10000); }
        catch (InterruptedException e) { e.printStackTrace(); }
}

if (1==0) {
	// these return the same list
	// reqId is only needed to matchup request and return - it can be arbitrary (or always 1)
	int reqId = 0;
	List<Execution> executedOrders = brokerManager.reqExecutions(reqId, 2000);
	
	reqId = 33;
    executedOrders = brokerManager.reqExecutions(reqId, 2000);
	
}

    }


    public void TestBuyOrSell(Action buyOrSell)
    {
    	System.out.println("***TEST BuyOrSell***");
    	
    	//brokerManager.updateAccount();  // we need an account to start

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
