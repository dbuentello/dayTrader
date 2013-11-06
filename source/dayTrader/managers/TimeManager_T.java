package managers;

import interfaces.Manager_IF;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.persistence.Transient;

import marketdata.MarketData_T;
import marketdata.Symbol_T;

import org.apache.log4j.Level;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

//SAL
//import org.apache.log4j.Logger;
//import org.jboss.logging.Logger.Level;

import util.Calendar_T;
import util.Utilities_T;
import dayTrader.DayTrader_T;

import trader.TraderCalculator_T;

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
    private final int MINUTES_BEFORE_CLOSE_TO_BUY = 15;    //SALxx
    /** The number of milliseconds in one minute. */
    private final int MS_IN_MINUTE = 1000 * 60;
    /** The number of minutes in one hour. */
    private final int MIN_IN_HOUR = 60;
    /** real time scan interval */
    private final int RT_SCAN_INTERVAL = 2 * MS_IN_MINUTE;
    
    
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
    
//SAL
//    private LoggerManager_T logger;
    
    
    public TimeManager_T() {
       
        //default the time to the current system time
        time = new Date(System.currentTimeMillis());
        calendar = Calendar.getInstance();
        calendar.setTime(time);
        
        prevScanTime = new Date(System.currentTimeMillis() - 10000);  // before now

    }
    

    public void run() {
        
        boolean running = true;

        /*
         * This loop will update the application time every three seconds and when a trigger time is reached
         * execute the appropriate actions. Triggers times can be the market open, a specified buy time,
         * and the market close.
         */

        Date d = getNextTradeDate();
        
        while (running) {

            try {            
                
                //updateTime is a blocking call that won't return until the time has been updated
            	updateTime();

            	/*
            	 * During market open hours, get RealTime Quotes for our Holdings
            	 * according to the scan interval
            	 */
            	if (isMarketOpen() && time.after(prevScanTime))
            	{
            		marketDataManager.getRealTimeQuotes();
            		prevScanTime.setTime(prevScanTime.getTime() + RT_SCAN_INTERVAL);
            	}
            	
                /*
                 * At the end of the market day, perform the following 
                 * 1. sell any outstanding positions we're still holding 
                 * 2. get a market snapshot
                 * 3. identify the positions we want to buy
                 * 4. execute buy orders 
                 */
                if (isMarketOpen() && time.after(buyTime)) {
                    
                    //TODO: Execute any remaining sell orders and then wait until they sell,
                    //we need to wait until they sell so we have money to buy new stocks
                    //but is this really feasible? Will the money be credited to our account immediately
                    //if the money isn't instantly credited what do we do? how do we buy additional positions?
                    //what is we can't sell a position? how are we going to handle that?
 
                	//SALxx--   we can run w/o a broker mgr - TODO: this is only for development                
                	if (brokerManager != null) brokerManager.liquidateHoldings();
                	else { TraderCalculator_T calc = new TraderCalculator_T(); calc.simulateLiquidateHoldings(); }
                                       
                	// get todays closing market info from TD and store in EndOfDayQuotes
// WARNING!!!
                	// TODO: this only needs to run once for debugging/development - dont want to run more than
                	// once before we auto delete previous data
                    if ( DayTrader_T.d_takeSnapshot) { marketDataManager.takeMarketSnapshot(); }
                
                    // SALxx- test
                    System.out.println("** Determing todays candidates ***");
                    List<Symbol_T> losers = databaseManager.determineBiggestLosers();
                    
                    Iterator<Symbol_T> it = losers.iterator();
                    System.out.print("\nBiggest Losers are: ");
                    while (it.hasNext()) {
                        Symbol_T symbol = it.next();
                        System.out.print(symbol.getSymbol()+ " ");
                    }
                    System.out.println();
                    
                    // Update Holdings Table w/losers
                    databaseManager.updateHoldings(losers);
                    
                    // calculate buy positions
                    TraderCalculator_T calc = new TraderCalculator_T();
                    calc.updateBuyPositions(losers);
                   
                    //SALxx - test
                    
                    //TODO: identify and buy positions to buy. un-comment this when ready to test
                    //brokerManager.buyBiggestLosers(losers);
                                        
                    
                    
                    //set     @Transientbuy_time to tomorrow so we don't execute this block again
                    //SALx-- CHECK THIS
                    buyTime.setTime(buyTime.getTime() + (MS_IN_MINUTE * MIN_IN_HOUR * 24));
                    
                    //TODO: For now terminate the application at the end of each day
                    System.out.println("*** dayTrader is exiting.  Bye ***");
                    throw new InterruptedException("Terminating TimeManager thread because the market is now closed");
                }
                
                //at some time if a BUY order hasn't been filled, just cancel it
//                if (time.after(cancel_time)) {
//                    //TODO: perform order cancels
//                }

            } catch (InterruptedException e) {
                running = false;
            }
        }

    }

    @Override
    public void initialize() {
        
        marketDataManager = (MarketDataManager_T) DayTrader_T.getManager(MarketDataManager_T.class);
        brokerManager = (BrokerManager_T) DayTrader_T.getManager(BrokerManager_T.class);
        databaseManager = (DatabaseManager_T) DayTrader_T.getManager(DatabaseManager_T.class);
 
        
//SALxx
//logger = (LoggerManager_T) DayTrader_T.getManager(LoggerManager_T.class);
LoggerManager_T logger = (LoggerManager_T) DayTrader_T.getManager(LoggerManager_T.class);       
logger.logText("Database is open = " + databaseManager.isConnected(), Level.INFO);

        calendar_t = (Calendar_T) databaseManager.query(Calendar_T.class, time);
        
        updateTime();
        
        //--SAL--   !!!!! this was + MINUTES
        this.buyTime = new Date(calendar_t.getCloseTime().getTime() - MINUTES_BEFORE_CLOSE_TO_BUY * MS_IN_MINUTE);

        // for simulation, buy date is always before now
        if ( DayTrader_T.d_simulateEOD) {

if (1==0) {
        	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            try { 
          	  long t = df.parse("2013-11-06 00:00:00").getTime();
          	  this.buyTime.setTime(t);
            } catch (ParseException e1)  { /*do nothing*/ System.out.println("sim time parse error");}
} else         
        	this.buyTime.setTime(0);
        }
        
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
        //Calendar_T open = brokerManager(Calendar_T) databaseManager.query(Calendar_T.class, date);
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

//SALxx
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
        
        if (!DayTrader_T.d_useSimulateTime.isEmpty())
        {
        	 SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        	  try { 
        	    Date d = df.parse(DayTrader_T.d_useSimulateTime);
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
        while(oldTime == time.getTime()) {

            brokerManager.reqCurrentTime();
            try {
                Thread.sleep(5000);
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
    	if (!DayTrader_T.d_useSimulateTime.isEmpty())
    		return Utilities_T.StringToDate(DayTrader_T.d_useSimulateTime);

    	
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
    
}

