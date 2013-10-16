package marketdata;

import interfaces.Persistable_IF;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import managers.DatabaseManager_T;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import dayTrader.DayTrader_T;


/******************************************************
 * Class used to represent a snapshot of data
 * 
 * @author nathan
 *
 */

@Entity
@Table(name="EndOfDayQuotes")
public class MarketData_T implements Persistable_IF {
  /* {src_lang=Java}*/


    //this is a persistable class so need an id attribute
    private long id;
    
    private long symbolId;
    private Symbol_T symbol;
    //these are the data fields we will store to persistence
    private Double bidSize = null;
    // The highest price anyone has declared that they are willing to pay for a security.
    private Double bidPrice = null;
    //  The lowest price anyone will offer to sell a security.  
    private Double askPrice = null;
    private Double askSize = null;
    // The price of the last trade for this symbol.
    private Double lastPrice = null;
    private Double lastSize = null;
    private Double high = null;
    private Double low = null;
    private Double volume = null;
    private Double close = null;
    private Double open = null;
    private Date lastTradeTimestamp = null;
    private Double change = null;
    private Double percentChange = null;
    private Double weekLow52 = null;
    private Double weekHigh52 = null;
    
    
/*    
 These are additional fields that are provided by IB
    private Double trades = null;
    private Double avgVolume = null;
    private Double weekLow13 = null;
    private Double weekHigh13 = null;
    private Double weekLow26 = null;
    private Double weekHigh26 = null;
    private Double bidOptComp = null;
    private Double askOptComp = null;
    private Double lastOptComp = null;
    private Double modelOptComp = null;
    private Double openInterest = null;
    private Double optionHistoricalVolatility = null;
    private Double optionImpliedVolatility = null;
    private Double optionBidExchStr = null;
    private Double optionAskExchStr = null;
    private Double optionCallOpenInterest = null;
    private Double optionPutOpenInterest = null;
    private Double optionCallVolume = null;
    private Double optionPutVolume = null;
    private Double indexFuturePremium = null;
    private Double bidExch = null;
    private Double askExch = null;
    private Double auctionVolume = null;
    private Double auctionPrice = null;
    private Double auctionImbalance = null;
    private Double markPrice = null;
    private Double bidEFP = null;
    private Double askEFP = null;
    private Double lastEFP = null;
    private Double openEFP = null;
    private Double highEFP = null;
    private Double lowEFP = null;
    private Double closeEFP = null;
    private Double shortable = null;
    private Double fundamentals = null;
    private Double rtVolume = null;
    private Double halted = null;
    private Double bidYield = null;
    private Double askYield = null;
    private Double lastYield = null;             
    private Double custOptComp = null;             
    private Double trades_min = null;
    private Double volume_min = null;             
    private Double lastRTHTrade = null;   
    */
  
