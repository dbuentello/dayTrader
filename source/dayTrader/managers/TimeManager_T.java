package managers;

import interfaces.Manager_IF;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.persistence.Transient;

import marketdata.RTData_T;
import marketdata.Symbol_T;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import trader.Holding_T;
import trader.TraderCalculator_T;
import trader.Trader_T;
import util.Calendar_T;
import util.Utilities_T;
import util.XMLTags_T;
import util.dtLogger_T;
import dayTrader.DayTrader_T;




/**
 * This manager class will keep track of the current market trading time as returned by our brokerage
 *  so we can trigger time based quote queries and buy/sell executions
 * 
 * @author nathan
 *
 */
public class TimeManager_T implements Manager_IF, Runnable {

    /** Time in minutes before the close of the market day that we want to capture our snapshot of the market
     * and execute our buy orders.
     */
    private int MINUTES_BEFORE_CLOSE_TO_BUY = 30;
    /** The number of milliseconds in one minute. */
    private final int MS_IN_MINUTE = 1000 * 60;
    /** The number of minutes in one hour. */
    private final int MIN_IN_HOUR = 60;
    /** real time scan interval */
    private int RT_SCAN_INTERVAL = 5 * MS_IN_MINUTE;
    /** how long we should wait MAX after liquidition and buying */
    private int MINUTES_TO_WAIT_FOR_EXECUTION = 5;
    

    
    /** A reference to the java Calendar class used to format and convert Date objects. */
    private static Calendar calendar;
    /** A reference to the Calendar_T class used to query the database to see if the market
     * is open or not. May be able to get rid of this if we can query IB or TDA for that information
     */
    private Calendar_T calendar_t;
    /** The last known time as returned by our broker. */
    private static Date time;
    /** scan interval keeper */
    private static Date prevScanTime;
    /** The time we want to execute our buys. buyTime = calendar_t.marketClose() - MINUTES_BEFORE_CLOSE_TO_BUY */
    private Date buyTime;
    /** A reference to the MarketDataManager used to retrieve market information. */
    private MarketDataManager_T marketDataManager;
    /** A reference to the BrokerManager. */
    private BrokerManager_T brokerManager;
    /** A reference to the DatabaseManager */
    private DatabaseManager_T databaseManager;
    
    /** our trader / calculator **/
    private TraderCalculator_T tCalculator;
    private Trader_T trader;
    
    private dtLogger_T Log;
    
    
    public TimeManager_T() {
       
        //default the time to the current system time
        time = new Date(System.currentTimeMillis());
        calendar = Calendar.getInstance();
        calendar.setTime(time);

        prevScanTime = new Date(System.currentTimeMillis() - 10000);  // before now

    }
    

