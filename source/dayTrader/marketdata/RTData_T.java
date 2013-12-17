/**
 * 
 */
package marketdata;

import interfaces.Persistable_IF;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import managers.DatabaseManager_T;

import org.hibernate.HibernateException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Type;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.property.Getter;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.Setter;

import dayTrader.DayTrader_T;


/**
 * @author steve
 *
 */

@Entity
@Table(name="RealTimeQuotes")

public class RTData_T implements PropertyAccessor, Persistable_IF {

    private long id;
    private Date date;
    private String symbol;
    private Double price;	// market
    private Double ask;		// for Buys
    private Double bid;		// for Sells
    private long volume;
    
    /**
     * Constructor with no parameters. This is needed for hibernate to be able to instantiate the class
     */
    public RTData_T() {
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
     * @return the date
     */
    @Column(name="date")
    @Temporal(value = TemporalType.TIMESTAMP)
    public Date getDate() {
        return date;
    }
    /**
     * @param name the name to set
     */
    public void setDate(Date date) {
        this.date = date;
    }
    
    /**
     * @return the current price
     */
    @Column(name="price")
    public Double getPrice() {
        return price;
    }
    /**
     * @param price
     */
    public void setPrice(Double price) {
        this.price = price;
    }

    /**
     * @return the ask price (for buys)
     */
    @Column(name="ask")
    public Double getAskPrice() {
        return ask;
    }
    /**
     * @param price
     */
    public void setAskPrice(Double price) {
        this.ask = price;
    }

    /**
     * @return the bid price (for buys)
     */
    @Column(name="bid")
    public Double getBidPrice() {
        return bid;
    }
    /**
     * @param price
     */
    public void setBidPrice(Double price) {
        this.bid = price;
    }
     
    
    /**
     * @return the volume
     */
    @Column(name="volume")   
    public long getVolume() {
        return volume;
    }
    /**
     * @param volume
     */
    public void setVolume(long volume) {
        this.volume = volume;
    }

    
    @Override
    public Getter getGetter(Class theClass, String propertyName)
            throws PropertyNotFoundException {
        return null;
    }
    @Override
    public Setter getSetter(Class theClass, String propertyName)
            throws PropertyNotFoundException {
        return null;
    }
    
    //@Override
    public long insertOrUpdate() throws HibernateException {
        Session session = DatabaseManager_T.getSessionFactory().openSession();
        Transaction tx = null;
        long id=0;
        
        try {
            tx = session.beginTransaction();
            id = (Long) session.save(this);
            tx.commit();
        //SALxx - this is OK, but the warn/error is logged  we dont want that!  
        } catch(ConstraintViolationException  e) {                                                         
            //System.out.println("[DEBUG] caught constraint violation"); 
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
        
    }
    
    @Override
    public void update() throws HibernateException {
        
    }


    @Override
    public boolean existsInDB(Persistable_IF persistable) {

        return false;
    }
    
}