package trader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Date;

import org.apache.log4j.Level;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import dayTrader.DayTrader_T;
import util.dtLogger_T;

import managers.DatabaseManager_T;
import managers.LoggerManager_T;
import managers.TimeManager_T;
import managers.MarketDataManager_T;
import marketdata.MarketData_T;
import marketdata.RTData_T;
import marketdata.Symbol_T;

// for simulation only
import trader.Holding_T;
import trader.DailyNet_T;


public class TraderCalculator_T {
  /* {src_lang=Java}*/

    /** References to the Managers we need */
    private DatabaseManager_T databaseManager;
    private MarketDataManager_T marketDataManager;
    private TimeManager_T timeManager;
    private dtLogger_T Log;  // shorthand
    
    
    /**
     * 
     */
    public TraderCalculator_T() {
	    databaseManager   = (DatabaseManager_T) DayTrader_T.getManager(DatabaseManager_T.class);
	    timeManager       = (TimeManager_T) DayTrader_T.getManager(TimeManager_T.class);
	    marketDataManager = (MarketDataManager_T) DayTrader_T.getManager(MarketDataManager_T.class);
	    //logger = (LoggerManager_T) DayTrader_T.getManager(LoggerManager_T.class);
	    Log = DayTrader_T.dtLog;
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
    	double totalCapital = getCapital();
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

            	//SALxx-- where is buy price?  using avgFillPrice for now
            	String hql = "UPDATE trader.Holding_T " +
            			"SET avgFillPrice = :buyPrice, remaining = :buyVolume " +
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
     * simulate what brokerManager.liquidateHoldings will do, but without
     * executing the orders.  Nathan, you can copy/use this logic.
     * 
     * Sell all remaining holdings at the end of the day and record in our
     * Holdings DB
     * 
     * The logic is based on the fact that when a holding is sold, the sell date
     * will be entered into the DB.  Any holding without a sell date needs to be sold
     */
    public void simulateLiquidateHoldings()
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
           holding.setExecSellPriceLow(price);	// TODO change this field name
           
           // for info
           double buyPrice = getBuyPrice(symbol.getId());
       	
           String net = "EVEN";
           if (price > buyPrice) { net = "GAIN"; }
           if (price < buyPrice) { net = "LOSS"; }

           double delta = price - buyPrice;
           delta = round(delta);

           Log.println("*** SELL " +symbol.getId()+ " at a " +net+" of $"+delta+" ("+price+" : "+buyPrice+") at "+date.toString()+" ***"); 


           // $stmt = "UPDATE $tableName SET sell_price = $price, sell_date= \"$date\" WHERE symbol = \"$symbol\" AND sell_date is NULL";
           //>>> holding.update(); SAL why doesnt this worK??? TODO
           
           // this does....
           holding.updateSellPosition(symbol.getId(), price, sellDate);

       }
       Log.newline();

    }
    
    public void calcSellPrice() {
    }
    
    public void calcNumSharesToBuy() {
    }
    
    public void calcDollarBuyAmount() {
    }

    /**
     * Get the $ amount of available capital
     * 
     * @return (double) the amount of capital we can invest
     */
    private double getCapital()
    {
    	// TODO!!!
    	//DailyNet_T dailyNet = getLatestDailyNet();
    	//if (dailyNet.getPrice() == null)
    	//    return 0.0;						// TODO
    	
        return 10000.00;   
  
    }
    
    /**
     * for the most recent updates to Holdings, the net profit/loss field (now criteria)
	 * will be empty.  Update the sell total, and calculate the net. Add this to our daily net table
	 * must be done before todays losers are determined, and the Holdings Table updated for the new losers
     */
    public void calculateNet()
    {
    	Date buyDate = timeManager.getPreviousTradeDate();
    	
    	Double cumNet = 0.00;
    	long   cumVol = 0;
    	
    	//SELECT id, buy_volume, buy_total, sell_price FROM $tableName WHERE DATE(buy_date) = \"$buy_date\" AND criteria IS NULL
    	// criteria is actually net$ for this stock; using exec_sell_price_high  for now
        Session session = databaseManager.getSessionFactory().openSession();
        
        Criteria criteria = session.createCriteria(Holding_T.class)
        	.add(Restrictions.ge("buyDate", buyDate))
            .add(Restrictions.isNull("execSellPriceHigh"));	//SALxx - TODO change this!

        @SuppressWarnings("unchecked")
        List<Holding_T> holdingData = criteria.list();
        
        //session.close();
        
        if (holdingData.size() != Trader_T.MAX_BUY_POSITIONS)
        {
        	Log.println("WARNING! something is fishy in Holdings... "+holdingData.size()+" rows retrieved.  Should be "+Trader_T.MAX_BUY_POSITIONS);
        }
            
        Iterator<Holding_T> it = holdingData.iterator();
        while (it.hasNext()) {
        	Holding_T holding = it.next();
        	
        	double buyPrice  = holding.getAvgFillPrice();
        	double sellPrice = holding.getExecSellPriceLow();
        	long   volume    = holding.getRemaining();
        	
        	if (sellPrice == 0.00)
        	{
        		Log.println("Error! no sell price for "+holding.getSymbolId());
        	}
        	else
        	{
        		double buyTotal  = buyPrice * volume;
        		double sellTotal = sellPrice * volume;
        		double net = sellTotal - buyTotal;
        		net = round(net);
        		
        		// update the Holdings net for this stock in the Holdings Table
        		
        		//UPDATE Holdings SET sell_total=$sell_total, criteria=\"$net\" WHERE id=$id"
                holding.setExecSellPriceHigh(net);		// TODO change field name
        		holding.update();						// why doesnt this worK?  no errors
        												// but this does?
        		holding.updateNet(holding.getId(), net);
	
        		
        		cumNet += net;
        		cumVol += volume;
        	}
        	
        }  // next Holding

        // update daily table with net for today

        // get latest capital record from DB
        
        double startCapital;
        
        // TODO:
if (1==0) {
        DailyNet_T dailyNet = getLatestDailyNet();
        dailyNet.setNet(cumNet);
        dailyNet.setVolume(cumVol);
        dailyNet.update();			// nope
        dailyNet.updateNet(); 		//yup
        
        startCapital = dailyNet.getPrice();		// TODO put double here
} else {
	
        //"SELECT id, price from DailyNet ORDER BY date DESC LIMIT 1";

        criteria = session.createCriteria(DailyNet_T.class)
        	.addOrder(Order.desc("date"))
        	.setMaxResults(1);

        @SuppressWarnings("unchecked")
        List<DailyNet_T> dailyNet = criteria.list();
        
            
        if (dailyNet.size() != 1)
        {
         	Log.println("FATAL: calculateNet() Cant get starting capital value from DailyNet!");
         	// throw exception
         	return;			//SALxx TODO
        }
        
        long id = dailyNet.get(0).getId();
        startCapital = dailyNet.get(0).getPrice();

        // update our net for today
        // UPDATE DailyNet SET $netName=$cum_net, totalVolume=$cum_vol WHERE id=$id;";
        DailyNet_T dn = dailyNet.get(0);
        dn.setNet(cumNet);
        dn.setVolume(cumVol);
        dn.update();					// nope
        								// yup
        dn.updateNet(cumNet, cumVol, dn.getId());
}        

        // tell us about it
        Date today = timeManager.getCurrentTradeDate();
        Log.println("*** Net for "+today.toString()+" is $"+cumNet+" on "+cumVol+" shares ***\n");


        // add new record for starting point for tomorrow
        Date date = timeManager.getNextTradeDate();

        // we can do it two ways - cumulative (pev+cumNet) or start fresh every day
        // TEST--- always start at 10000
        startCapital = 10000;
        // TEST---

        //INSERT INTO DailyNet (date, price) VALUES (\"$date\", $start_capital+$cum_net)";
        DailyNet_T newRecord = new DailyNet_T();
        newRecord.setDate(date);
        newRecord.setPrice(startCapital);
        newRecord.insertOrUpdate();
        
        session.close();
        
    }
    
    /**
     * Determine if we should sell - trailing sell algorithm
     * 
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

if (1==0) {
        	// simple test...
        	if (price != buyPrice)
        	{
        		Holding_T h = new Holding_T();	// just a handle
        												// or should this be last trade date?
        		h.updateSellPosition(sym.getId(), price, date);
        	}
} else {
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
	 			//update the Holdings Table with this info
        		Holding_T h = new Holding_T();	// just a handle
	 			if (h.updateSellPosition(sym.getId(), price, date) != 0)
	 			{
	 				String net = "EVEN";
	 				if (price > buyPrice) { net = "GAIN"; }
	 				if (price < buyPrice) { net = "LOSS"; }

	 				double delta = price - buyPrice;
	 				delta = round(delta);

	 				Log.println("*** SELL " +sym.getId()+ " at a " +net+" of $"+delta+" ("+price+" : "+buyPrice+") at "+date.toString()+" ***"); 

// print LOGFILE "\n".timeStamp().":<1> *** SELL $symbol at a $net of \$$delta ($price : $buy_price)  at $date ***";
//if ($_dbg) { print DBGFILE "\n".timeStamp().":<1>  *** SELL $symbol at a $net of \$$delta ($price : $buy_price) at $date ***"; }
//report the reason if its a gain
//if ($_dbg) { if ($net eq "GAIN") { print DBGFILE "\n**SELL (gain)** price $price fell below .005% tolerance: $upper_threshhold (initial: $initial_upper_limit)"; } }

	 			}  // only log if updated
	 		}  // if sold
	
} // trailing algorithm
        	
        }  // next holding

    }

    /**
    # get the buy price for this symbol from the Holdings Table.  Get the last
    # holdings by previous date
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
        	Log.println("WARNING: getBuyDate returns empty for SymbolId:" + symbolId  + " (" + holdingData.size() + ")");
        	return 0.00;
        }

        double buyPrice = holdingData.get(0).getAvgFillPrice();

        return buyPrice;
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

        Double price = holdingData.get(0).getExecSellPriceLow();    	// TODO: change field name to sellPrice
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
           			"SET execSellPriceLow = :price " +	// TODO: change this fieldname
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
     * get the latest (last) record from the DailyNet Table
     * we want to get the starting capital at the start of the day,
     * and the record id for the end of the day update
     * 
     * @return DaiyNet_T
     */
    private DailyNet_T getLatestDailyNet()
    {
    	//"SELECT id, price from DailyNet ORDER BY date DESC LIMIT 1";

       	Session session = DatabaseManager_T.getSessionFactory().openSession();
       	
       	Criteria criteria = session.createCriteria(DailyNet_T.class)
       			.addOrder(Order.desc("date"))
       			.setMaxResults(1);

       	@SuppressWarnings("unchecked")
       	List<DailyNet_T> dailyNet = criteria.list();
        
       	if (dailyNet.size() != 1)
       	{
       		Log.println("FATAL: getLatestDailyNet() Cant get starting capital record from DailyNet!");
       		// throw exception
       		DailyNet_T badRet = new DailyNet_T();
       		return badRet;			//SALxx TODO now the id is 0, and price is null
       	}
       	
       	return dailyNet.get(0);
    }
    
    
    // round price to 3 digits
    public double round(double n)
    {
  	    double d = (double)((long)(n*1000.0+0.5))/1000.0;
  	  
    	  Double nd = ((n * 1000.0) + 0.5);
    	  int ni = nd.intValue();
    	  n = (double)ni/1000;
    	  
    	  return n;
    }
}