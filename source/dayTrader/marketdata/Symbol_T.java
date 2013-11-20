/**
 * 
 */
package marketdata;

import interfaces.Persistable_IF;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import managers.DatabaseManager_T;

import org.hibernate.HibernateException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Type;
import org.hibernate.property.Getter;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.Setter;

import dayTrader.DayTrader_T;

import util.Exchange_T;

/**
 * @author nathan
 *
 */
@Entity
@Table(name="symbols")
public class Symbol_T implements PropertyAccessor, Persistable_IF {

    private long id;
    private String symbol;
    private String name;
    private String sector;
    private String industry;
    private String exchange;
    private double market_cap;
    //private Set<MarketData_T> quotes = new HashSet<MarketData_T>(0);
    
    /**
     * Constructor with no parameters. This is needed for hibernate to be able to instantiate the class
     */
    public Symbol_T() {
    }
    
    //SALxx- doesnt work!
    public Symbol_T(String symbol) {
        DatabaseManager_T databaseManager = (DatabaseManager_T) DayTrader_T.getManager(DatabaseManager_T.class);
        Symbol_T sym = databaseManager.getSymbol(symbol);
        if (sym != null) {
            this.id = sym.id;
            this.industry = sym.industry;
            this.market_cap = sym.market_cap;
            this.name = sym.name;
            this.sector = sym.sector;
            this.symbol = sym.symbol;
            this.exchange = sym.exchange;
        }
    }

    public Symbol_T(long symbolId) {
        DatabaseManager_T databaseManager = (DatabaseManager_T) DayTrader_T.getManager(DatabaseManager_T.class);
        Symbol_T sym = (Symbol_T) databaseManager.query(Symbol_T.class, symbolId);
        if (sym != null) {
            this.id = sym.id;
            this.industry = sym.industry;
            this.market_cap = sym.market_cap;
            this.name = sym.name;
            this.sector = sym.sector;
            this.symbol = sym.symbol;
            this.exchange = sym.exchange;
        }
    }
   
    
    /**
     * @return the id
     */
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    public long getId() {
        return id;
    }
    /**
     * @param id the id to set
     */
    public void setId(long id) {
        this.id = id;
    }
    /**
     * @return the quotes
     */
//    @JoinTable(name = "symbols_EndOfDayQuotes", joinColumns = 
//        { @JoinColumn(name = "id") }, inverseJoinColumns = 
//        { @JoinColumn(name = "symboldId") })
//    public Set<MarketData_T> getQuotes() {
//        return quotes;
//    }
//    /**
//     * @param quotes the quotes to set
//     */
//    public void setQuotes(Set<MarketData_T> quotes) {
//        this.quotes = quotes;
//    }
    /**
     * @return the symbol
     */
    @Column(name="symbol")
    public String getSymbol() {
        return symbol;
    }
    /**
     * @param symbol the symbol to set
     */
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    /**
     * @return the name
     */
    @Column(name="name")
    public String getName() {
        return name;
    }
    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * @return the sector
     */
    public String getSector() {
        return sector;
    }
    /**
     * @param sector the sector to set
     */
    public void setSector(String sector) {
        this.sector = sector;
    }
    /**
     * @return the industry
     */
    public String getIndustry() {
        return industry;
    }
    /**
     * @param industry the industry to set
     */
    public void setIndustry(String industry) {
        this.industry = industry;
    }
    /**
     * @return the exchange
     */
    public String getExchange() {
        return exchange;
    }
    
    /**
     * @param exchange the exchange to set
     */
    public void setExchange(String exchange) {
        this.exchange = exchange;
    }
    /**
     * @return the market_cap
     */
    public double getMarket_cap() {
        return market_cap;
    }
    /**
     * @param market_cap the market_cap to set
     */
    public void setMarket_cap(double market_cap) {
        this.market_cap = market_cap;
    }

    
    @Override
    public Getter getGetter(Class theClass, String propertyName)
            throws PropertyNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public Setter getSetter(Class theClass, String propertyName)
            throws PropertyNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public long insertOrUpdate() throws HibernateException {
//        Session session = DatabaseManager_T.getSessionFactory().openSession();
//        Transaction tx = null;
//        long id;
//        
//        try {
//            tx = session.beginTransaction();
//            id = (Long) session.save(this);
//            tx.commit();
//        } catch (HibernateException e) {
//            //TODO: for now just print to stdout, we'll change this to a log file later
//            e.printStackTrace();
//            if (tx != null) tx.rollback();
//            throw e;
//        } finally {
//            session.close();
//        }
//        
//        return id;
        
        return -1;
    }
    
    @Override
    public void delete() throws HibernateException {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void update() throws HibernateException {
        // TODO Auto-generated method stub
        
    }


    @Override
    public boolean existsInDB(Persistable_IF persistable) {
        // TODO Auto-generated method stub
        return false;
    }
    
}
