/**
 * 
 */
package util;

import managers.LoggerManager_T;


import org.apache.log4j.Level;

import dayTrader.DayTrader_T;

/**
 * @author nathan
 *
 */
public class XMLTags_T {

    //Tags for parsing TDA quotes
    public final static String TDA_SESSION_ID = "session-id";
    public final static String TDA_QUOTE_LIST = "quote-list";
    public final static String TDA_QUOTE = "quote";
    public final static String TDA_LAST_TRADE_DATE = "last-trade-date";
    public final static String TDA_SYMBOL = "symbol";
    public final static String TDA_OPEN = "open";
    public final static String TDA_CLOSE = "close";
    public final static String TDA_LOW = "low";
    public final static String TDA_HIGH = "high";
    public final static String TDA_LAST = "last";
    public final static String TDA_VOLUME = "volume";
    public final static String TDA_CHANGE = "change";
    public final static String TDA_CHANGE_PERCENT = "change-percent";
    public final static String TDA_BID = "bid";
    public final static String TDA_ASK = "ask";
    public final static String TDA_BID_ASK_PRICE = "bid-ask-size";
    public final static String TDA_YEAR_LOW = "year-low";
    public final static String TDA_YEAR_HIGH = "year-high";
    
    //Tags for parsing the DayTrader configuration file
    public final static String CFG_DAY_TRADER_CONFIG = "dayTraderConfig";
    public final static String CFG_VERSION = "version";
    public final static String CFG_DT_LOG_FILE_NAME = "dtLogFileName";
    public final static String CFG_DT_REPORT_DIR_PATH = "dtReportDirPath";
    public final static String CFG_MINUTES_BEFORE_CLOSE_TO_BUY = "minutesBeforeCloseToBuy";
    public final static String CFG_MINUTES_TO_WAIT_FOR_EXECUTION = "minutesToWaitForExecution";
    public final static String CFG_RT_SCAN_INTERVAL_MINUTES = "rtScanIntervalMinutes";
    public final static String CFG_USE_IB = "useIB";
    public final static String CFG_USE_TD = "useTD";
    public final static String CFG_IGNORE_MARKET_CLOSED = "ignoreMarketClosed";
    public final static String CFG_USE_SIMULATE_DATE = "useSimulateDate";
    public final static String CFG_TAKE_SNAPSHOT = "takeSnapshot";
    public final static String CFG_GET_RT_DATA = "getRTData";
    public final static String CFG_USE_SYSTEM_TIME = "useSystemTime";
    public final static String CFG_ACCOUNT_CODE = "accountCode";
    public final static String CFG_DATABASE_NAME = "databaseName";
    public final static String CFG_DATABASE_USER = "databaseUser";
    public final static String CFG_DATABASE_PASSWORD = "databasePassword";
    public final static String CFG_MIN_TRADE_VOLUME = "minTradeVolume";
    public final static String CFG_MIN_BUY_PRICE = "minBuyPrice";
    public final static String CFG_MAX_BUY_POSITIONS = "maxBuyPositions";
    public final static String CFG_MAX_BUY_AMOUNT = "maxBuyAmount";
    public final static String CFG_MIN_ACCOUNT_BALANCE = "minAccountBalance";
    
    /**
     * 
     * @param xml
     * @param tag
     * @return sessionId
     */
    //we use a real XML parser, although this is very effective
    public static String simpleParse(String xml, String tag) {

      LoggerManager_T logger = (LoggerManager_T) DayTrader_T.getManager(LoggerManager_T.class);
        
      String val = "";
            
      String taga = "<" + tag + ">";
      String tagb = "</" + tag + ">";
      int tagsize = tag.length() + 2;

      int start = xml.indexOf(taga);
      int end   = xml.indexOf(tagb);
      if ( start==-1 || end==-1 ) {
          logger.logText("\nParse error for tag " + tag, Level.ERROR);
      } 
      else {
          try {
              val = xml.substring(start+tagsize, end);
          } catch (Exception e) {
              logger.logText("Excpetion thrown in simpleParse: tag = " + tag + " - XML = " + xml, Level.WARN);
              logger.logFault("Error parsing XML", e);
          }
      }

      return val;

    }
    
}