    public void run() {
        
        boolean running = true;

        // TODO - replace w/config paramter
        Log.println("\n*** Day Trader V.12.02.0 has started at "+TimeNow()+" ***\n");

        if (!isMarketOpen()) {
        	Log.print("Market is not open.  Bye.");
        	return;
        }

        // we'll need this before we can use any brokerMgr/trader functions and as it could take time to init, do it here
        // and as it could take time to init, do it here
		if (!trader.init()) {
			Log.println("[FATAL ERROR] Cannot get account info or nextValidOrderId");
			return;
		}
      	
        /*
         * This loop will update the application time every three seconds and when a trigger time is reached
         * execute the appropriate actions. Triggers times can be the market open, a specified buy time,
         * and the market close.
         */

//TEST
//trader.TestCode(); running = false;
//trader.liquidateHoldings(); running = false;
//boolean b = trader.buyHoldings(); running = false;
//trader.getOutstandingOrders();
//boolean b = trader.liquidateHoldings();
//tCalculator.CreateReport();
//running = false;
//TEST        


        // whenever we start/restart, get open orders, and unrecorded execute orders
if (DayTrader_T.d_useIB) {
	
		Log.println("Retrieving Outstanding orders...");
   		int nOpenOrders = trader.getOutstandingOrders();  // from IB 
   		Log.println("There are "+nOpenOrders+" Open Orders");
}


        while (running) {

            try {            
                
                //updateTime is a blocking call that won't return until the time has been updated
            	updateTime();

            	/*
            	 * During market open hours, get RealTime Quotes for our Holdings
            	 * according to the scan interval
            	 * 
            	 * determine if we should sell now
            	 */
// debug only            	
if (DayTrader_T.d_getRTData) {
		
				// run every n minutes as define by RT_SCAN_INTERVAL
				// sell a holding if it meets the criteria.  The Order is executed,
				// but it may fill asychronously.  At the end of the day, we'll
				// check that is has completely sold
            	if (time.after(prevScanTime))
            	{
            		List <RTData_T> rtData = marketDataManager.getRealTimeQuotes();
            		
            		trader.shouldWeSell(rtData);
            		
            		prevScanTime.setTime(prevScanTime.getTime() + RT_SCAN_INTERVAL);
            	}
}
                /*
                 * At the end of the market day, perform the following 
                 * 1. get a market snapshot
                 * 2. sell any outstanding positions we're still holding 
                 * 3. identify the positions we want to buy
                 * 4. execute buy orders 
                 */
                if (time.after(buyTime)) {


                	// We need to conclude todays business by selling any remaining stocks
                	// Then calculate our net position for today
                	
                    // Execute any remaining sell orders and then wait until they sell
                	// Do not include any that are in progress from selling during the day

                    //we need to wait until they sell so we have money to buy new stocks
                    //but is this really feasible? Will the money be credited to our account immediately
                    //if the money isn't instantly credited what do we do? how do we buy additional positions?
                    //what is we can't sell a position? how are we going to handle that?

					// first, get the most recent RT data
					List <RTData_T> rtData = marketDataManager.getRealTimeQuotes();

					int nLiquidated = trader.liquidateHoldings();


					// get todays closing market info from TD and store in EndOfDayQuotes
					// this will give us time for the liquidated holdings to be sold
					// (I hope its enough time...)
if ( DayTrader_T.d_takeSnapshot) {

					marketDataManager.takeMarketSnapshot();
}


					// TODO? hang around a while so we can check if the orders
					// have been filled. If we still have partially/unfilled sell orders,
					// what do we do????
if (DayTrader_T.d_useIB) {
					int nRemaining = 1;   // this is arbitrary
					int retryCount = (MINUTES_TO_WAIT_FOR_EXECUTION * MIN_IN_HOUR)/10;
					// TODO: wait until 10 minutes? before market close
					
					List<Holding_T> remaining=null;
					while (nRemaining != 0 && retryCount != 0) {
						try { Thread.sleep(10000); }
						catch (InterruptedException e) { e.printStackTrace(); }
						retryCount--;
		
						remaining = trader.allSold();
						nRemaining = remaining.size();
					}
					trader.logRemaining("SELL", remaining);

					
}  // useIB


                    // now we can calculate net for the end of the day
                 	
                	// how much did we gain or lose today?  Persist in DB and log
                	// NOTE: we wont know until all the orders are executed, but they
					// all should be by now.
					// also calculate the estimate of unsold holdings, and
					// the actual net of any deferred holdings that were realized today
            		tCalculator.calculateNet();
            		tCalculator.calculateUnrealizedNet();
            		tCalculator.calculateDeferredNet();

            		
                    // Now we can determine todays holdings candidates (biggest losers for today)
                    Log.println("** Determing todays candidates ***");
                    List<Symbol_T> losers = databaseManager.determineBiggestLosers();
                    
                    // save in log file
                    logCandidates(losers);

                    
                    // TODO: combine these two methods?
                    // Finally, update Holdings Table w/tomorrows candidates
                    databaseManager.addHoldings(losers);
                    
                    // calculate buy positions (buy price and volume) and set initial fill/remaining
                    trader.updateBuyPositions(losers);
                    
                    // Now buy them - this will wait a bit for immediate fills
                    // TODO: but what do we do if they arent all filled now?
                    int nBought = trader.buyHoldings();
                     
					// hang around a while so we can check if the orders
					// have been filled.If we wait around long enough, the unfilled
                    // ones should fill  but if we're too close to the end of the day,
                    // and we still have partially/unfilled buy orders,
					// cancel the remaining and adjust buy volume/remaining
                    
if (DayTrader_T.d_useIB) {
					int nRemaining = 1;   // this is arbitrary
					int retryCount = (MINUTES_TO_WAIT_FOR_EXECUTION * MIN_IN_HOUR)/10;
					// TODO: or wait until 2 minutes before market close
					
					List<Holding_T> remaining= null;
					
					while (nRemaining != 0 && retryCount != 0) {
			   	    	try { Thread.sleep(10000); }
			   	        catch (InterruptedException e) { e.printStackTrace(); }
						retryCount--;
						
						remaining = trader.allBought();
						nRemaining = remaining.size();
					}
					trader.logRemaining("BUY", remaining);
					
					if (nRemaining > 0 ) {

						// Cancel remainder of the open buys and adjust volume
						trader.cancelBuyOrders();
					}
}

                    
                    // DayTrader_YYYY-MM-DD.rpt
                    tCalculator.CreateReport();
                    
                    //set buy_time to tomorrow so we don't execute this block again
                    //TODO: Check that this works
                    buyTime.setTime(buyTime.getTime() + (MS_IN_MINUTE * MIN_IN_HOUR * 24));
                    
                    //TODO: For now terminate the application at the end of each day
                    Log.println("\n*** dayTrader is exiting at "+TimeNow()+"  Bye ***");
                    throw new InterruptedException("Terminating TimeManager thread because the market is now closed");
                
                }  // end EOD
                
            } catch (InterruptedException e) {
                running = false;
            }
        }  // while running

    }

