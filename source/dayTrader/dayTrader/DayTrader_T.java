package dayTrader;

import interfaces.Manager_IF;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import managers.BrokerManager_T;
import managers.DatabaseManager_T;
import managers.LoggerManager_T;
import managers.MarketDataManager_T;
import managers.TimeManager_T;


public class DayTrader_T {
  /* {src_lang=Java}*/

    private static Map<Class<?>, Manager_IF> serviceManager = new HashMap<Class<?>, Manager_IF>();
    private static List<Thread> threads = new ArrayList<Thread>();
    
    private static DatabaseManager_T databaseManager = null;
    private static MarketDataManager_T marketDataManager = null;
    private static BrokerManager_T brokerManager = null;
    private static LoggerManager_T loggerManager = null;
    private static TimeManager_T timeManager = null;

	/**
     * 
     */
    public DayTrader_T() {
        
    }

    public static void main(String[] alrgs) {
        
        databaseManager = new DatabaseManager_T();
        marketDataManager = new MarketDataManager_T();
        brokerManager = new BrokerManager_T();
        loggerManager = new LoggerManager_T();
        timeManager = new TimeManager_T();
        
        initialize();
        run();
        terminate();
        
	}

	public static void initialize() {
	    
		serviceManager.put(databaseManager.getClass(), databaseManager);
		serviceManager.put(marketDataManager.getClass(), marketDataManager);
		//--SAL--serviceManager.put(brokerManager.getClass(), brokerManager);
		serviceManager.put(loggerManager.getClass(), loggerManager);
		serviceManager.put(timeManager.getClass(), timeManager);
		
		//threads.add(new Thread(databaseManager));
		//threads.add(new Thread(brokelrManager));
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
        
        //TODO: uncomment me when ready for production
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
	    
	    Iterator<Thread> it = threads.iterator();
        while (it.hasNext()) {
            Thread thread = it.next();
            //start each manager thread
            thread.interrupt();
        }
        
        
		
	}
	
	public void wakeup() {
	    //if the main thread is interrupted, just terminate
		terminate();		
	}
	
	public static Manager_IF getManager(Class<?> clazz) {
        
	    return serviceManager.get(clazz);
	    
	}

}