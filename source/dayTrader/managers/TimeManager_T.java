package managers;

import interfaces.Manager_IF;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import util.Calendar_T;
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
    private final int MINUTES_BEFORE_CLOSE_TO_BUY = 30;
    /** The number of milliseconds in one minute. */
    private final int MS_IN_MINUTE = 1000 * 60;
    /** The number of minutes in one hour. */
    private final int MIN_IN_HOUR = 60;
    
    /** A reference to the java Calendar class used to format and convert Date objects. */
    private static Calendar calendar;
    /** A reference to the Calendar_T class used to query the database to see if the market
     * is open or not. May be able to get rid of this if we can query IB or TDA for that information
     */
    private Calendar_T calendar_t;
    /** The last known time as returned by our broker. */
    private static Date time;
    /** The time we want to execute our buys. butTime = calendar_t.marketClose() - MINUTES_BEFORE_CLOSE_TO_BUY */
    private Date buyTime;
    /** A reference to the MarketDataManager used to retrieve market information. */
    private MarketDataManager_T marketDataManager;
    /** A reference to the BrokerManager. */
    private BrokerManager_T brokerManager;
    /** A reference to the DatabaseManager */
    private DatabaseManager_T databaseManager;
    
    
    public TimeManager_T() {
       
        //default the time to the current system time
        time = new Date(System.currentTimeMillis());
        calendar = Calendar.getInstance();
        calendar.setTime(time);
    }
    

    public void run() {
        
        boolean running = true;
        int i = 0;
        /*
         * This loop will update the application time every three seconds and when a trigger time is reached
         * execute the appropriate actions. Triggers times can be the market open, a specificied buy time,
         * and the market close.
         */
        while (running) {
            
            /**
             * to run application in simulation mode, un-comment the following lines 
             *  and comment out line 77 "updateTime()"
             */
//            i++;
//            SimpleDateFormat df = new SimpleDateFormat();
//            try { time.setTime(df.parse("10-15-2013 09:30:00").getTime() + (i * 1000*10)); } catch (ParseException e1) { /*do nothing*/ }
            
            try {            
                
                //updateTime is a blocking call that won't return until the time has been updated
                updateTime();
               
                /*
                 * At the end of the market day, perform the following 
                 * 1. sell any outstanding positions we're still holding 
                 * 2. get a market snapshot
                 * 3. identify the positions we want to buy
                 * 4. execute buy orders 
                 */
                if (time.after(buyTime) && isMarketOpen()) {
                    
                    //TODO: Execute any remaining sell orders and then wait until they sell,
                    //we need to wait until they sell so we have money to buy new stocks
                    //but is this really feasible? Will the money be credited to our account immediately
                    //if the money isn't instantly credited what do we do? how do we buy additional positions?
                    //what is we can't sell a position? how are we going to handle that?
                    brokerManager.liquidateHoldings();
                    
                    marketDataManager.takeMarketSnapshot();
                    
                    //TODO: identify and buy positions to buy. un-comment this when ready to test
                    //brokerManager.buyBiggestLosers();
                                        
                    
                    
                    //set buy_time to tomorrow so we don't execute this block again
                    buyTime.setTime(buyTime.getTime() + (MS_IN_MINUTE * MIN_IN_HOUR * 24));
                    
                    //TODO: For now terminate the application at the end of each day
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
        
        calendar_t = (Calendar_T) databaseManager.query(Calendar_T.class, time);
        
        updateTime();
        this.buyTime = new Date(calendar_t.getCloseTime().getTime() + MINUTES_BEFORE_CLOSE_TO_BUY * MS_IN_MINUTE);
        
        
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
        boolean isOpen = false;
        
        calendar.setTime(time);
        String date = calendar.get(Calendar.YEAR) + "-" 
                + (calendar.get(Calendar.MONTH) + 1) + "-" 
                + calendar.get(Calendar.DAY_OF_MONTH);
        
        Calendar_T open = (Calendar_T) databaseManager.query(Calendar_T.class, date);
        
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
     * Update the time be retrieving the last known time from the broker.
     * 
     */
    private void updateTime() {
        long oldTime = time.getTime();
        
        //TODO: update this method so that if IB doesn't return an updated time for whatever reason
        //use the system time
        
        
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

}

