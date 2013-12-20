package dayTrader;

import interfaces.Manager_IF;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import managers.BrokerManager_T;
import managers.ConfigurationManager_T;
import managers.DatabaseManager_T;
import managers.EmailManager_T;
import managers.LoggerManager_T;
import managers.MarketDataManager_T;
import managers.TimeManager_T;
import util.XMLTags_T;
import util.dtLogger_T;


public class DayTrader_T {
    /* {src_lang=Java}*/

    private static Map<Class<?>, Manager_IF> serviceManager = new HashMap<Class<?>, Manager_IF>();
    private static List<Thread> threads = new ArrayList<Thread>();

    private static ConfigurationManager_T configurationManager = null;
    private static DatabaseManager_T databaseManager = null;
    private static MarketDataManager_T marketDataManager = null;
    private static BrokerManager_T brokerManager = null;
    private static LoggerManager_T loggerManager = null;
    private static TimeManager_T timeManager = null;
    private static EmailManager_T emailManager = null;

    public static dtLogger_T dtLog;

    /*** global testing/development parameters ***/

    // For Testing, we can run w/o IB (BrokerMgr) and not execute trades
    public static boolean d_useIB = true;
    // or without TD if we want dont need to update the Quote data
    public static boolean d_useTD = true;

    // override is marketOpen - make it open
    public static boolean d_ignoreMarketClosed = false;

    // if not null, use this simulated time as current time
    public static String d_useSimulateDate = "";    
    //public static String d_useSimulateDate = "2013-11-21 15:50:00";

    // get EndOfDayQuotes from TD - only needs to be run once.  if you run it multiple
    // times, first delete all of todays EOD data DELETE FROM EndOfDayQuotes WHERE DATE(date) = CURRENT_DATE()
    public static boolean d_takeSnapshot = true;

    // enable RT logic
    public static boolean d_getRTData = true;

    // use system time instead of IB time
    public static boolean d_useSystemTime = true;

    /*** end development defines ***/

    /* our one and only log file */
    private static String dtLogFilename = "";
    private static boolean echoLog = true;
    private static boolean logTimestamp = false;

    /**
     * 
     */
    public DayTrader_T() {}

    
    public static void main(String[] alrgs) {

        //implement a global exception handler in case anything gets thrown uncaught
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Thread.currentThread().setDefaultUncaughtExceptionHandler(handler);
        
        
        
        String configFile = "";

        for (String arg : alrgs) {
            if (arg.startsWith("-cfg=")) {
                configFile = arg.substring(5);
            }
        }
        
        configurationManager = new ConfigurationManager_T(configFile);
        
        
        serviceManager.put(configurationManager.getClass(), configurationManager);

        databaseManager = new DatabaseManager_T();
        marketDataManager = new MarketDataManager_T();
        brokerManager = new BrokerManager_T();
        loggerManager = new LoggerManager_T();
        timeManager = new TimeManager_T();
        emailManager = new EmailManager_T();
        dtLog = new dtLogger_T();

        dtLogFilename = configurationManager.getConfigParam(XMLTags_T.CFG_DT_LOG_FILE_NAME);
        d_useIB = Boolean.parseBoolean(configurationManager.getConfigParam(XMLTags_T.CFG_USE_IB));
        d_useTD = Boolean.parseBoolean(configurationManager.getConfigParam(XMLTags_T.CFG_USE_TD));
        d_ignoreMarketClosed = Boolean.parseBoolean(configurationManager.getConfigParam(XMLTags_T.CFG_IGNORE_MARKET_CLOSED));
        d_useSimulateDate = configurationManager.getConfigParam(XMLTags_T.CFG_USE_SIMULATE_DATE);
        d_takeSnapshot = Boolean.parseBoolean(configurationManager.getConfigParam(XMLTags_T.CFG_TAKE_SNAPSHOT));
        d_getRTData = Boolean.parseBoolean(configurationManager.getConfigParam(XMLTags_T.CFG_GET_RT_DATA));
        d_useSystemTime = Boolean.parseBoolean(configurationManager.getConfigParam(XMLTags_T.CFG_USE_SYSTEM_TIME));

        initialize();
        run();
        terminate();

    }

    public static void initialize() {

        serviceManager.put(databaseManager.getClass(), databaseManager);

        //--SAL--
        if (d_useTD) { serviceManager.put(marketDataManager.getClass(), marketDataManager); }
        if (d_useIB) { serviceManager.put(brokerManager.getClass(), brokerManager); }
        //--SAL--
        serviceManager.put(timeManager.getClass(), timeManager);
        serviceManager.put(loggerManager.getClass(), loggerManager);
        serviceManager.put(emailManager.getClass(), emailManager);

        dtLog.open(dtLogFilename); dtLog.setEcho(echoLog); dtLog.setTimeStamp(logTimestamp);

        //threads.add(new Thread(databaseManager));
        threads.add(new Thread(brokerManager));
        //threads.add(new Thread(loggerManager));
        threads.add(new Thread(marketDataManager));
        threads.add(new Thread(timeManager));


        Iterator<Manager_IF> it = serviceManager.values().iterator();
        while (it.hasNext()) {
            Manager_IF mgr = it.next();
            //Initialize each manager
            mgr.initialize();
        }
        
    }

    public static void run() {

        Iterator<Thread> it = threads.iterator();
        while (it.hasNext()) {
            Thread thread = it.next();
            //start each manager thread
            thread.run();
        }

        //TODO: uncomment me when ready for production  SALxx - why??
        //sleep();

    }

    public void sleep() {
        //TODO: we'll want to sleep indefinitely if we run the appplication 24/7
        //otherwise we'll need to build in a trigger to terminate the app

        /*
         * while (Thread.interrupted()) {
         *     try {
         *         Thread.sleep(60000 * 10);
         *     } catch (InterruptedException e) {
         *         wakeup();
         *     }
         */
    }

    public static void terminate() {

        dtLog.println("\n*** DayTrader is terminating at "+timeManager.TimeNow()+" ***\n");

        Iterator<Manager_IF> mit = serviceManager.values().iterator();
        while (mit.hasNext()) {
            Manager_IF mgr = mit.next();
            //Initialize each manager
            mgr.terminate();

        }

        Iterator<Thread> it = threads.iterator();
        while (it.hasNext()) {
            Thread thread = it.next();
            //stop each manager thread
            thread.interrupt();
        }
        
        dtLog.close();

    }

    public void wakeup() {
        //if the main thread is interrupted, just terminate
        terminate();		
    }

    public static Manager_IF getManager(Class<?> clazz) {

        return serviceManager.get(clazz);

    }


}

/**
 * A default uncaught exception handler
 * 
 * @author nathan
 *
 */
class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {
    
    public void uncaughtException(Thread t, Throwable e) {
        dtLogger_T Log  = DayTrader_T.dtLog;
        
        Log.println("[ERROR] An exception was thrown and caught by the GlobalExceptionHandler!!!!");
        e.printStackTrace();
        
        DayTrader_T.terminate();
    }
    
}