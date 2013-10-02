package managers;

import interfaces.Connector_IF;
import interfaces.Manager_IF;
import interfaces.Persistable_IF;

import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

import trader.Holding_T;

/** 
 *  This class will manage the database connection. 
 *  It will be invoked to persist, update, retrieve and delete items from the database.
 *  In Java it will invoke the methods of the MySQL Connector/J class to interact with the database.
 */
/**
 * @author nathan
 *
 */
public class DatabaseManager_T implements Manager_IF, Connector_IF {
  /* {src_lang=Java}*/

    private static ServiceRegistryBuilder serviceRegistryBuilder = new ServiceRegistryBuilder();
    private static SessionFactory sessionFactory = null;

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
    public boolean isConnected() {
    	
    	return !(sessionFactory.isClosed());
    }
    
    @Override
    public void initialize() {
        
        
        serviceRegistryBuilder.configure();
        ServiceRegistry serviceRegistry = serviceRegistryBuilder.buildServiceRegistry();
        MetadataSources metaData = new MetadataSources(serviceRegistry);
    	metaData.addAnnotatedClass(marketdata.Symbol_T.class);
    	sessionFactory = metaData.buildMetadata().buildSessionFactory();
    	
    	metaData.addAnnotatedClass(marketdata.MarketData_T.class);
    	sessionFactory = metaData.buildMetadata().buildSessionFactory();
    	
    }
    
    @Override
    public void run() {
        //Nothing to do in the run state
    }
    
    @Override
    public void sleep() {
    	// TODO Auto-generated method stub
    	
    }
    
    @Override
    public void terminate() {
        if (isConnected()) {
            sessionFactory.close();
        }
    	
    }
    
    @Override
    public void wakeup() {
    	
    	
    }
    
//    @SuppressWarnings("deprecation")
//    private static SessionFactory buildSessionFactory() {
//        try {
//            // Create the SessionFactory from hibernate.cfg.xml
//            return new Configuration().configure().buildSessionFactory();
//        }
//        catch (Throwable ex) {
//            // Make sure you log the exception, as it might be swallowed
//            System.err.println("Initial SessionFactory creation failed." + ex);
//            throw new ExceptionInInitializerError(ex);
//        }
//    }
    
    
    /**
     * @return sessionFactory
     */
    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    
    public static <T> Persistable_IF retrieve(Class<T> persistableClass, long id) {
        Session session = DatabaseManager_T.getSessionFactory().openSession();
        Transaction tx = null;
        Persistable_IF persistentData = null;
        
        try {
            tx = session.beginTransaction();
            persistentData = (Persistable_IF) session.get(persistableClass, id);
            tx.commit();
        } catch (HibernateException e) {
            //TODO: for now just print to stdout, we'll change this to a log file later
            e.printStackTrace();
            if (tx != null) tx.rollback();
        } finally {
            session.close();
        }

        return persistentData;
    }
    
    public List<Holding_T> retrieveCurrentHoldings() {
        
        List<Holding_T> holdings = null;
        
        //TODO: implement query to get holdings
        //Use the IB API to get all
        
        return holdings;
        
    }
}