    /**
     * 
     */
    public MarketData_T() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public long insert() {
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
    public void delete() {
        Session session = DatabaseManager_T.getSessionFactory().openSession();
        Transaction tx = null;
        
//        try {
//            tx = session.beginTransaction();
//            marketData = (MarketData_T) session.get(MarketData_T.class, id);
//            tx.commit();
//        } catch (HibernateException e) {
//            //TODO: for now just print to stdout, we'll change this to a log file later
//            e.printStackTrace();
//            if (tx != null) tx.rollback();
//        } finally {
//            session.close();
//        }
        
    }
    
    @Override
    public void update() throws HibernateException {
        // TODO Auto-generated method stub
        
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
    private void setId(long id) {
        this.id = id;
    }


   
    /**
     * @return the symbolId
     */
    public long getSymbolId() {
        return symbolId;
    }

    /**
     * @param symbolId the symbolId to set
     */
    public void setSymbolId(long symbolId) {
        this.symbolId = symbolId;
    }

    @Transient
    public Symbol_T getSymbol() {
        
        setSymbol();
        
        return symbol;
    }
    
    public void setSymbol() {
        if (symbol == null) {
            DatabaseManager_T databaseManager = (DatabaseManager_T) DayTrader_T.getManager(DatabaseManager_T.class);
            symbol = (Symbol_T) databaseManager.query(Symbol_T.class, this.symbolId);
        }
    }
    
    /**
     * @return the lastTradeTimestamp
     */
    @Column( name = "date" )
    @Temporal(value = TemporalType.TIMESTAMP)
    public Date getLastTimestamp() {
        return lastTradeTimestamp;
    }


    /**
     * @param lastTradeTimestamp the lastTradeTimestamp to set
     */
    public void setLastTimestamp(Date lastTimestamp) {
        this.lastTradeTimestamp = lastTimestamp;
    }

    /**
     * @return the bidSize
     */
    public Double getBidSize() {
        return bidSize;
    }

    /**
     * @param bidSize the bidSize to set
     */
    public void setBidSize(Double bidSize) {
        this.bidSize = bidSize;
    }

    /**
     * @return the bidPrice
     */
    @Column( name = "bid" )
    public Double getBidPrice() {
        return bidPrice;
    }

    /**
     * @param bidPrice the bidPrice to set
     */
    public void setBidPrice(Double bidPrice) {
        this.bidPrice = bidPrice;
    }

    /**
     * @return the askPrice
     */
    @Column( name = "ask" )
    public Double getAskPrice() {
        return askPrice;
    }

    /**
     * @param askPrice the askPrice to set
     */
    public void setAskPrice(Double askPrice) {
        this.askPrice = askPrice;
    }

    /**
     * @return the askSize
     */
    public Double getAskSize() {
        return askSize;
    }

    /**
     * @param askSize the askSize to set
     */
    public void setAskSize(Double askSize) {
        this.askSize = askSize;
    }

    /**
     * @return the lastPrice
     */
    @Column( name = "price" )
    public Double getLastPrice() {
        return lastPrice;
    }

    /**
     * @param lastPrice the lastPrice to set
     */
    public void setLastPrice(Double lastPrice) {
        this.lastPrice = lastPrice;
    }

    /**
     * @return the lastSize
     */
    @Transient
    public Double getLastSize() {
        return lastSize;
    }

    /**
     * @param lastSize the lastSize to set
     */
    public void setLastSize(Double lastSize) {
        this.lastSize = lastSize;
    }

    /**
     * @return the high
     */
    public Double getHigh() {
        return high;
    }

    /**
     * @param high the high to set
     */
    public void setHigh(Double high) {
        this.high = high;
    }

    /**
     * @return the low
     */
    public Double getLow() {
        return low;
    }

    /**
     * @param low the low to set
     */
    public void setLow(Double low) {
        this.low = low;
    }

    /**
     * @return the volume
     */
    public Double getVolume() {
        return volume;
    }

    /**
     * @param volume the volume to set
     */
    public void setVolume(Double volume) {
        this.volume = volume;
    }

    /**
     * @return the close
     */
    public Double getClose() {
        return close;
    }

    /**
     * @param close the close to set
     */
    public void setClose(Double close) {
        this.close = close;
    }

    /**
     * @return the open
     */
    public Double getOpen() {
        return open;
    }

    /**
     * @param open the open to set
     */
    public void setOpen(Double open) {
        this.open = open;
    }

    /**
     * @return the change
     */
    @Column (name = "chng")
    public Double getChange() {
        return change;
    }

    /**
     * @param change the change to set
     */
    public void setChange(Double change) {
        this.change = change;
    }

    /**
     * @return the percentChange
     */
    @Column (name = "chgper")
    public Double getPercentChange() {
        return percentChange;
    }

    /**
     * @param percentChange the percentChange to set
     */
    public void setPercentChange(Double percentChange) {
        this.percentChange = percentChange;
    }

    /**
     * @return the weekLow52
     */
    @Column( name = "yearlo" )
    public Double getWeekLow52() {
        return weekLow52;
    }

    /**
     * @param weekLow52 the weekLow52 to set
     */
    public void setWeekLow52(Double weekLow52) {
        this.weekLow52 = weekLow52;
    }

    /**
     * @return the weekHigh52
     */
    @Column (name = "yearhi")
    public Double getWeekHigh52() {
        return weekHigh52;
    }

    /**
     * @param weekHigh52 the weekHigh52 to set
     */
    public void setWeekHigh52(Double weekHigh52) {
        this.weekHigh52 = weekHigh52;
    }

    
    
    
}