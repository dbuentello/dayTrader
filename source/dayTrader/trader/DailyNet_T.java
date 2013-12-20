/**
 * Persistable DailyNet Class for Holdings Summary
 */
package trader;

import interfaces.Persistable_IF;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import managers.DatabaseManager_T;

import org.hibernate.HibernateException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.property.Getter;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.Setter;


/**
 * @author steve
 *
 */

@Entity
@Table(name="DailyNet")

public class DailyNet_T implements PropertyAccessor, Persistable_IF {

    private long id = 0;
    private Date date;
    private Double price = null;
    private Double net = null;
    private Long volume = null;
    
    /**
     * Constructor with no parameters. This is needed for hibernate to be able to instantiate the class
     */
    public DailyNet_T() {
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
     * @return the price
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
     * @return the net profit/loss
     */
    @Column(name="net")
    public Double getNet() {
        return net;
    }
    /**
     * @param net the net value to set
     */
    public void setNet(Double net) {
        this.net = net;
    }
    
    /**
     * @return the volume
     */
    @Column(name="totalVolume")   
    public Long getVolume() {
        return volume;
    }
    /**
     * @param volume
     */
    public void setVolume(Long volume) {
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
    
    // note - this only inserts
    //@Override
    public long insertOrUpdate() throws HibernateException {
        Session session = DatabaseManager_T.getSessionFactory().openSession();
        Transaction tx = null;
        long id=0;
        
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
        
    }
    
    @Override
    public void update() throws HibernateException {
        
    }


    @Override
    public boolean existsInDB(Persistable_IF persistable) {
        return false;
    }


    /**
     *  update
     *  only because update doesnt work!
     *  
     *  @return 1 on update, else 0
     */
    //TODO: deprecate
    public int updateNet(Double net, long vol, long id) throws HibernateException {
 
    	Session session = DatabaseManager_T.getSessionFactory().openSession();
        Transaction tx = null;
        
        int nrows = 0;
        
        try {
            tx = session.beginTransaction();
        	String hql = "UPDATE trader.DailyNet_T " +
        			"SET net = :net, volume = :vol " +
        			"WHERE id = :id";
        	Query query = session.createQuery(hql);
        	query.setDouble("net", net);
        	query.setParameter("vol", vol);
        	query.setParameter("id", id);

        	nrows = query.executeUpdate();
            tx.commit();
        } catch (HibernateException e) {
            //TODO: for now just print to stdout, we'll change this to a log file later
            e.printStackTrace();
            if (tx != null) tx.rollback();
            throw e;
        } finally {
            session.close();
        }
        
        return nrows;
        
    }
        
    public int updateNet() throws HibernateException {
    	 
    	Session session = DatabaseManager_T.getSessionFactory().openSession();
        Transaction tx = null;
        
        int nrows = 0;
        
        try {
            tx = session.beginTransaction();
        	String hql = "UPDATE trader.DailyNet_T " +
        			"SET net = :net, volume = :vol " +
        			"WHERE id = :id";
        	Query query = session.createQuery(hql);
        	query.setDouble("net", this.net);
        	query.setParameter("vol", this.volume);
        	query.setParameter("id", this.id);

        	nrows = query.executeUpdate();
            tx.commit();
        } catch (HibernateException e) {
            //TODO: for now just print to stdout, we'll change this to a log file later
            e.printStackTrace();
            if (tx != null) tx.rollback();
            throw e;
        } finally {
            session.close();
        }
        
        return nrows;
        
    }

}