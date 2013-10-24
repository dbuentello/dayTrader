/**
 * 
 */
package util;

import interfaces.Persistable_IF;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.HibernateException;

/**
 * @author nathan
 *
 */
@Entity
@Table( name = "calendar" )
public class Calendar_T implements Persistable_IF {

    
    private Date date;
    private Date openTime;
    private Date closeTime;
    private boolean isMarketOpen;
    
    
    public Calendar_T() {
        
    }
    
    /**
     * @return the date
     */
    @Id
    @Column( name = "date" )
    @Temporal(value = TemporalType.DATE)
    public Date getDate() {
        return date;
    }
    /**
     * @param date the date to set
     */
    public void setDate(Date date) {
        this.date = date;
    }
    /**
     * @return the openTime
     */
    @Column( name = "open_time" )
    @Temporal(value = TemporalType.TIMESTAMP)
    public Date getOpenTime() {
        return openTime;
    }
    /**
     * @param openTime the openTime to set
     */
    public void setOpenTime(Date openTime) {
        this.openTime = openTime;
    }
    /**
     * @return the closeTime
     */
    @Column( name = "close_time" )
    @Temporal(value = TemporalType.TIMESTAMP)
    public Date getCloseTime() {
        return closeTime;
    }
    /**
     * @param closeTime the closeTime to set
     */
    public void setCloseTime(Date closeTime) {
        this.closeTime = closeTime;
    }
    /**
     * @return the isMarketOpen
     */
    @Column( name = "is_market_open" )
    public boolean isMarketOpen() {
        return isMarketOpen;
    }
    /**
     * @param isMarketOpen the isMarketOpen to set
     */
    public void setMarketOpen(boolean isMarketOpen) {
        this.isMarketOpen = isMarketOpen;
    }

    @Override
    public long insertOrUpdate() throws HibernateException {
        // TODO Auto-generated method stub
        return 0;
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