    @Override
    public void initialize() {
        
        marketDataManager = (MarketDataManager_T) DayTrader_T.getManager(MarketDataManager_T.class);
        brokerManager = (BrokerManager_T) DayTrader_T.getManager(BrokerManager_T.class);
        databaseManager = (DatabaseManager_T) DayTrader_T.getManager(DatabaseManager_T.class);
        
        tCalculator = new TraderCalculator_T(); 
        trader      = new Trader_T();
        
        Log = DayTrader_T.dtLog;
        
        ConfigurationManager_T cfgMgr = (ConfigurationManager_T) DayTrader_T.getManager(ConfigurationManager_T.class);
        MINUTES_BEFORE_CLOSE_TO_BUY = Integer.parseInt(cfgMgr.getConfigParam(XMLTags_T.CFG_MINUTES_BEFORE_CLOSE_TO_BUY));
        MINUTES_TO_WAIT_FOR_EXECUTION = Integer.parseInt(cfgMgr.getConfigParam(XMLTags_T.CFG_MINUTES_TO_WAIT_FOR_EXECUTION));
        RT_SCAN_INTERVAL = Integer.parseInt(cfgMgr.getConfigParam(XMLTags_T.CFG_RT_SCAN_INTERVAL_MINUTES)) * MS_IN_MINUTE;

        calendar_t = (Calendar_T) databaseManager.query(Calendar_T.class, time);
        
        updateTime();

        this.buyTime = new Date(calendar_t.getCloseTime().getTime() - MINUTES_BEFORE_CLOSE_TO_BUY * MS_IN_MINUTE);

        // for simulation, buy date is always before now
        if (!DayTrader_T.d_useSimulateDate.isEmpty()) { this.buyTime.setTime(0); }
        
    }

    @Override
    public void sleep() {
        // TODO Auto-generated method stub
    }

    @Override
    public void terminate() {
        // TODO Auto-generated method stub

    }

    @Override
    public void wakeup() {
        // TODO Auto-generated method stub

    }
    
