/**
 * 
 */
package marketdata;

import java.util.HashSet;
import java.util.Set;

import interfaces.Persistable_IF;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import managers.DatabaseManager_T;

import org.hibernate.HibernateException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.property.Getter;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.Setter;

import util.Exchange_E;

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
    private Exchange_E exchange;
    private double market_cap;
    //private Set<MarketData_T> quotes = new HashSet<MarketData_T>(0);
    
    /**
     * Constructor with no parameters. This is needed for hibernate to be able to instantiate the class
     */
    public Symbol_T() {
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
    @Enumerated(EnumType.STRING)
    public Exchange_E getExchange() {
        return exchange;
    }
    /**
     * @param exchange the exchange to set
     */
    public void setExchange(Exchange_E exchange) {
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
    public long insert() throws HibernateException {
        Session session = DatabaseManager_T.getSessionFactory().openSession();
        Transaction tx = null;
        long id;
        
        try {
            tx = session.beginTransaction();
            id = (Long) session.save(this);
            tx.commit();
        } catch (HibernateException e) {
            //TODO: for now just print to stdout, we'll change this to a log file later
            e.printStackTrace();
            if (tx != null) tx.rollback();
            throw e;
        } finally {
            session.close();
        }
        
        return id;
    }
    
    @Override
    public void delete() throws HibernateException {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void update() throws HibernateException {
        // TODO Auto-generated method stub
        
    }
    
}
