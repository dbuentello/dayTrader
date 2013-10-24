/**
 * 
 */
package trader;

import interfaces.Persistable_IF;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import managers.DatabaseManager_T;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * @author nathan
 *
 */
@Entity( name = "OrderIds")
public class OrderId_T implements Persistable_IF {

    private long id;
    
    
    public OrderId_T() {
        
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


    @Override
    public long insertOrUpdate() throws HibernateException {
        
        id = -1;
        Session session = DatabaseManager_T.getSessionFactory().openSession();
        Transaction tx = null;
            
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


    @Override
    public boolean existsInDB(Persistable_IF persistable) {
        // TODO Auto-generated method stub
        return false;
    }
    
    
    
    
}
