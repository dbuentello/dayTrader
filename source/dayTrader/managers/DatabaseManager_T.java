package managers;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import interfaces.Connector_IF;
import interfaces.LifeState_IF;

/** 
 *  This class will manage the database connection. 
 *  It will be invoked to persist, update, retrieve and delete items from the database.
 *  In Java it will invoke the methods of the MySQL Connector/J class to interact with the database.
 */
/**
 * @author nathan
 *
 */
public class DatabaseManager_T implements LifeState_IF, Connector_IF {
  /* {src_lang=Java}*/

    private static final SessionFactory sessionFactory = buildSessionFactory();

    /**
     * 
     */
    public DatabaseManager_T() {
        // TODO Auto-generated constructor stub
    }
    
    
    @Override
    public void connect() {
    	// TODO Auto-generated method stub
    	
    }
    
    @Override
    public void disconnect() {
    	// TODO Auto-generated method stub
    	
    }
    
    @Override
    public void isConnected() {
    	// TODO Auto-generated method stub
    	
    }
    
    @Override
    public void initialize() {
        
        // to use the add class method, the hbm.xml file should be in the same directory as the class file itself 
        Configuration cfg = new Configuration()
            .addClass(marketdata.MarketData_T.class);
    	
    }
    
    @Override
    public void run() {
    	// TODO Auto-generated method stub
    	
    }
    
    @Override
    public void sleep() {
    	// TODO Auto-generated method stub
    	
    }
    
    @Override
    public void terminate() {
    	sessionFactory.close();
    	
    }
    
    @Override
    public void wakeup() {
    	// TODO Auto-generated method stub
    	
    }
    
    @SuppressWarnings("deprecation")
    private static SessionFactory buildSessionFactory() {
        try {
            // Create the SessionFactory from hibernate.cfg.xml
            return new Configuration().configure().buildSessionFactory();
        }
        catch (Throwable ex) {
            // Make sure you log the exception, as it might be swallowed
            System.err.println("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }
    
    
    /**
     * @return sessionFactory
     */
    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

}