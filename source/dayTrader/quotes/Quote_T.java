package quotes;

import java.util.Date;
import com.ib.client.TickType;

public class Quote_T extends TickType {
  /* {src_lang=Java}*/


    /** 
     *  The previous day's close price in US dollars
     */
    private float prevCloseUSD;

    /** 
     *  The day's open price for the stock in US dollars
     */
    public float openUSD;

    /** 
     *  The bid price of the stock in US dollars as of {@code quoteTimestamp}.
     */
    public float bidUSD;

    /** 
     *  The ask price of the stock in US dollars as of {@code quoteTimestamp}.
     */
    public float askUSD;

    /** 
     *  The day trade volume for the stock as of {@code quoteTimestamp}.
     */
    public double daysVolume;

    /** 
     *  Timestamp that the quote was retrieved from the data source.
     */
    public Date quoteTimestamp;

    /** 
     *  The day's high price for the stock as of {@code quoteTimestamp}.
     */
    public float daysHigh;

  
    /**
     * 
     */
    public Quote_T() {
        // TODO Auto-generated constructor stub
    }


    /**
     * @param prevCloseUSD
     * @param openUSD
     * @param bidUSD
     * @param askUSD
     * @param daysVolume
     * @param quoteTimestamp
     * @param daysHigh
     */
    public Quote_T(float prevCloseUSD, float openUSD, float bidUSD,
            float askUSD, double daysVolume, Date quoteTimestamp, float daysHigh) {
        this.prevCloseUSD = prevCloseUSD;
        this.openUSD = openUSD;
        this.bidUSD = bidUSD;
        this.askUSD = askUSD;
        this.daysVolume = daysVolume;
        this.quoteTimestamp = quoteTimestamp;
        this.daysHigh = daysHigh;
    }

    
    
    
}