    /**
     * Return true if the market is open for trading, otherwise return false
     * 
     * @return true if the market is open for trading, else false
     */
    public boolean isMarketOpen() {
    	
    	if (DayTrader_T.d_ignoreMarketClosed)
    		return true;
        
        boolean isOpen = false;
        
        calendar.setTime(time);
        String date = calendar.get(Calendar.YEAR) + "-" 
                + (calendar.get(Calendar.MONTH) + 1) + "-" 
                + calendar.get(Calendar.DAY_OF_MONTH);
        
        //SALxx - changed to use date rather than string as string query doesnt work
        Calendar_T open = (Calendar_T) databaseManager.query(Calendar_T.class, time);
        
        //TODO: Can we use TDA or IB to determine if the market is open or not?
        if (open.isMarketOpen() && time.compareTo(open.getCloseTime()) <= 0) {
            isOpen = true;
        }
        
        return isOpen;
        
    }

    public String mysqlDate() {
        
        calendar.setTime(time);
        String date = calendar.get(Calendar.YEAR) + "-" 
                + (calendar.get(Calendar.MONTH) + 1) + "-" 
                + calendar.get(Calendar.DAY_OF_MONTH);        
        
        return date;
    }

    /**
     * 
     * @return the current time
     */
    public Date TimeNow() {
        return time;
    }
    
    /**
     * get todays date with 00:00:00
     * @return Date
     */
    public Date Today()
    {
        Calendar c = Calendar.getInstance();
        c.setTime(time);
        
        if (!DayTrader_T.d_useSimulateDate.isEmpty())
        {
        	 SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        	  try { 
        	    Date d = df.parse(DayTrader_T.d_useSimulateDate);
        	    c.setTime(d);
        	  } catch (ParseException e1)  { /*do nothing*/ System.out.println("sim time parse error");}
        	    
        }
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        Date today = c.getTime(); //  midnight, that's the first second of the day
        
        return today;
    }

