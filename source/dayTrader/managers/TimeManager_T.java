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
public class TimeManager_T implements Manager_IF {

    /** Time in minutes before the close of the market day that we want to capture our snapshot of the market
     * and execute our buy orders.
     */
    private final int MINUTES_BEFORE_CLOSE_TO_BUY = 30;
    /** The number of milliseconds in one minute. */
    private final int MS_IN_MINUTE = 1000 * 60;
    
    /** A reference to the java Calendar class used to format and convert Date objects. */
    private Calendar calendar;
    /** A reference to the Calendar_T class used to query the database to see if the market
     * is open or not. May be able to get rid of this if we can query IB or TDA for that information
     */
    private Calendar_T calendar_t;
    /** The last known time as returned by our broker. */
    private Date time;
    /** The time we want to execute our buys. butTime = calendar_t.marketClose() - MINUTES_BEFORE_CLOSE_TO_BUY */
    private Date buyTime;
    /** A reference to the MarketDataManager used to retrieve market information. */
    private MarketDataManager_T marketDataManager;
    /** A reference to the BrokerManager. */
    private BrokerManager_T brokerManager;
    /** A reference to the DatabaseManager */
    private DatabaseManager_T databaseManager;
    
    
    public TimeManager_T() {
       time = new Date();
       calendar = Calendar.getInstance();
       calendar.setTime(time);
    }
    

    public void run() {
        
        int i = 0;
        while (true) {
            
            /**
             * to run application in simulation mode, and un-comment the following lines 
             *  and comment out  line "time.setTime(BrokerManager_T.getBrokerTime());"
             */
            i++;
            SimpleDateFormat df = new SimpleDateFormat();
            try { time.setTime(df.parse("10-15-2013 09:30:00").getTime() + (i * 1000*10)); } catch (ParseException e1) { /*do nothing*/ }
             
            
            //At the end of the day get a snapshot of the current quotes
            if (time.after(buyTime)) {
                marketDataManager.takeMarketSnapshot();
            }
            
            /* time.setTime(BrokerManager_T.getBrokerTime()); */
            
            
            
            
            //sleep ten seconds
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                //Do nothing
            }
        }

    }

    @Override
    public void initialize() {
        
        marketDataManager = (MarketDataManager_T) DayTrader_T.getManager(MarketDataManager_T.class);
        brokerManager = (BrokerManager_T) DayTrader_T.getManager(BrokerManager_T.class);
        databaseManager = (DatabaseManager_T) DayTrader_T.getManager(DatabaseManager_T.class);
        
        calendar_t = (Calendar_T) databaseManager.query(Calendar_T.class, mysqlDate());
        
        time.setTime(brokerManager.getBrokerTime());
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
    
    private String mysqlDate() {
        
        calendar.setTime(time);
        String date = calendar.get(Calendar.YEAR) + "-" 
                + (calendar.get(Calendar.MONTH) + 1) + "-" 
                + calendar.get(Calendar.DAY_OF_MONTH);        
        
        return date;
    }

}

