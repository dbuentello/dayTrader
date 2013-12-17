package trader;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Level;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import dayTrader.DayTrader_T;
import accounts.Portfolio_T;
import util.XMLTags_T;
import util.dtLogger_T;

import managers.BrokerManager_T;
import managers.DatabaseManager_T;
import managers.LoggerManager_T;
import managers.TimeManager_T;
import marketdata.MarketData_T;
import managers.ConfigurationManager_T;

import trader.Holding_T;
import trader.DailyNet_T;

import util.Utilities_T;


public class TraderCalculator_T {
  /* {src_lang=Java}*/

    /** The cumulative maximum we want to buy. */ 
    private double MAX_BUY_AMOUNT = 10000.0;
    /** The minimum account balance we want to have. */
    public static int MIN_ACCOUNT_BALANCE = 25000;
    
    /** References to the Managers we need */
    private DatabaseManager_T databaseManager;
    private TimeManager_T timeManager;
    private dtLogger_T Log;  // shorthand
    
    
    /**
     * 
     */
    public TraderCalculator_T() {
        
        ConfigurationManager_T cfgMgr = (ConfigurationManager_T) DayTrader_T.getManager(ConfigurationManager_T.class);
        MAX_BUY_AMOUNT = Double.parseDouble(cfgMgr.getConfigParam(XMLTags_T.CFG_MAX_BUY_AMOUNT));
        MIN_ACCOUNT_BALANCE = Integer.parseInt(cfgMgr.getConfigParam(XMLTags_T.CFG_MIN_ACCOUNT_BALANCE));
        
        
	    databaseManager   = (DatabaseManager_T) DayTrader_T.getManager(DatabaseManager_T.class);
	    timeManager       = (TimeManager_T) DayTrader_T.getManager(TimeManager_T.class);
	    //logger = (LoggerManager_T) DayTrader_T.getManager(LoggerManager_T.class);
	    Log = DayTrader_T.dtLog;
    }

 

    /**
     * Get the $ amount of available capital
     * 
     * @return (double) the amount of capital we can invest
     */
    public double getCapital()
    {
    	double accountBalance = 0;
    	
if (DayTrader_T.d_useIB) {
		// TODO: do we want the balance now, or at the start?  if we want current
		// we need to updateAccount() first
        BrokerManager_T brokerManager = (BrokerManager_T) DayTrader_T.getManager(BrokerManager_T.class);
        accountBalance = brokerManager.getAccount().getBalance();
}
// TODO: while paper trade account is 1M!! and when in simulation!!!
//else {
	accountBalance = MIN_ACCOUNT_BALANCE + MAX_BUY_AMOUNT;
//}

		// TODO: while paper trade account is 1M!!

        double availCap = accountBalance - MIN_ACCOUNT_BALANCE;
        
    	// TODO!!!
    	//DailyNet_T dailyNet = getLatestDailyNet();
    	//if (dailyNet.getPrice() == null)
    	//    return 0.0;						// TODO
    	
        return availCap <= MAX_BUY_AMOUNT ? availCap : MAX_BUY_AMOUNT;
  
    }
 
