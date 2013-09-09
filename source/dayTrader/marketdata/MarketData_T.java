package marketdata;

import java.util.Date;

/******************************************************
 * Class used to represent a snopshot of data
 * 
 * @author nathan
 *
 */
public class MarketData_T {
  /* {src_lang=Java}*/


    //this is a persistable class so need an id attribute
    private long id;
    
    private Symbol_T symbol;
    
    //these are the data fields we will store to persistence
    private Double bidSize = null;
    private Double bidPrice = null;
    private Double askPrice = null;
    private Double askSize = null;
    private Double lastPrice = null;
    private Double lastSize = null;
    private Double high = null;
    private Double low = null;
    private Double volume = null;
    private Double close = null;
    private Double open = null;
    private Double weekLow13 = null;
    private Double weekHigh13 = null;
    private Double weekLow26 = null;
    private Double weekHigh26 = null;
    private Double weekLow52 = null;
    private Double weekHigh52 = null;
    private Double avgVolume = null;
    private Date lastTimestamp = null;
    private Double trades = null;
    
/*    
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


    /**
     * @return the id
     */
    public int getId() {
        return id;
    }


    /**
     * @param id the id to set
     */
    private void setId(int id) {
        this.id = id;
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
     * @return the bidOptComp
     */
    public Double getBidOptComp() {
        return bidOptComp;
    }


    /**
     * @param bidOptComp the bidOptComp to set
     */
    public void setBidOptComp(Double bidOptComp) {
        this.bidOptComp = bidOptComp;
    }


    /**
     * @return the askOptComp
     */
    public Double getAskOptComp() {
        return askOptComp;
    }


    /**
     * @param askOptComp the askOptComp to set
     */
    public void setAskOptComp(Double askOptComp) {
        this.askOptComp = askOptComp;
    }


    /**
     * @return the lastOptComp
     */
    public Double getLastOptComp() {
        return lastOptComp;
    }


    /**
     * @param lastOptComp the lastOptComp to set
     */
    public void setLastOptComp(Double lastOptComp) {
        this.lastOptComp = lastOptComp;
    }


    /**
     * @return the modelOptComp
     */
    public Double getModelOptComp() {
        return modelOptComp;
    }


    /**
     * @param modelOptComp the modelOptComp to set
     */
    public void setModelOptComp(Double modelOptComp) {
        this.modelOptComp = modelOptComp;
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
     * @return the weekLow13
     */
    public Double getWeekLow13() {
        return weekLow13;
    }


    /**
     * @param weekLow13 the weekLow13 to set
     */
    public void setWeekLow13(Double weekLow13) {
        weekLow13 = weekLow13;
    }


    /**
     * @return the weekHigh13
     */
    public Double getWeekHigh13() {
        return weekHigh13;
    }


    /**
     * @param weekHigh13 the weekHigh13 to set
     */
    public void setWeekHigh13(Double weekHigh13) {
        weekHigh13 = weekHigh13;
    }


    /**
     * @return the weekLow26
     */
    public Double getWeekLow26() {
        return weekLow26;
    }


    /**
     * @param weekLow26 the weekLow26 to set
     */
    public void setWeekLow26(Double weekLow26) {
        weekLow26 = weekLow26;
    }


    /**
     * @return the weekHigh26
     */
    public Double getWeekHigh26() {
        return weekHigh26;
    }


    /**
     * @param weekHigh26 the weekHigh26 to set
     */
    public void setWeekHigh26(Double weekHigh26) {
        weekHigh26 = weekHigh26;
    }


    /**
     * @return the weekLow52
     */
    public Double getWeekLow52() {
        return weekLow52;
    }


    /**
     * @param weekLow52 the weekLow52 to set
     */
    public void setWeekLow52(Double weekLow52) {
        weekLow52 = weekLow52;
    }


    /**
     * @return the weekHigh52
     */
    public Double getWeekHigh52() {
        return weekHigh52;
    }


    /**
     * @param weekHigh52 the weekHigh52 to set
     */
    public void setWeekHigh52(Double weekHigh52) {
        weekHigh52 = weekHigh52;
    }


    /**
     * @return the avgVolume
     */
    public Double getAvgVolume() {
        return avgVolume;
    }


    /**
     * @param avgVolume the avgVolume to set
     */
    public void setAvgVolume(Double avgVolume) {
        avgVolume = avgVolume;
    }


    /**
     * @return the openInterest
     */
    public Double getOpenInterest() {
        return openInterest;
    }


    /**
     * @param openInterest the openInterest to set
     */
    public void setOpenInterest(Double openInterest) {
        openInterest = openInterest;
    }


    /**
     * @return the optionHistoricalVolatility
     */
    public Double getOptionHistoricalVolatility() {
        return optionHistoricalVolatility;
    }


    /**
     * @param optionHistoricalVolatility the optionHistoricalVolatility to set
     */
    public void setOptionHistoricalVolatility(Double optionHistoricalVolatility) {
        optionHistoricalVolatility = optionHistoricalVolatility;
    }


    /**
     * @return the optionImpliedVolatility
     */
    public Double getOptionImpliedVolatility() {
        return optionImpliedVolatility;
    }


    /**
     * @param optionImpliedVolatility the optionImpliedVolatility to set
     */
    public void setOptionImpliedVolatility(Double optionImpliedVolatility) {
        optionImpliedVolatility = optionImpliedVolatility;
    }


    /**
     * @return the optionBidExchStr
     */
    public Double getOptionBidExchStr() {
        return optionBidExchStr;
    }


    /**
     * @param optionBidExchStr the optionBidExchStr to set
     */
    public void setOptionBidExchStr(Double optionBidExchStr) {
        optionBidExchStr = optionBidExchStr;
    }


    /**
     * @return the optionAskExchStr
     */
    public Double getOptionAskExchStr() {
        return optionAskExchStr;
    }


    /**
     * @param optionAskExchStr the optionAskExchStr to set
     */
    public void setOptionAskExchStr(Double optionAskExchStr) {
        optionAskExchStr = optionAskExchStr;
    }


    /**
     * @return the optionCallOpenInterest
     */
    public Double getOptionCallOpenInterest() {
        return optionCallOpenInterest;
    }


    /**
     * @param optionCallOpenInterest the optionCallOpenInterest to set
     */
    public void setOptionCallOpenInterest(Double optionCallOpenInterest) {
        optionCallOpenInterest = optionCallOpenInterest;
    }


    /**
     * @return the optionPutOpenInterest
     */
    public Double getOptionPutOpenInterest() {
        return optionPutOpenInterest;
    }


    /**
     * @param optionPutOpenInterest the optionPutOpenInterest to set
     */
    public void setOptionPutOpenInterest(Double optionPutOpenInterest) {
        optionPutOpenInterest = optionPutOpenInterest;
    }


    /**
     * @return the optionCallVolume
     */
    public Double getOptionCallVolume() {
        return optionCallVolume;
    }


    /**
     * @param optionCallVolume the optionCallVolume to set
     */
    public void setOptionCallVolume(Double optionCallVolume) {
        optionCallVolume = optionCallVolume;
    }


    /**
     * @return the optionPutVolume
     */
    public Double getOptionPutVolume() {
        return optionPutVolume;
    }


    /**
     * @param optionPutVolume the optionPutVolume to set
     */
    public void setOptionPutVolume(Double optionPutVolume) {
        optionPutVolume = optionPutVolume;
    }


    /**
     * @return the indexFuturePremium
     */
    public Double getIndexFuturePremium() {
        return indexFuturePremium;
    }


    /**
     * @param indexFuturePremium the indexFuturePremium to set
     */
    public void setIndexFuturePremium(Double indexFuturePremium) {
        indexFuturePremium = indexFuturePremium;
    }


    /**
     * @return the bidExch
     */
    public Double getBidExch() {
        return bidExch;
    }


    /**
     * @param bidExch the bidExch to set
     */
    public void setBidExch(Double bidExch) {
        this.bidExch = bidExch;
    }


    /**
     * @return the askExch
     */
    public Double getAskExch() {
        return askExch;
    }


    /**
     * @param askExch the askExch to set
     */
    public void setAskExch(Double askExch) {
        this.askExch = askExch;
    }


    /**
     * @return the auctionVolume
     */
    public Double getAuctionVolume() {
        return auctionVolume;
    }


    /**
     * @param auctionVolume the auctionVolume to set
     */
    public void setAuctionVolume(Double auctionVolume) {
        this.auctionVolume = auctionVolume;
    }


    /**
     * @return the auctionPrice
     */
    public Double getAuctionPrice() {
        return auctionPrice;
    }


    /**
     * @param auctionPrice the auctionPrice to set
     */
    public void setAuctionPrice(Double auctionPrice) {
        this.auctionPrice = auctionPrice;
    }


    /**
     * @return the auctionImbalance
     */
    public Double getAuctionImbalance() {
        return auctionImbalance;
    }


    /**
     * @param auctionImbalance the auctionImbalance to set
     */
    public void setAuctionImbalance(Double auctionImbalance) {
        this.auctionImbalance = auctionImbalance;
    }


    /**
     * @return the markPrice
     */
    public Double getMarkPrice() {
        return markPrice;
    }


    /**
     * @param markPrice the markPrice to set
     */
    public void setMarkPrice(Double markPrice) {
        this.markPrice = markPrice;
    }


    /**
     * @return the bidEFP
     */
    public Double getBidEFP() {
        return bidEFP;
    }


    /**
     * @param bidEFP the bidEFP to set
     */
    public void setBidEFP(Double bidEFP) {
        this.bidEFP = bidEFP;
    }


    /**
     * @return the askEFP
     */
    public Double getAskEFP() {
        return askEFP;
    }


    /**
     * @param askEFP the askEFP to set
     */
    public void setAskEFP(Double askEFP) {
        this.askEFP = askEFP;
    }


    /**
     * @return the lastEFP
     */
    public Double getLastEFP() {
        return lastEFP;
    }


    /**
     * @param lastEFP the lastEFP to set
     */
    public void setLastEFP(Double lastEFP) {
        this.lastEFP = lastEFP;
    }


    /**
     * @return the openEFP
     */
    public Double getOpenEFP() {
        return openEFP;
    }


    /**
     * @param openEFP the openEFP to set
     */
    public void setOpenEFP(Double openEFP) {
        this.openEFP = openEFP;
    }


    /**
     * @return the highEFP
     */
    public Double getHighEFP() {
        return highEFP;
    }


    /**
     * @param highEFP the highEFP to set
     */
    public void setHighEFP(Double highEFP) {
        this.highEFP = highEFP;
    }


    /**
     * @return the lowEFP
     */
    public Double getLowEFP() {
        return lowEFP;
    }


    /**
     * @param lowEFP the lowEFP to set
     */
    public void setLowEFP(Double lowEFP) {
        this.lowEFP = lowEFP;
    }


    /**
     * @return the closeEFP
     */
    public Double getCloseEFP() {
        return closeEFP;
    }


    /**
     * @param closeEFP the closeEFP to set
     */
    public void setCloseEFP(Double closeEFP) {
        this.closeEFP = closeEFP;
    }


    /**
     * @return the lastTimestamp
     */
    public Double getLastTimestamp() {
        return lastTimestamp;
    }


    /**
     * @param lastTimestamp the lastTimestamp to set
     */
    public void setLastTimestamp(Double lastTimestamp) {
        this.lastTimestamp = lastTimestamp;
    }


    /**
     * @return the shortable
     */
    public Double getShortable() {
        return shortable;
    }


    /**
     * @param shortable the shortable to set
     */
    public void setShortable(Double shortable) {
        this.shortable = shortable;
    }


    /**
     * @return the fundamentals
     */
    public Double getFundamentals() {
        return fundamentals;
    }


    /**
     * @param fundamentals the fundamentals to set
     */
    public void setFundamentals(Double fundamentals) {
        this.fundamentals = fundamentals;
    }


    /**
     * @return the rTVolume
     */
    public Double getRTVolume() {
        return rtVolume;
    }


    /**
     * @param rTVolume the rTVolume to set
     */
    public void setRTVolume(Double rTVolume) {
        rtVolume = rTVolume;
    }


    /**
     * @return the halted
     */
    public Double getHalted() {
        return halted;
    }


    /**
     * @param halted the halted to set
     */
    public void setHalted(Double halted) {
        this.halted = halted;
    }


    /**
     * @return the bidYield
     */
    public Double getBidYield() {
        return bidYield;
    }


    /**
     * @param bidYield the bidYield to set
     */
    public void setBidYield(Double bidYield) {
        this.bidYield = bidYield;
    }


    /**
     * @return the askYield
     */
    public Double getAskYield() {
        return askYield;
    }


    /**
     * @param askYield the askYield to set
     */
    public void setAskYield(Double askYield) {
        this.askYield = askYield;
    }


    /**
     * @return the lastYield
     */
    public Double getLastYield() {
        return lastYield;
    }


    /**
     * @param lastYield the lastYield to set
     */
    public void setLastYield(Double lastYield) {
        this.lastYield = lastYield;
    }


    /**
     * @return the custOptComp
     */
    public Double getCustOptComp() {
        return custOptComp;
    }


    /**
     * @param custOptComp the custOptComp to set
     */
    public void setCustOptComp(Double custOptComp) {
        this.custOptComp = custOptComp;
    }


    /**
     * @return the trades
     */
    public Double getTrades() {
        return trades;
    }


    /**
     * @param trades the trades to set
     */
    public void setTrades(Double trades) {
        this.trades = trades;
    }


    /**
     * @return the trades_min
     */
    public Double getTrades_min() {
        return trades_min;
    }


    /**
     * @param trades_min the trades_min to set
     */
    public void setTrades_min(Double trades_min) {
        this.trades_min = trades_min;
    }


    /**
     * @return the volume_min
     */
    public Double getVolume_min() {
        return volume_min;
    }


    /**
     * @param volume_min the volume_min to set
     */
    public void setVolume_min(Double volume_min) {
        this.volume_min = volume_min;
    }


    /**
     * @return the lastRTHTrade
     */
    public Double getLastRTHTrade() {
        return lastRTHTrade;
    }


    /**
     * @param lastRTHTrade the lastRTHTrade to set
     */
    public void setLastRTHTrade(Double lastRTHTrade) {
        this.lastRTHTrade = lastRTHTrade;
    }
          
    
    
}