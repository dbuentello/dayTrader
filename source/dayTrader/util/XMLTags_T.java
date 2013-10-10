/**
 * 
 */
package util;

import managers.LoggerManager_T;


import org.apache.log4j.Level;

/**
 * @author nathan
 *
 */
public class XMLTags_T {

    //This class will probably not be needed once we use a real XML parser
    public final static String SESSION_ID = "session-id";
    public final static String QUOTE_LIST = "quote-list";
    public final static String QUOTE = "quote";
    public final static String LAST_TRADE_DATE = "last-trade-date";
    public final static String SYMBOL = "symbol";
    public final static String OPEN = "open";
    public final static String CLOSE = "close";
    public final static String LOW = "low";
    public final static String HIGH = "high";
    public final static String LAST = "last";
    public final static String VOLUME = "volume";
    public final static String CHANGE = "change";
    public final static String CHANGE_PERCENT = "change-percent";
    public final static String BID = "bid";
    public final static String ASK = "ask";
    public final static String BID_ASK_PRICE = "bid-ask-size";
    public final static String YEAR_LOW = "year-low";
    public final static String YEAR_HIGH = "year-high";
    
    /**
     * 
     * @param xml
     * @param tag
     * @return sessionId
     */
    //TODO: Use a real parser, although this is very effective
    public static String simpleParse(String xml, String tag) {

      String val = "";
            
      String taga = "<" + tag + ">";
      String tagb = "</" + tag + ">";
      int tagsize = tag.length() + 2;

      int start = xml.indexOf(taga);
      int end   = xml.indexOf(tagb);
      if ( start==-1 || end==-1 ) {
         LoggerManager_T.logText("\nParse error for tag " + tag, Level.ERROR);
      } 
      else {
          try {
              val = xml.substring(start+tagsize, end);
          } catch (Exception e) {
              LoggerManager_T.logText("Excpetion thrown in simpleParse: tag = " + tag + " - XML = " + xml, Level.WARN);
              LoggerManager_T.logFault("Error parsing XML", e);
          }
      }

      return val;

    }
    
}
