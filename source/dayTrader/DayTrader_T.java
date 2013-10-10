
import interfaces.Manager_IF;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import managers.BrokerManager_T;
import managers.DatabaseManager_T;
import managers.LoggerManager_T;
import managers.MarketDataManager_T;


public class DayTrader_T {
  /* {src_lang=Java}*/

    private static List<Manager_IF> serviceManager = new ArrayList<Manager_IF>();
    
    static DatabaseManager_T databaseManager = null;
    static MarketDataManager_T marketDataManager = null;
    static BrokerManager_T brokerManager = null;
    static LoggerManager_T loggerManager = null;

	/**
     * 
     */
    public DayTrader_T() {
        // TODO Auto-generated constructor stub
    }

    public static void main(String[] args) {
        
        initialize();
        run();
        terminate();
        
	}

	public static void initialize() {
	    
	    databaseManager = new DatabaseManager_T();
	    marketDataManager = new MarketDataManager_T();
		brokerManager = new BrokerManager_T();
		loggerManager = new LoggerManager_T();
		
		serviceManager.add(databaseManager);
		serviceManager.add(marketDataManager);
		serviceManager.add(brokerManager);
		serviceManager.add(loggerManager);
		
		
		Iterator<Manager_IF> it = serviceManager.iterator();
		while (it.hasNext()) {
		    Manager_IF mgr = it.next();
		    mgr.initialize();
		}
		
	}
	
	public static void run() {
		
	    
	    Iterator<Manager_IF> it = serviceManager.iterator();
        while (it.hasNext()) {
            Manager_IF mgr = it.next();
            mgr.run();
        }
        
	}
	
	public void sleep() {
		// TODO Auto-generated method stub
		
	}
	
	public static void terminate() {
	    Iterator<Manager_IF> it = serviceManager.iterator();
        while (it.hasNext()) {
            Manager_IF mgr = it.next();
            mgr.terminate();
        }
		
	}
	
	public void wakeup() {
		// TODO Auto-generated method stub
		
	}

}