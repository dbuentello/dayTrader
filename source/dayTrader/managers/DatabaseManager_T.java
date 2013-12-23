package managers;

import interfaces.Connector_IF;
import interfaces.Manager_IF;
import interfaces.Persistable_IF;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import marketdata.MarketData_T;
import marketdata.RTData_T;
import marketdata.Symbol_T;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

import trader.Holding_T;
import trader.TraderCalculator_T;
import trader.Trader_T;
import util.Exchange_T;
import util.Utilities_T;
import util.XMLTags_T;

import com.ib.controller.OrderStatus;

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
        
        ConfigurationManager_T cfgMgr = (ConfigurationManager_T) DayTrader_T.getManager(ConfigurationManager_T.class);
        
        serviceRegistryBuilder.applySetting("hibernate.connection.url", "jdbc:mysql://localhost:3306/" + cfgMgr.getConfigParam(XMLTags_T.CFG_DATABASE_NAME))
            .applySetting("hibernate.connection.username", cfgMgr.getConfigParam(XMLTags_T.CFG_DATABASE_USER))
            .applySetting("hibernate.connection.password", cfgMgr.getConfigParam(XMLTags_T.CFG_DATABASE_PASSWORD));
        
        ServiceRegistry serviceRegistry = serviceRegistryBuilder.buildServiceRegistry();
        serviceRegistryBuilder.configure();
        
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
        
        Date d2 = Utilities_T.tomorrow(timeManager.getCurrentTradeDate());
        
        Criteria criteria = session.createCriteria(MarketData_T.class)
            .add(Restrictions.between("lastTradeTimestamp", timeManager.getCurrentTradeDate(), d2 ))
        	.add(Restrictions.gt("volume", Trader_T.MIN_TRADE_VOLUME))
            .add(Restrictions.gt("lastPrice", Trader_T.MIN_BUY_PRICE))
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
     * Only todays date as buy-date, buy action and symbol id are populated to indicate
     * these are the candidates
     * 
     * All previous entries for today are delete so we only have one set of entries
     *
     * @returns nothing, but throws fatal exception on errors
     */
    
    public synchronized void addHoldings(List<Symbol_T> losers) {
        
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

 //SALxx TODO date between??
            String hql = "DELETE FROM trader.Holding_T WHERE buy_date >= :date";
            Query query = session.createQuery(hql);
            query.setDate("date", date);

            query.executeUpdate();
 
            tx.commit();
        } catch (HibernateException e) {
            //TODO: for now just print to stdout, we'll change this to a log file later
            e.printStackTrace();
            if (tx != null) tx.rollback();
        } finally {
            session.close();
        }      
       
        // create initial holdings entries in the DB.  All it contains is the
        // symbol and buy date.  All other information (including the orderid) will
        // need to be filled in before submission
        TraderCalculator_T tCalc = new TraderCalculator_T();
        
    	double totalCapital = tCalc.getCapital();
    	double buyTotal = totalCapital/Trader_T.MAX_BUY_POSITIONS;
    	
        Iterator<Symbol_T> it = losers.iterator();
        while (it.hasNext()) {
            Symbol_T symbol = it.next();
            
            Holding_T holding = new Holding_T();
            holding.setSymbol(symbol);
            holding.setBuyDate(date);
            
            // get the current ask(buy) price from EOD and calc number of shares to buy
            double buyPrice = getEODAskPrice(symbol);
            int buyVolume = (int)(buyTotal/buyPrice);
            //double adjustedBuyTotal = buyVolume * buyPrice;
            
            holding.setBuyPrice(buyPrice);
            holding.setVolume(buyVolume);
            
            // also initialize these...
            holding.setRemaining(buyVolume);
            holding.setOrderStatus(OrderStatus.PreSubmitted.toString());
            
            holding.insertOrUpdate();
        }
        
    }    

    /**
     * get yesterdays (eg most recent) losers from our Holdings DB
     * used to retrieve RT market data
     * 
     * @return returns a list of Symbols
     */
    public List<Symbol_T> getHoldingsSymbols()
    {
    	Date date = timeManager.getPreviousTradeDate();  	
    	
    	//"SELECT symbol from Holdings WHERE DATE(buy_date) = \"$date\"";

    	Session session = getSessionFactory().openSession();
          
    	Criteria criteria = session.createCriteria(Holding_T.class)
        .add(Restrictions.between("buyDate", date, Utilities_T.tomorrow(date) ));         
          
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
 
    /*
     * Get Holdings for this date - this is used for any retrieval done for Today, eg
     * Buying todays holdings
     */
    public List<Holding_T> getCurrentHoldings(Date buyDate)
    {
    	//"SELECT * from Holdings WHERE DATE(buy_date) = \"$date\"";

    	Session session = getSessionFactory().openSession();
          
    	Criteria criteria = session.createCriteria(Holding_T.class)       
                .add(Restrictions.between("buyDate", buyDate, Utilities_T.tomorrow(buyDate) ));         
          
          @SuppressWarnings("unchecked")
          List<Holding_T> holdings = criteria.list();
          
          session.close();
                 	  
          return holdings;      	  
    	  
    }

    /*
     * Get a Holding by SymbolId for this buy date
     * 
     */
    public Holding_T getCurrentHolding(long symbolId, Date buyDate)
    {
    	//"SELECT * from Holdings WHERE symbolId = sid AND DATE(buy_date) = \"$date\"";

    	Session session = getSessionFactory().openSession();
          
    	Criteria criteria = session.createCriteria(Holding_T.class)
                .add(Restrictions.eq("symbolId", symbolId ))         
                .add(Restrictions.between("buyDate", buyDate, Utilities_T.tomorrow(buyDate) ));         
          
          @SuppressWarnings("unchecked")
          List<Holding_T> holdings = criteria.list();
          
          session.close();
          
          if (holdings.size() != 1)
          {
        	  // TODO: This is real bad!!!  There must be one and only 1 holding
          }
          
          return holdings.get(0);      	  
    	  
    }
    /**
     * Get all the orders with order status "Submitted"
     * 
     * @return List of Holdings
     */
    public List<Holding_T> getSubmittedOrders() {

        Session session = getSessionFactory().openSession();

        Criteria criteria = session.createCriteria(Holding_T.class)
                .add(Restrictions.disjunction()
                		.add(Restrictions.eq("orderStatus", OrderStatus.Submitted.toString()))
                        .add(Restrictions.eq("orderStatus", OrderStatus.PreSubmitted.toString()))
                        .add(Restrictions.eq("orderStatus", OrderStatus.Inactive.toString())));
       
        @SuppressWarnings("unchecked")
        List<Holding_T> results = criteria.list();        
        
        session.close();

        return results;
    }
    
    /**
     * Get todays End of Day Price for this symbol from EODQuote database
     * 
     * @return (double) price
     */ 
/*** NOT USED    
    public double getEODPrice(Symbol_T symbol)
    {
    	Date date = timeManager.getCurrentTradeDate();
    	
    	//"SELECT price from EndOfDayQuotes where symbol = \"$symbol\" AND DATE(date) = \"$date\"";
        Session session = getSessionFactory().openSession();
        
        Criteria criteria = session.createCriteria(MarketData_T.class)
            .add(Restrictions.between("lastTradeTimestamp", date, Utilities_T.tomorrow(date)))
            .add(Restrictions.eq("symbolId", symbol.getId()))
            .addOrder(Order.desc("lastTradeTimestamp"))
            .setMaxResults(1); 
            
        @SuppressWarnings("unchecked")
        List<MarketData_T> quoteData = criteria.list();
        
        session.close();
        
        if (quoteData.size() != 1) {
        	//Log.println("[ERROR] Bad EOD price for "+symbol.getSymbol());
        	return 0.0;
        }
        
        double price = quoteData.get(0).getLastPrice();
        return price;
            	
    }
***/
    
    /**
     * Get todays End of Day Ask Price for this symbol from EODQuote database
     * This is the desired buy price
     * 
     * @return (double) price
     */ 
    public double getEODAskPrice(Symbol_T symbol)
    {
    	Date date = timeManager.getCurrentTradeDate();
    	
    	//"SELECT price from EndOfDayQuotes where symbol = \"$symbol\" AND DATE(date) = \"$date\"";
        Session session = getSessionFactory().openSession();
        
        Criteria criteria = session.createCriteria(MarketData_T.class)
            .add(Restrictions.between("lastTradeTimestamp", date, Utilities_T.tomorrow(date)))
            .add(Restrictions.eq("symbolId", symbol.getId()))
            .addOrder(Order.desc("lastTradeTimestamp"))
            .setMaxResults(1);
        
        @SuppressWarnings("unchecked")
        List<MarketData_T> quoteData = criteria.list();
        
        session.close();
        
        if (quoteData.size() != 1) {
        	//Log.println("[ERROR] Bad EOD Ask price for "+symbol.getSymbol());
        	return 0.0;
        }
        
        double price = quoteData.get(0).getAskPrice();
        return price;
            	
    }

    /**
     * Get todays most recent Bid Price for this symbol from RealTimeQuote database
     * This is the desired sell price
     * 
     * @return (double) price
     */ 
    public double getCurrentBidPrice(Symbol_T symbol)
    {
    	Date date = timeManager.getCurrentTradeDate();
    	
    	//"SELECT price from EndOfDayQuotes where symbol = \"$symbol\" AND DATE(date) = \"$date\"";
        Session session = getSessionFactory().openSession();
        
        Criteria criteria = session.createCriteria(RTData_T.class)
            .add(Restrictions.between("date", date, Utilities_T.tomorrow(date)))
            // TODO: RT still uses symbol, not symbolId!
            .add(Restrictions.eq("symbol", symbol.getSymbol()))
            .addOrder(Order.desc("date"))
            .setMaxResults(1); 


        @SuppressWarnings("unchecked")
        List<RTData_T> quoteData = criteria.list();
        
        session.close();
        
        if (quoteData.size() != 1) {
        	//Log.println("[ERROR] Bad Current Bid price for "+symbol.getSymbol());
        	return 0.0;
        }
        
        double price = quoteData.get(0).getBidPrice();
        return price;
            	
    }

    // for reporting
    /**
     * Get the market data for this symbol from EODQuote database for this date
     * 
     * @return MarketData_T, or null on error
     */ 
    public MarketData_T getMarketData(long symbolId, Date date)
    {	
    	//"SELECT * from EndOfDayQuotes where symbol = \"$symbol\" AND DATE(date) = \"$date\"";
        Session session = getSessionFactory().openSession();
        
        Criteria criteria = session.createCriteria(MarketData_T.class)
            .add(Restrictions.between("lastTradeTimestamp", date, Utilities_T.tomorrow(date)))
            .add(Restrictions.eq("symbolId", symbolId))
            .addOrder(Order.desc("lastTradeTimestamp"))
            .setMaxResults(1);
        
        @SuppressWarnings("unchecked")
        List<MarketData_T> quoteData = criteria.list();
        
        session.close();

        if (quoteData.size() != 1) {
        	return null;
        }
        
        return quoteData.get(0);
        	
    }
    
    /**
     * Get the most recent numOfQuotes this symbol from EODQuote table
     * 
     * @return MarketData_T, or null on error
     */ 
    public List<MarketData_T> getRecentQuotes(long symbolId, int numOfQuotes)
    {   
        
        Session session = getSessionFactory().openSession();
        
        Criteria criteria = session.createCriteria(MarketData_T.class)
            .add(Restrictions.eq("symbolId", symbolId))
            .addOrder(Order.desc("lastTradeTimestamp"))
            .setMaxResults(numOfQuotes);

        @SuppressWarnings("unchecked")
        List<MarketData_T> quoteData = criteria.list();
        
        session.close();

        return quoteData;
            
    }
    
    
    public void bulkMarketDataInsert(ArrayList<MarketData_T> data) {
        
        Session session = DatabaseManager_T.getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();
        
        for (int i = 0; i < data.size(); i++) {
            try {
                session.save(data.get(i));
                if ( i % 50 == 0 ) { //50, same as the JDBC batch size
                    //flush a batch of inserts and release memory:
                    session.flush();
                    session.clear();
                }
            } catch (HibernateException e) {
                //TODO: for now just print to stdout, we'll change this to a log file later
                e.printStackTrace();
            }
        }
        tx.commit();
        session.close();
        
        return;
    }
    
    
    public void updateSymbolAverages() {
        
        long time = System.currentTimeMillis();
        
        Session session = DatabaseManager_T.getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();
        
        ScrollableResults symbols = session.getNamedQuery("getNasdaqSymbols")
                .setString("exchange", Exchange_T.NASDAQ)
                .setCacheMode(CacheMode.IGNORE)
                .scroll(ScrollMode.FORWARD_ONLY);
        int i = 0;
        //for (int i = 0; i < symbols.size(); i++) {
        while(symbols.next()) {
            Symbol_T symbol = (Symbol_T) symbols.get(0);
            try {
                symbol.calcAverages();
                String hql = "UPDATE marketdata.Symbol_T " +
                        "SET avg_vol_15d = :avgVolume15day, avg_bid_ask_15d = :avgBidAsk15day " +
                        "WHERE id = :id";
                Query query = session.createQuery(hql)
                        .setDouble("avgBidAsk15day", symbol.getAvgBidAsk15day())
                        .setParameter("avgVolume15day", symbol.getAvgVolume15day())
                        .setParameter("id", symbol.getId());

                query.executeUpdate();
               // session.update(symbol);
//                if ( ++i % 50 == 0 ) { //50, same as the JDBC batch size
//                    //flush a batch of inserts and release memory:
//                    session.flush();
//                    session.clear();
//                }
            } catch (HibernateException e) {
                //TODO: for now just print to stdout, we'll change this to a log file later
                e.printStackTrace();
            }
        }
        
        time = System.currentTimeMillis() - time;
        time /= 1000;
        System.out.println("time to execute = " + time);
        tx.commit();
        session.close();
        
        return;
    }
}