    /**
     * Update the time by retrieving the last known time from the broker.
     * 
     */
    private void updateTime() {
        long oldTime = time.getTime();
        
        //TODO: update this method so that if IB doesn't return an updated time for whatever reason
        //use the system time
        
//SAL
if (!DayTrader_T.d_useSystemTime) {
        //the broker manager will invoke the setTime() method when the current time has been returned so
        //loop until we get an updated time.
		//we can get stuck in an infinite loop here if we never connect to IB
        while(oldTime == time.getTime()) {

            brokerManager.reqCurrentTime();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
}
else  //SALxx - get system time
{
	// block for a bit
    try { Thread.sleep(2000);
    } catch (InterruptedException e) { e.printStackTrace(); }
    
	long now = System.currentTimeMillis();
	setTime(now);
	Date d = new Date(now);
	
//System.out.println("updateTime(): " + now + " " + d.toString());
}

        return;
    }
    
    /**
     * Get the year represented as YYYY
     * 
     * @return YYYY
     */
    public static int getYear4Digit() {
        calendar.setTime(time);
        return calendar.get(Calendar.YEAR);
    }
    
    /**
     * Get the month represented as 01-12
     * 
     * @return MM
     */
    public static int getMonthDigit() {
        calendar.setTime(time);
        return calendar.get(Calendar.MONTH) + 1;
    }
    
    /**
     * get the time - not this also does an update time (get from server) which blocks
     *  if you dont want to block, use TimeNow
     * @return the time   
     */
    public Date getTime() {
    	updateTime();
        return time;
    }


    /**
     * @param time the time to set
     */
    public void setTime(long time) {
        this.time.setTime(time);
    }

    /**
     * return date of the most current open market (accounts for weekends and holidays)
     * 
     * @return Date    Date(0) on error
     */
    public Date getCurrentTradeDate()
    {
    	if (!DayTrader_T.d_useSimulateDate.isEmpty())
    		return Utilities_T.StringToDate(DayTrader_T.d_useSimulateDate);

    	
    	//SELECT date, market_is_open FROM calendar WHERE date BETWEEN DATE_ADD(\"$date\", INTERVAL -7 DAY) AND \"$date\" ORDER BY date DESC
        Session session = databaseManager.getSessionFactory().openSession();
        
        Calendar c = Calendar.getInstance();
        c.setTime(Today());
        c.add(Calendar.DATE, -7);
        Date lastWeek = c.getTime();

        Criteria criteria = session.createCriteria(Calendar_T.class)
            .add(Restrictions.between("date", lastWeek, Today()))        		
            .addOrder(Order.desc("date"));

        @SuppressWarnings("unchecked")
        List<Calendar_T> dates = criteria.list();
        
        session.close();
        
        Iterator<Calendar_T> it = dates.iterator();
        while (it.hasNext()) {
            Calendar_T date = it.next();
            if (date.isMarketOpen()) return date.getDate();
        }
      
        Date d = new Date(0);
        return d;		// error
    }

    /**
     * Previous Trade Date (previous date market was open)
	 * return date of the previous open market date (accounts for weekends and holidays)
	 * input is the 'current' date; return is the date previous
	 * default input is today
     * 
     * @return Date  Date(0) on error
     */
    public Date getPreviousTradeDate()
    {
    	
    	//SELECT date, market_is_open FROM calendar WHERE date BETWEEN DATE_ADD(\"$date\", INTERVAL -8 DAY) AND DATE_ADD(\"$date\", INTERVAL -1 DAY) ORDER BY date DESC
        Session session = databaseManager.getSessionFactory().openSession();
        
        Calendar c = Calendar.getInstance();
        c.setTime(getCurrentTradeDate());
        c.add(Calendar.DATE, -1);
        Date yesterday = c.getTime();
        
        c.add(Calendar.DATE, -7);
        Date lastWeek = c.getTime();      

        Criteria criteria = session.createCriteria(Calendar_T.class)
            .add(Restrictions.between("date", lastWeek, yesterday))        		
            .addOrder(Order.desc("date"));

        @SuppressWarnings("unchecked")
        List<Calendar_T> dates = criteria.list();
        
        session.close();
        
        Iterator<Calendar_T> it = dates.iterator();
        while (it.hasNext()) {
            Calendar_T date = it.next();
            if (date.isMarketOpen()) return date.getDate();
        }
      
        Date d = new Date(0);
        return d;		// error
    }

    
    /**
     * return date of the next current open market (accounts for weekends and holidays)
     * 
     * @return Date    Date(0) on error
     */
    public Date getNextTradeDate()
    {
   	
    	//SELECT date, market_is_open FROM calendar WHERE date BETWEEN DATE_ADD(\"$date\", INTERVAL 1 DAY) AND DATE_ADD(\"$date\", INTERVAL 8 DAY) AND market_is_open = 1 ORDER BY date LIMIT 1
        Session session = databaseManager.getSessionFactory().openSession();
        
        Calendar c = Calendar.getInstance();
        c.setTime(getCurrentTradeDate());
        c.add(Calendar.DATE, 1);
        Date nextDay = c.getTime();
        
        c.add(Calendar.DATE, 7);
        Date nextWeek = c.getTime();       

        Criteria criteria = session.createCriteria(Calendar_T.class)
            .add(Restrictions.between("date", nextDay, nextWeek))
            .add(Restrictions.eq("marketOpen", true))
            .addOrder(Order.asc("date"))
            .setMaxResults(1);

        @SuppressWarnings("unchecked")
        List<Calendar_T> dates = criteria.list();

        session.close();
        
        if (!dates.isEmpty())
        	return dates.get(0).getDate();
        
      
        Date d = new Date(0);
        return d;		// error
    }

    /**
     * Log Helper
     */
    private void logCandidates(List<Symbol_T> losers) {
    	Iterator<Symbol_T> it = losers.iterator();
    	Log.print("Biggest Losers are: ");
    	while (it.hasNext()) {
    		Symbol_T symbol = it.next();
    		Log.print(symbol.getSymbol()+ " ");
    	}
    	Log.newline();
    }


}