    /**
     * Calculate the net for the holdings that were just sold.
     * For the most recent updates to Holdings, the net profit/loss field
	 * will be empty.
	 * Only completely sold holdings are calculated
     */
    public void calculateNet()
    {

    	Date buyDate = timeManager.getPreviousTradeDate();
    	
        Session session = databaseManager.getSessionFactory().openSession();
        
        // checking the net field makes sure we dont calculate it twice
        Criteria criteria = session.createCriteria(Holding_T.class)
            .add(Restrictions.between("buyDate", buyDate, Utilities_T.tomorrow(buyDate)))
        	.add(Restrictions.isNull("net"));
        
        @SuppressWarnings("unchecked")
        List<Holding_T> holdingData = criteria.list();
                   
        session.close();

        Iterator<Holding_T> it = holdingData.iterator();
        while (it.hasNext()) {
        	Holding_T holding = it.next();
        	
        	// only include completely sold holdings (others will be calculated later as unrealized)
        	if (!holding.isSold())
        		continue;

        	double buyPrice  = holding.getActualBuyPrice();
        	double sellPrice = holding.getAvgFillPrice();	//getActualSellPrice();
        	long   volume    = holding.getVolume();
        	
        	// TODO:  this should never happen
        	if (sellPrice == 0.00) {
        		Log.println("[ERROR] CalculateNet() no sell price for "+holding.getSymbolId());
        		continue;
        	}
        	
        	//LOG
            String netGL = "EVEN";
            if (sellPrice > buyPrice) { netGL = "GAIN"; }
            if (sellPrice < buyPrice) { netGL = "LOSS"; }

            double delta = Utilities_T.round(sellPrice - buyPrice);
            double total = Utilities_T.round(delta*volume);

            // TODO: there better not be any remaining! - remove remaining when we're sure
            Log.println("*** SOLD " +holding.getSymbol().getSymbol()+" ("+holding.getSymbolId()+ ") at a "+
            			netGL+" of $"+total+" ("+delta+":"+sellPrice+"/"+buyPrice+") "+
               			" at "+holding.getSellDate().toString()+" ***"); 

                
        	double buyTotal  = buyPrice * volume;
        	double sellTotal = sellPrice * volume;
        	double net = sellTotal - buyTotal;
        	net = Utilities_T.round(net);

        	holding.updateNet(net);
        	
        }  // next Holding
        
    }
    
    
    /**
     * Calculate the total net for all holdings that were bought yesterday and 
     * completely sold today.
     * Calculate the volume and total net and add this to our daily net table
	 * Only completely sold holdings are calculated - deferred holdings will
	 * show up later
     */
    public void calculateRealizedNet()
    {
    	Date buyDate = timeManager.getPreviousTradeDate();
    	
    	Double cumNet = 0.00;
    	long   cumVol = 0;
    	
        Session session = databaseManager.getSessionFactory().openSession();
        
        // anull net field means it wasnt calculated - deferred will show up later
        Criteria criteria = session.createCriteria(Holding_T.class)
            .add(Restrictions.between("buyDate", buyDate, Utilities_T.tomorrow(buyDate)))
        	.add(Restrictions.isNotNull("net"));
        
        @SuppressWarnings("unchecked")
        List<Holding_T> holdingData = criteria.list();
                    
        Iterator<Holding_T> it = holdingData.iterator();
        while (it.hasNext()) {
        	Holding_T holding = it.next();

        	double net    = holding.getNet();
        	long   volume = holding.getVolume();
   		
        	cumNet += net;
        	cumVol += volume;
        	
        }  // next Holding
        cumNet = Utilities_T.round(cumNet);
        
        // update daily table with net for today

        // get latest capital record from DB
        
        double startCapital;
	
        // TODO: use the following getLatestDailyNet() method
        
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
         	return;			// TODO
        }
        // end TODO
        
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
     * get the latest (last) record from the DailyNet Table
     * we want to get the starting capital at the start of the day,
     * and the record id for the end of the day update
     * 
     * @return DaiyNet_T or null on error
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
       		return null;
       	}
       	
       	return dailyNet.get(0);
    }

    /**
     * Calculate estimated Net on unrealized holdings for today
     * Must be called after calculate net, as the realized net is filled in there
     */
    public void calculateUnrealizedNet()
    {
    	Date buyDate = timeManager.getPreviousTradeDate();
    	
    	Double cumNet = 0.00;
    	long   cumVol = 0;
    	
    	//SELECT id, buy_volume, buy_total, sell_price FROM Holdings WHERE DATE(buy_date) = \"$buy_date\" AND net IS NULL
        Session session = databaseManager.getSessionFactory().openSession();
        
        // any holding with a null net field has not yet been realized
        // it does need a valid buy orderId, tho
        Criteria criteria = session.createCriteria(Holding_T.class)
            .add(Restrictions.between("buyDate", buyDate, Utilities_T.tomorrow(buyDate)))
        	.add(Restrictions.isNull("net"))
        	.add(Restrictions.ne("orderId", 0));
        
        @SuppressWarnings("unchecked")
        List<Holding_T> holdingData = criteria.list();
        
        session.close();
       
        if (holdingData.size() > 0)
        	Log.println("[ATTENTION] CalculateUnrealizedNet() There are " + holdingData.size() +" unrealized holdings...");
            
        Iterator<Holding_T> it = holdingData.iterator();
        while (it.hasNext()) {
        	Holding_T holding = it.next();

        	double buyPrice  = holding.getActualBuyPrice();
        	double sellPrice = holding.getAvgFillPrice();	// this is an estimate
        	if (sellPrice == 0.00) sellPrice = holding.getSellPrice();
        	long   volume    = holding.getVolume();
        	
         	//LOG
            String netGL = "EVEN";
            if (sellPrice > buyPrice) { netGL = "GAIN"; }
            if (sellPrice < buyPrice) { netGL = "LOSS"; }

            double delta = sellPrice - buyPrice;
            delta = Utilities_T.round(delta);

            Log.println("*** UNREALIZED " +holding.getSymbolId()+ " at a " +netGL+" of $"+delta+" ("+sellPrice+"/"+buyPrice+" "+
               				holding.getRemaining()+ " remaining) at "+timeManager.TimeNow().toString()+" ***"); 

        	double buyTotal  = buyPrice * volume;
        	double sellTotal = sellPrice * volume;
        	double net = sellTotal - buyTotal;
        	net = Utilities_T.round(net);
        		
        	cumNet += net;
        	cumVol += holding.getRemaining();

        }  // next Holding
        
        if (holdingData.size() > 0) {
        	Log.println("*** Total Unrealized net of $"+cumNet+" on "+holdingData.size()+" holdings ("+cumVol+" shares)");
        }
        // TODO: update Daily Net w/Unrealized Net and remaining total Volume
    }
    

    /**
     * Calculate Net on deferred holdings that have been realized today
     * ie, any holding with a sell date of today, but buy date prior to previousTradeDate()
     */
    public void calculateDeferredNet()
    {
    	Date buyDate = timeManager.getPreviousTradeDate();
    	
    	Double cumNet = 0.00;
    	long   cumVol = 0;
    	
        Session session = databaseManager.getSessionFactory().openSession();
        
        // any holding with a null net field has not yet been realized
    	Date today = timeManager.getCurrentTradeDate();
        Criteria criteria = session.createCriteria(Holding_T.class)
                .add(Restrictions.lt("buyDate", buyDate))
                .add(Restrictions.between("sellDate", today, Utilities_T.tomorrow(today)));

        @SuppressWarnings("unchecked")
        List<Holding_T> holdingData = criteria.list();
        
        session.close();
        
        if (holdingData.size() > 0)
        	Log.println("[ATTENTION] CalculateDeferredNet() There are " + holdingData.size() +" realized deferred holdings...");
            
        Iterator<Holding_T> it = holdingData.iterator();
        while (it.hasNext()) {
        	Holding_T holding = it.next();

        	double buyPrice  = holding.getActualBuyPrice();
        	double sellPrice = holding.getAvgFillPrice();
        	long   volume    = holding.getVolume();
        	
        	//LOG
            String netGL = "EVEN";
            if (sellPrice > buyPrice) { netGL = "GAIN"; }
            if (sellPrice < buyPrice) { netGL = "LOSS"; }

            double delta = sellPrice - buyPrice;
            delta = Utilities_T.round(delta);

            Log.println("*** DEFERRED " +holding.getSymbolId()+ " at a " +netGL+" of $"+delta+" ("+sellPrice+"/"+buyPrice+") "+
               				" at "+holding.getSellDate().toString()+" ***"); 

        	double buyTotal  = buyPrice * volume;
        	double sellTotal = sellPrice * volume;
        	double net = sellTotal - buyTotal;
        	net = Utilities_T.round(net);
        	
        	// update DB
        	holding.updateNet(net);
        	
        	cumNet += net;
        	cumVol += holding.getRemaining();

        }  // next Holding
        
        if (holdingData.size()>0) {
        	Log.println("*** Total Deferred net of $"+cumNet+" on "+holdingData.size()+" holdings ("+cumVol+" shares)");
        }
        
        // TODO: update Daily Net w/Deferred Net and total Volume
    }
 
    /**
     * Prelude Report - indicate outstanding holdings
     *   open orders from more than 2 days ago that didnt buy or sell, or
     *   are still in the process of bying or selling.  This most likely
     *   indicates some error, but they could also be low volatility holdings
     *   We would need to take manual action
     *   
     *   @return whether the report needs attention
     */
    public boolean PreludeReport()
    {
    	boolean attention = false;
    	
        ConfigurationManager_T cfgMgr = (ConfigurationManager_T) DayTrader_T.getManager(ConfigurationManager_T.class);
        String reportDir = cfgMgr.getConfigParam(XMLTags_T.CFG_DT_REPORT_DIR_PATH);
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd"); 
      	String reportName = "Attention_"+df.format(timeManager.getCurrentTradeDate());
       	dtLogger_T report = new dtLogger_T();
       	report.open(reportDir +"/"+reportName+".rpt");

       	report.println("\nDaily Trade Report for "+df.format(timeManager.getCurrentTradeDate())+"\n");

       	// anything that was before this...
    	Date buyDate = timeManager.getPreviousTradeDate();
    	
        Session session = databaseManager.getSessionFactory().openSession();

        //======================================
    	// these are the ones that werent bought
        //======================================
        
        // if no orderId and permId, the order was not created. thats probably due to a crash
        Criteria criteria = session.createCriteria(Holding_T.class)
            .add(Restrictions.lt("buyDate", buyDate))
            .add(Restrictions.eq("orderId", 0))
            .add(Restrictions.eq("permId", 0));       
        
        @SuppressWarnings("unchecked")
        List<Holding_T> h1 = criteria.list();
     
        if (!h1.isEmpty()) {
        	report.println("\nThere are "+h1.size()+" Holdings with no BUY order id:");
        
        	Iterator<Holding_T> it = h1.iterator();
        	while (it.hasNext()) {
        	
        		Holding_T h = it.next();
        		report.println("  "+h.getSymbol().getSymbol()+" ("+h.getSymbolId()+") "+
        					"Buy date: "+h.getBuyDate()+" RecordId: "+h.getId()+" "+h.getVolume()+" shares");        	
        	}
        	
        	attention = true;
        }

        // no permId - order may not have gone thru to IB, or we never received the ack
        criteria = session.createCriteria(Holding_T.class)
                .add(Restrictions.lt("buyDate", buyDate))
                .add(Restrictions.ne("orderId", 0))
                .add(Restrictions.eq("permId", 0));       
            
        @SuppressWarnings("unchecked")
        List<Holding_T>h2 = criteria.list();
         
        if (!h2.isEmpty()) {
        	report.println("\nThere are "+h2.size()+" BUY Holdings that were not placed or confirmed:");

        	Iterator<Holding_T> it = h2.iterator();
        	while (it.hasNext()) {
        		Holding_T h = it.next();
        		report.println("  "+h.getSymbol().getSymbol()+" ("+h.getSymbolId()+") "+
    					"Buy date: "+h.getBuyDate()+" OrderId: "+h.getOrderId()+ " RecordId: "+h.getId() +
    					" "+h.getVolume()+" shares");
        	}
        	
        	attention = true;
        }

        // these may still be in progress (filled < volume)
        criteria = session.createCriteria(Holding_T.class)
                .add(Restrictions.lt("buyDate", buyDate))
                .add(Restrictions.ltProperty("filled", "volume"))
                .add(Restrictions.ne("orderId", 0))
                .add(Restrictions.ne("permId", 0));       
            
        @SuppressWarnings("unchecked")
        List<Holding_T>h3 = criteria.list();
         
        if (!h3.isEmpty()) {
        	report.println("\nThere are "+h3.size()+" BUY Holdings still being processed:");
            
        	Iterator<Holding_T> it = h3.iterator();
        	while (it.hasNext()) {
        		Holding_T h = it.next();
        		report.println("  "+h.getSymbol().getSymbol()+" ("+h.getSymbolId()+") "+
    					"Buy date: "+h.getBuyDate()+" OrderId: "+h.getOrderId()+ " RecordId: "+h.getId() +
    					" "+h.getVolume()+" shares, only "+h.getFilled()+" filled");        	
        	}
        	
        	attention = true;
        }        

        //====================================
    	// these are the ones that werent sold
        //====================================
        
        // if no orderId2 and permId2, the order was not created. thats probably due to a crash
        criteria = session.createCriteria(Holding_T.class)
            .add(Restrictions.lt("buyDate", buyDate))
            .add(Restrictions.eq("orderId2", 0))
            .add(Restrictions.eq("permId2", 0));       
        
        @SuppressWarnings("unchecked")
        List<Holding_T> h4 = criteria.list();
     
        if (!h4.isEmpty()) {
        	report.println("\nThere are "+h4.size()+" Holdings with no SELL order id:");
        
        	Iterator<Holding_T> it = h4.iterator();
        	while (it.hasNext()) {
        	
        		Holding_T h = it.next();
        		report.println("  "+h.getSymbol().getSymbol()+" ("+h.getSymbolId()+") "+
        					"Buy date: "+h.getBuyDate()+" RecordId: "+h.getId()+" "+h.getVolume()+" shares");        	
        	}
        	
        	attention = true;
        }

        // no permId2 - order may not have gone thru to IB, or we never received the ack
        criteria = session.createCriteria(Holding_T.class)
                .add(Restrictions.lt("buyDate", buyDate))
                .add(Restrictions.ne("orderId2", 0))
                .add(Restrictions.eq("permId2", 0));       
            
        @SuppressWarnings("unchecked")
        List<Holding_T>h5 = criteria.list();
         
        if (!h5.isEmpty()) {
        	report.println("\nThere are "+h5.size()+" SELL Holdings that were not placed or confirmed:");

        	Iterator<Holding_T> it = h5.iterator();
        	while (it.hasNext()) {
        		Holding_T h = it.next();
        		report.println("  "+h.getSymbol().getSymbol()+" ("+h.getSymbolId()+") "+
    					"Buy date: "+h.getBuyDate()+" OrderId: "+h.getOrderId2()+ " RecordId: "+h.getId() +
    					" "+h.getVolume()+" shares");        	

        	}
        	
        	attention = true;
        }

        // these may still be in progress (there are still some remaining)
        criteria = session.createCriteria(Holding_T.class)
                .add(Restrictions.lt("buyDate", buyDate))
                .add(Restrictions.ne("orderId", 0))
                .add(Restrictions.ne("permId", 0))
                .add(Restrictions.ne("remaining", 0));
            
        @SuppressWarnings("unchecked")
        List<Holding_T>h6 = criteria.list();
         
        if (!h6.isEmpty()) {
        	report.println("\nThere are "+h6.size()+" SELL Holdings still being processed:");
            
        	Iterator<Holding_T> it = h6.iterator();
        	while (it.hasNext()) {
        		Holding_T h = it.next();
        		report.println("  "+h.getSymbol().getSymbol()+" ("+h.getSymbolId()+") "+
    					"Buy date: "+h.getBuyDate()+" OrderId: "+h.getOrderId2()+ " RecordId: "+h.getId() +
    					" "+h.getVolume()+" shares, still "+h.getRemaining()+" remaining");        	
        	}
        	
        	attention = true;
        }        

        // maybe TODO: delete if nothing needs attention
        if (!attention)
        	report.println("No Holdings need attention");
 
        session.close();
        report.close();
        
        return attention;
    }
    
    /**
     * Create End of Day Holdings Report
     */
    public void DailyReport()
    {
        ConfigurationManager_T cfgMgr = (ConfigurationManager_T) DayTrader_T.getManager(ConfigurationManager_T.class);
        String reportDir = cfgMgr.getConfigParam(XMLTags_T.CFG_DT_REPORT_DIR_PATH);
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd"); 
    	String reportName = "dayTrader_"+df.format(timeManager.getCurrentTradeDate());
    	dtLogger_T report = new dtLogger_T();
    	report.open(reportDir +"/"+reportName+".rpt");

    	report.println("\nDaily Trade Report for "+df.format(timeManager.getCurrentTradeDate())+"\n");

if (DayTrader_T.d_useIB) {
        BrokerManager_T brokerManager = (BrokerManager_T) DayTrader_T.getManager(BrokerManager_T.class);

    	//trader.getStartingBalance();  this is OK als long as update was only called once
    	double startingBalance = brokerManager.getAccount().getBalance();
    	// get most recent
    	brokerManager.updateAccount();
    	double endingBalance = brokerManager.getAccount().getBalance();
    	
    	report.println("Starting balance: $"+startingBalance+" Ending balance: $"+endingBalance+
    					" Net: $"+(endingBalance-startingBalance)+"\n");
}

    	Double cumNet = 0.00;
    	long   cumVol = 0;
    	
    	Date buyDate = timeManager.getPreviousTradeDate();
    	
    	//SELECT * FROM Holdings WHERE DATE(buy_date) = \"$buy_date\"
        Session session = databaseManager.getSessionFactory().openSession();
        
        Criteria criteria = session.createCriteria(Holding_T.class)
            .add(Restrictions.between("buyDate", buyDate, Utilities_T.tomorrow(buyDate)));
        
        @SuppressWarnings("unchecked")
        List<Holding_T> holdingData = criteria.list();
        
        
        if (holdingData.size() != Trader_T.MAX_BUY_POSITIONS) {
        	Log.println("[WARNING] CreateReports() something is fishy in Holdings... "+holdingData.size()+" rows retrieved.  Should be "+Trader_T.MAX_BUY_POSITIONS);
        }
        
        //                                  actual    desired
        report.println("Symbol\t\tBuy/Sell\t\t$$\tact$$\tvolume\tleft\tEOD$$\task/bid\t\tdvol\tOrderId");
        
        Iterator<Holding_T> it = holdingData.iterator();
        while (it.hasNext()) {
        	
        	Holding_T holding = it.next();
        	
        	// maybe it was never bought...
        	if (!holding.isSelling() && !holding.isSold())
        		continue;
        	       	
        	double buyPrice  = holding.getActualBuyPrice();
        	double sellPrice = holding.getAvgFillPrice();	//getActualSellPrice();
        	if (holding.getNet() == null) {					// this order hasnt filled yet
        		sellPrice = holding.getSellPrice();
        	}
        	long   volume    = holding.getVolume();
        	
        	double buyTotal  = buyPrice * volume;
        	double sellTotal = sellPrice * volume;
        	double net = sellTotal - buyTotal;
        	net = Utilities_T.round(net);
 	
        	// get EOD info  (it has extra info)
        	MarketData_T buyData  = databaseManager.getMarketData(holding.getSymbolId(), buyDate);
        	Date sellDate = timeManager.getCurrentTradeDate();
        	MarketData_T sellData = databaseManager.getMarketData(holding.getSymbolId(), sellDate);
        	
        	if (buyData == null || sellData == null) {
        		Log.println("[ERROR] CreateReport() cant get EOD data for "+holding.getSymbol().getSymbol()+ "("+holding.getSymbolId()+")");
        		continue;
        	}
        	
        	report.print(holding.getSymbol().getSymbol()+ "["+holding.getSymbolId()+"]");
        	report.print("\t"+holding.getBuyDate()+"\t$"+holding.getBuyPrice()+"\t$"+holding.getActualBuyPrice()+"\t"+holding.getVolume()+"\t");
        	report.println("\t$"+buyData.getLastPrice()+"\t$"+buyData.getAskPrice()+"/$"+buyData.getBidPrice()+"\t"+buyData.getVolume().longValue()+"\t"+holding.getOrderId());
        	report.print("\t\t"+holding.getSellDate()+"\t$" +sellPrice+"\t$"+holding.getAvgFillPrice()+"\t"+holding.getVolume()+"\t"+holding.getRemaining());
        	report.println("\t$"+sellData.getLastPrice()+"\t$"+sellData.getAskPrice()+"/$"+sellData.getBidPrice()+"\t"+sellData.getVolume().longValue()+"\t"+holding.getOrderId2());
         	
        	if (holding.getNet() != null) {	// this order hasnt filled yet, dont count it
        		cumNet += net;
        		cumVol += volume;
               	report.println("\t\t\t\t[net: $"+net+"]");
        	} else {
               	report.println("\t\t\t[deferred net: $"+net+"]");
        	}
        	
        }  // next Holding
                
        cumNet = Utilities_T.round(cumNet);
        //double netLessCommision =  Utilities_T.round(cumNet - (cumVol * 0.01));
        double netLessCommision =  Utilities_T.round(cumNet - 50.00);
        //report.println("\nTotal Net: $"+netLessCommision+" on "+cumVol+" shares ($"+cumNet+" less commission)");
        report.println("\nTotal Net: $"+cumNet+" on "+cumVol+" shares");

        //==================================
        // TODO: maybe we should report adjusted cancel orders new volume and old remaining
        //==================================
        
        
        //===================================       
        // outstanding positions (unrealized)
        //===================================
        criteria = session.createCriteria(Holding_T.class)
                .add(Restrictions.lt("buyDate", buyDate))
                .add(Restrictions.isNull("net"))
    			.add(Restrictions.ne("orderId", 0));
    	
        @SuppressWarnings("unchecked")
        List<Holding_T> outstandingData = criteria.list();
        
        
        report.newline(); report.newline();
        report.println("There are "+outstandingData.size()+" previous outstanding holdings:");
        it = outstandingData.iterator();
        while (it.hasNext()) {
        	Holding_T holding = it.next();
        	
        	report.print(holding.getSymbol().getSymbol()+ "["+holding.getSymbolId()+"]");
        	report.print("\t"+holding.getBuyDate()+"\t$"+holding.getBuyPrice()+"\t$"+holding.getActualBuyPrice()+"\t"+holding.getVolume()+"\t");
        	report.println("\t\t\t\t\t"+holding.getOrderId());
        	if (holding.getSellDate() != null)
        		report.print("\t\t"+holding.getSellDate()+"\t$" +holding.getSellPrice()+"\t$"+holding.getAvgFillPrice()+"\t"+holding.getVolume()+"\t"+holding.getRemaining());
        	else
        		report.print("\t\t\t\t\t$" +holding.getSellPrice()+"\t$"+holding.getAvgFillPrice()+"\t"+holding.getVolume()+"\t"+holding.getRemaining());
        	report.println("\t\t\t\t\t"+holding.getOrderId2());	
        }

        //================================================
        // deferred holdings that have been realized today
        //================================================
        Date today = timeManager.getCurrentTradeDate();
        criteria = session.createCriteria(Holding_T.class)
                .add(Restrictions.lt("buyDate", buyDate))
                .add(Restrictions.between("sellDate", today, Utilities_T.tomorrow(today)));
         
        @SuppressWarnings("unchecked")
        List<Holding_T> deferredData = criteria.list();
        
        report.newline(); report.newline();
        report.println("There are "+deferredData.size()+" realized deferred holdings:");
        it = deferredData.iterator();
        while (it.hasNext()) {
        	Holding_T holding = it.next();
        	
        	report.print(holding.getSymbol().getSymbol()+ "["+holding.getSymbolId()+"]");
        	report.print("\t"+holding.getBuyDate()+"\t$"+holding.getBuyPrice()+"\t$"+holding.getActualBuyPrice()+"\t"+holding.getVolume()+"\t");
        	report.println("\t\t\t\t\t"+holding.getOrderId());
       		report.print("\t\t"+holding.getSellDate()+"\t$" +holding.getSellPrice()+"\t$"+holding.getAvgFillPrice()+"\t"+holding.getVolume()+"\t"+holding.getRemaining());
        	report.println("\t\t\t\t\t"+holding.getOrderId2());	
        }
        
        session.close();        
        report.close();
    }
 
    /**
     * Reconcile our DB with IB - report differences in portfolio holdings
     */
    public void ReconciliationReport()
    {
if (!DayTrader_T.d_useIB) {
   return;    		
}

        ConfigurationManager_T cfgMgr = (ConfigurationManager_T) DayTrader_T.getManager(ConfigurationManager_T.class);
        String reportDir = cfgMgr.getConfigParam(XMLTags_T.CFG_DT_REPORT_DIR_PATH);
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd"); 
    	String reportName = "Reconcile_"+df.format(timeManager.getCurrentTradeDate());
    	dtLogger_T report = new dtLogger_T();
    	report.open(reportDir +"/"+reportName+".rpt");

    	report.println("\nReconciliation Report for "+df.format(timeManager.getCurrentTradeDate())+"\n");


        BrokerManager_T brokerManager = (BrokerManager_T) DayTrader_T.getManager(BrokerManager_T.class);

        // get most recent portfolio?
    	//brokerManager.updateAccount();

    	Map <String, Portfolio_T> p = brokerManager.getPortfolio();

    	// make a copy, as we'll be deleting from this as we go
    	// mind as well sort, too
        Map<String, Portfolio_T> portfolio = new TreeMap<String, Portfolio_T>(p);

    	Date buyDate = timeManager.getPreviousTradeDate();
    	
    	// these are the ones we expect to have closed

        Session session = databaseManager.getSessionFactory().openSession();
        
        Criteria criteria = session.createCriteria(Holding_T.class)
            .add(Restrictions.between("buyDate", buyDate, Utilities_T.tomorrow(buyDate)))
            .add(Restrictions.eqProperty("volume", "filled"))
            .add(Restrictions.eq("remaining", 0))
            .add(Restrictions.ne("orderId", 0))
            .add(Restrictions.ne("orderId2", 0));       
        
        @SuppressWarnings("unchecked")
        List<Holding_T> holdingData = criteria.list();
     
        report.println("\nThere are "+holdingData.size()+" Closed Holdings: ");
        
        Iterator<Holding_T> it = holdingData.iterator();
        while (it.hasNext()) {
        	
        	Holding_T holding = it.next();
        	report.print("  "+holding.getSymbol().getSymbol()+" ("+holding.getSymbolId()+") "+
        					"Bought: "+holding.getBuyDate()+" Sold: "+holding.getSellDate()+
        					" "+holding.getVolume()+" shares at net $"+holding.getNet());
        	
        	if (!portfolio.containsKey(holding.getSymbol().getSymbol()))
        		report.print(" [NOT IN PORTFOLIO!]");
        	else {
        		Integer pos= portfolio.get(holding.getSymbol().getSymbol()).m_position;
        		if (pos!=0)	// 0 is closed
        			report.print(" [OUTSTANDING SHARES: "+pos+"]");
        	}
        	report.newline();
        	
        	// we found it, whats left will be what IB thinks we own
        	portfolio.remove(holding.getSymbol().getSymbol());
        }

       	// these are the ones we expect to have open (not including today)
        // (submitted but not filled)
        
        criteria = session.createCriteria(Holding_T.class)
            .add(Restrictions.le("buyDate", buyDate))
            .add(Restrictions.eqProperty("volume", "filled"))
            .add(Restrictions.ne("remaining", 0))
            .add(Restrictions.ne("orderId", 0))
        	.add(Restrictions.ne("orderId2", 0));      
        
        @SuppressWarnings("unchecked")
        List<Holding_T> openData = criteria.list();
     
        report.println("\nThere are "+openData.size()+" Open Holdings: ");
        
        it = openData.iterator();
        while (it.hasNext()) {
        	
        	Holding_T holding = it.next();
        	report.print("  "+holding.getSymbol().getSymbol()+" ("+holding.getSymbolId()+") "+
        					"Bought: "+holding.getBuyDate()+" Remaining shares: "+holding.getRemaining()+
        					" out of "+holding.getVolume());
        	
        	if (!portfolio.containsKey(holding.getSymbol().getSymbol()))
        		report.print(" [NOT IN PORTFOLIO!]");
        	else {
        		Integer pos= portfolio.get(holding.getSymbol().getSymbol()).m_position;
        		if (pos!=holding.getRemaining())
        			report.print(" [NUMBER OF SHARES DONT MATCH: "+pos+"]");
        	}
        	report.newline();
        	
        	// we found it, whats left will be what IB thinks we own
        	portfolio.remove(holding.getSymbol().getSymbol());
        }
        
        // remove todays holdings
        buyDate = timeManager.getCurrentTradeDate();
        
        criteria = session.createCriteria(Holding_T.class)
                .add(Restrictions.between("buyDate", buyDate, Utilities_T.tomorrow(buyDate)))
                .add(Restrictions.ne("orderId", 0))
                .add(Restrictions.eq("orderId2", 0));               
            
        @SuppressWarnings("unchecked")
        List<Holding_T> todaysData = criteria.list();
            
        it = todaysData.iterator();
        while (it.hasNext()) {
            	
          	Holding_T holding = it.next();
             	
           	portfolio.remove(holding.getSymbol().getSymbol());           	
        }
        
        // remove closed positions, need to remove from iterator
        Iterator <Map.Entry<String, Portfolio_T>> pit = portfolio.entrySet().iterator();
        while (pit.hasNext())
        {
        	Map.Entry<String, Portfolio_T> entry  = pit.next();
        	if (entry.getValue().m_position == 0)
        		pit.remove();
        	
        }
        //for (Map.Entry<String, Portfolio_T> entry : portfolio.entrySet()) {
    	//    Integer pos = entry.getValue().m_position;
    	//    if (pos == 0)
    	//    	portfolio.remove(entry.getKey());
    	//}
        
        // The remaining are what IB thinks we own...
        report.println("\nThere are "+portfolio.size()+" Unaccounted Holdings:");
        //Map<String, Portfolio_T> sortedMap = new TreeMap<String, Portfolio_T>(portfolio);
    	for (Map.Entry<String, Portfolio_T> entry : portfolio.entrySet()) {
    	    String key = entry.getKey();
    	    Integer pos = entry.getValue().m_position;
    	    report.println(key+": "+pos+" shares");
    	}    	

        session.close();
 
        //===========================================================
        // TEST
        //===========================================================
        
        // Stale holdings..
        report.println("\nTEST-----------------\n");
        
        List<Holding_T> staleHoldings = databaseManager.getSubmittedOrders();
        
        Iterator<Holding_T> itt = staleHoldings.iterator();
    	while (itt.hasNext()) {
    		Holding_T h = itt.next();

    		Date d = h.getSellDate(); String ds="NULL";
    		if (d!=null) ds = h.getSellDate().toString();
    		
    		if (!portfolio.containsKey(h.getSymbol().getSymbol())) {
    			report.println("STALE Holding "+h.getId()+": "+h.getSymbol().getSymbol()+
    					" Status: "+h.getOrderStatus()+" owned: "+h.getFilled()+" remaining: "+h.getRemaining() +
    					" BUY: "+h.getBuyDate()+" SELL: "+ds);
    		}
    	}
    	
        // Lets do the inverse - what does IB say we own that we cant account for?
        ArrayList <String> IBHoldings = new ArrayList<String>();
        
        for (Map.Entry<String, Portfolio_T> entry : p.entrySet()) {
    	    String symbol = entry.getKey();
        	
        	itt = staleHoldings.iterator();
        	while (itt.hasNext()) {
        		Holding_T h = itt.next();
        		if (symbol.equalsIgnoreCase(h.getSymbol().getSymbol())) {
        			IBHoldings.add(symbol);
        			break;
        		}
        	}
        }
        if (!IBHoldings.isEmpty()) {
            report.print("[There are "+IBHoldings.size()+" Unaccounted holdings: ");
        	Iterator<String> its = IBHoldings.iterator();
        	while (its.hasNext()) { report.print(its.next()+" "); } report.newline();
        }
        else report.println("There are no Unaccounted holdings");
        
        // current holdings
        List<Holding_T> currentHoldings = databaseManager.getCurrentHoldings(timeManager.getPreviousTradeDate());
        itt = currentHoldings.iterator();
    	while (itt.hasNext()) {
    		Holding_T h = itt.next();
    		
    		if (!p.containsKey(h.getSymbol().getSymbol())) {
        		Date d = h.getSellDate(); String ds="NULL";
        		if (d!=null) ds = h.getSellDate().toString();
        		
    			report.println("Current Holding "+h.getId()+": "+h.getSymbol().getSymbol()+
    					" Status: "+h.getOrderStatus()+" owned: "+h.getFilled()+" remaining: "+h.getRemaining() +
    					" BUY: "+h.getBuyDate()+" SELL: "+ds);
    		}
    	}
    	   
    	Map<String, Portfolio_T> sortedMap = new TreeMap<String, Portfolio_T>(p);
    	for (Map.Entry<String, Portfolio_T> entry : sortedMap.entrySet()) {
    	    String key = entry.getKey();
    	    Integer pos = entry.getValue().m_position;
    	    report.println("IB Holdings "+key+": "+pos+" shares");
    	}
    	
    	// diff
    	IBHoldings.clear();
    	for (Map.Entry<String, Portfolio_T> entry : sortedMap.entrySet()) {
    	    String symbol = entry.getKey();
    	    
        	itt = currentHoldings.iterator();
        	while (itt.hasNext()) {
        		Holding_T h = itt.next();
        		if (symbol.equalsIgnoreCase(h.getSymbol().getSymbol())) {
        			IBHoldings.add(symbol);
        			break;
        		}
        	}
    	}
        if (!IBHoldings.isEmpty()) {
            report.print("[There are "+IBHoldings.size()+" differences: ");
        	Iterator<String> its = IBHoldings.iterator();
        	while (its.hasNext()) { report.print(its.next()+" "); } report.newline();
        }
        else report.println("There are no differnces");
  	
        
        
        report.close();
    }
}