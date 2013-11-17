package managers;

import interfaces.Connector_IF;
import interfaces.Manager_IF;
import interfaces.Persistable_IF;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import marketdata.MarketData_T;
import marketdata.Symbol_T;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

import trader.Holding_T;
import trader.Trader_T;
import dayTrader.DayTrader_T;

/** 
 *  This class will manage the database connection. 
 *  It will be invoked to persist, update, retrieve and delete items from the database.
 *  In Java it will invoke the methods of the MySQL Connector/J class to interact with the database.
 */
/**
 * @author nathan and steve
 *
 */
public class DatabaseManager_T implements Manager_IF, Connector_IF {
  /* {src_lang=Java}*/

    private final ServiceRegistryBuilder serviceRegistryBuilder = new ServiceRegistryBuilder();
    private static SessionFactory sessionFactory = null;
    /** A reference to the time manager. */
    private TimeManager_T timeManager = null;

    /**
     * 
     */
    public DatabaseManager_T() {
        serviceRegistryBuilder.configure();
        ServiceRegistry serviceRegistry = serviceRegistryBuilder.buildServiceRegistry();
        MetadataSources metaData = new MetadataSources(serviceRegistry);
        
        metaData.addAnnotatedClass(marketdata.Symbol_T.class);
        sessionFactory = metaData.buildMetadata().buildSessionFactory();
        
        metaData.addAnnotatedClass(marketdata.MarketData_T.class);
        sessionFactory = metaData.buildMetadata().buildSessionFactory();
                
        metaData.addAnnotatedClass(util.Calendar_T.class);
        sessionFactory = metaData.buildMetadata().buildSessionFactory();
                
        metaData.addAnnotatedClass(trader.Holding_T.class);
        sessionFactory = metaData.buildMetadata().buildSessionFactory();
        
        metaData.addAnnotatedClass(marketdata.RTData_T.class);
        sessionFactory = metaData.buildMetadata().buildSessionFactory();

        metaData.addAnnotatedClass(trader.DailyNet_T.class);
        sessionFactory = metaData.buildMetadata().buildSessionFactory();
         
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
        
    	timeManager = (TimeManager_T) DayTrader_T.getManager(TimeManager_T.class);
    	
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
   
    
    /**
     * @return sessionFactory
     */
    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * Query the database for a single row. persistableClass will be the object class that maps to the table 
     * to query, id is the primary key id of the object
     * @param persistableClass
     * @param id
     * @return object if found in the database, otherwise null
     */
    public synchronized <T> Persistable_IF query(Class<T> persistableClass, long id) {
        Session session = getSessionFactory().openSession();
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
    
    /**
     * Query the database for a single row. persistableClass will be the object class that maps to the table 
     * to query, string is the primary key of the object
     * @param <T>
     * @param persistableClass
     * @param id
     * @return object if found in the database, otherwise null
     */
    public synchronized <T> Persistable_IF query(Class<T> persistableClass, String string) {
        Session session = getSessionFactory().openSession();
        Transaction tx = null;
        Persistable_IF persistentData = null;
        
        try {
            tx = session.beginTransaction();
            persistentData = (Persistable_IF) session.get(persistableClass, string);
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
    
    /**
     * Query the database for a single date row. persistableClass will be the object class that maps to the table 
     * to query, date is the primary key of the object
     * @param <T>
     * @param persistableClass
     * @param id
     * @return object if found in the database, otherwise null
     */
    public synchronized <T> Persistable_IF query(Class<T> persistableClass, Date date) {
        Session session = getSessionFactory().openSession();
        Transaction tx = null;
        Persistable_IF persistentData = null;
        
        try {
            tx = session.beginTransaction();
            persistentData = (Persistable_IF) session.get(persistableClass, date);
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
    
    public synchronized Symbol_T getSymbol(String symbolString) {
        
        Session session = getSessionFactory().openSession();
        Criteria criteria = session.createCriteria(Symbol_T.class)
            .add(Restrictions.eq("symbol", symbolString));
        Symbol_T symbol = (Symbol_T) criteria.list().get(0);
        
        session.close();
        
        return symbol;
    }
    
    /**
     * Query the database for all symbols in a given exchange
     * 
     * @param exchange
     * @return all found symbols for that exchange
     */
    public synchronized List<Symbol_T> getSymbolsByExchange(String exchange) {
        
        Session session = getSessionFactory().openSession();
        Criteria criteria = session.createCriteria(Symbol_T.class)
            .add(Restrictions.eq("exchange", exchange));
       
        @SuppressWarnings("unchecked")
        List<Symbol_T> results = criteria.list();
        
        
        session.close();
        
        return results;   
        
    }
    
    /**
     * Query the database for the day's biggest losers. This method will query the database
     * on our buy criteria and can be tweaked to optimize the stocks we buy.
     * 
     * @return List of the biggest losers
     */
    public synchronized List<Symbol_T> determineBiggestLosers() {
        
        Session session = getSessionFactory().openSession();
        
        Criteria criteria = session.createCriteria(MarketData_T.class)
//SALxx            .add(Restrictions.ge("date", timeManager.mysqlDate() + "00:00:00" ))
            .add(Restrictions.ge("lastTradeTimestamp", timeManager.getCurrentTradeDate() ))
            .add(Restrictions.gt("volume", Trader_T.MIN_TRADE_VOLUME))
//SALxx		.add(Restrictions.gt("price", Trader_T.MIN_BUY_PRICE))
            .add(Restrictions.gt("lastPrice", Trader_T.MIN_BUY_PRICE))
//SALxx            .addOrder(Order.asc("chgper"))
            .addOrder(Order.asc("percentChange"))
            .setMaxResults(Trader_T.MAX_BUY_POSITIONS);
        
        
        @SuppressWarnings("unchecked")
        List<MarketData_T> loserQuotes = criteria.list();
        
        session.close();
        
        List<Symbol_T> losers = new ArrayList<Symbol_T>();
        
        Iterator<MarketData_T> it = loserQuotes.iterator();
        while (it.hasNext()) {
            MarketData_T quote = it.next();
            losers.add(quote.getSymbol());
        }
        
        return losers;   
        
    }

    /**
     * Update the Holdings database with the symbolIds for the day's biggest losers.
     * Only todays date and symbol id are populated to indicate these are the candidates
     *
     * @returns nothing, but throws fatal exception on errors
     */
    
    public synchronized void updateHoldings(List<Symbol_T> losers) {
        
    	// for development only
    	Date date;
    	if (!DayTrader_T.d_useSimulateDate.isEmpty())
    		date = timeManager.getCurrentTradeDate();
    	else
    		date = timeManager.TimeNow();
    	
    	// TODO - move this to a delete method
        Session session = getSessionFactory().openSession();

        // deletes must be within a transaction
        Transaction tx = null;
        
        try {
            tx = session.beginTransaction();

            String hql = "DELETE FROM trader.Holding_T WHERE buy_date >= :date";
            Query query = session.createQuery(hql);
            query.setDate("date", date);

            int nrows = query.executeUpdate();
 
            tx.commit();
        } catch (HibernateException e) {
            //TODO: for now just print to stdout, we'll change this to a log file later
            e.printStackTrace();
            if (tx != null) tx.rollback();
        } finally {
            session.close();
        }      
       
        
        Iterator<Symbol_T> it = losers.iterator();
        while (it.hasNext()) {
            Symbol_T symbol = it.next();
            
            Holding_T holding = new Holding_T(0);	//SALxx - do we need an orderId now? - YUP!!!! TODO
            holding.setSymbol(symbol);
            holding.setBuyDate(date);
            
            holding.insertOrUpdate();
        }
        
    }    

    /**
     * get yesterdays (eg most recent) losers from our Holdings DB
     * 
     * @return returns a list of Symbols
     */
    public List<Symbol_T> getHoldings()
    {
    	Date date = timeManager.getPreviousTradeDate();
  	
    	//"SELECT symbol from Holdings WHERE DATE(buy_date) = \"$date\"";

    	Session session = getSessionFactory().openSession();
          
    	Criteria criteria = session.createCriteria(Holding_T.class)
              .add(Restrictions.ge("buyDate", date ));         
          
          @SuppressWarnings("unchecked")
          List<Holding_T> holdings = criteria.list();
          
          session.close();
          
          List<Symbol_T> losers = new ArrayList<Symbol_T>();
          
          Iterator<Holding_T> it = holdings.iterator();
          while (it.hasNext()) {
              Holding_T h = it.next();
              losers.add(h.getSymbol());
          }
        	  
          return losers;      	  
    	  
    }
}