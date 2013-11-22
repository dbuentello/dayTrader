package managers;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Level;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import dayTrader.DayTrader_T;
import util.dtLogger_T;


import trader.Trader_T;
import util.Exchange_T;
import util.Utilities_T;
import util.XMLTags_T;

import marketdata.MarketData_T;
import marketdata.Symbol_T;
import marketdata.RTData_T;
import marketdata.TDAmeritradeConnection_T;

import trader.TraderCalculator_T;

import exceptions.ConnectionException;
import interfaces.Connector_IF;
import interfaces.Manager_IF;

public class MarketDataManager_T implements Manager_IF, Connector_IF, Runnable {

    
    private TDAmeritradeConnection_T dataSource = null;
    /** A reference to the DatabaseManager class. */
    private DatabaseManager_T databaseManager;
    /** A reference to the time manager. */
    private TimeManager_T timeManager;
    /** A reference to the LoggerManager class. */
    private LoggerManager_T logger;
    /** Map to hold the most recent snapshot taken of the market. **/
    private HashMap<Long, MarketData_T> lastSnapshot = new HashMap<Long, MarketData_T>();
    
    private dtLogger_T Log;
    
    public MarketDataManager_T() {
    
    }
    
    
    @Override
    public void initialize() {
        
        logger          = (LoggerManager_T) DayTrader_T.getManager(LoggerManager_T.class);
        databaseManager = (DatabaseManager_T) DayTrader_T.getManager(DatabaseManager_T.class);
        timeManager     = (TimeManager_T) DayTrader_T.getManager(TimeManager_T.class);
        dataSource      = new TDAmeritradeConnection_T();
        
        Log = DayTrader_T.dtLog;
        
        try {
            connect();
        } catch (ConnectionException e) {
            // TODO Auto-generated catch block
            logger.logFault("Could not connect to TDAmeritrade API", e);      
        }

    }

    @Override
    public void sleep() {
        // TODO Auto-generated method stub

    }

    @Override
    public void terminate() {
        disconnect();
    }

    @Override
    public void wakeup() {
        // TODO Auto-generated method stub

    }

    @Override
    public void connect() throws ConnectionException {
        dataSource.connect();
    }

    @Override
    public void disconnect() {
        dataSource.disconnect();
    }

    @Override
    public boolean isConnected() {
        return dataSource.isConnected();
    }
    
    /**
     * Get a snapshot of all the symbols being traded on all markets and save the quote
     * information to the database
     * 
     */
    public void takeMarketSnapshot() {
        
        //get the symbols for an exchange from the database
        //TODO: update to query all exchanges
        String exchange = Exchange_T.NASDAQ;
        List<Symbol_T> symbols = databaseManager.getSymbolsByExchange(exchange);
        
        //logger.logText("Found " + symbols.size() + " for exchange " + exchange, Level.DEBUG);
        
        Log.print("\n*** Getting End of Day Quote Data");
        
        List<String> quoteDataList = new ArrayList<String>();
        Iterator<Symbol_T> it = symbols.iterator();
        String symbolList = "";
        int count = 1;
        while (it.hasNext()) {
            
            Symbol_T symbol = it.next();
            
            symbolList += symbol.getSymbol() + ",";
            
            //Get 100 quotes at a time. If we're on the last item in the symbol list, get the remaining symbols 
            if ((count % 100) == 0 || it.hasNext() == false) {
            	Log.print(".");		// progress
            	
            	String quoteResponse = dataSource.getQuote(symbolList);
                //logger.logText(quoteResponse, Level.DEBUG);
            	
                quoteDataList.add(quoteResponse);
                symbolList = "";               
            }
            
            count++;
        }
        
        Log.println("Done ***");
        
        System.out.print("Parsing");
        
        //loop through our retrieved quotes and parse out the quote information
        for(int i = 0; i < quoteDataList.size(); i++) {
            parseQuote(quoteDataList.get(i));
            
            System.out.print(".");		// progress
        }
        
        System.out.println("Done");
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        
    }

    /**
     * parse the quote data returned from TD
     * persist in EndOfDayQuote Table
     * 
     * @param quoteData
     */
    private void parseQuote(String quoteData) {
        
        int qlStart = quoteData.indexOf(XMLTags_T.QUOTE_LIST);
        
        if (qlStart == -1) {
            logger.logText("Failed to find start of quote list", Level.DEBUG);
        } else {
            
            int qStart = quoteData.indexOf("<quote>");
            while (qStart != -1) {
    
                // strip off beginning
                quoteData = quoteData.substring(qStart);
        
                MarketData_T marketData = new MarketData_T();
          
                String dateString = XMLTags_T.simpleParse(quoteData, XMLTags_T.LAST_TRADE_DATE);
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
                Date date = null;
                try {
                    date = df.parse(dateString);
                } catch (ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }  
                marketData.setLastTradeTimestamp(date);
              
                Symbol_T symbol = new Symbol_T(XMLTags_T.simpleParse(quoteData, XMLTags_T.SYMBOL));
                marketData.setSymbolId(symbol.getId());
              
                marketData.setOpen(Utilities_T.stringToDouble(XMLTags_T.simpleParse(quoteData, XMLTags_T.OPEN)));
                marketData.setClose(Utilities_T.stringToDouble(XMLTags_T.simpleParse(quoteData, XMLTags_T.CLOSE)));
                marketData.setOpen(Utilities_T.stringToDouble(XMLTags_T.simpleParse(quoteData, XMLTags_T.OPEN)));
                marketData.setHigh(Utilities_T.stringToDouble(XMLTags_T.simpleParse(quoteData, XMLTags_T.HIGH)));
                marketData.setLastPrice(Utilities_T.stringToDouble(XMLTags_T.simpleParse(quoteData, XMLTags_T.LAST)));
                marketData.setVolume(Utilities_T.stringToDouble(XMLTags_T.simpleParse(quoteData, XMLTags_T.VOLUME)));
                marketData.setChange(Utilities_T.stringToDouble(XMLTags_T.simpleParse(quoteData, XMLTags_T.CHANGE)));
                String percentChange = XMLTags_T.simpleParse(quoteData, XMLTags_T.CHANGE_PERCENT);
                // TD returns a string with a trailing % we dont want that
                int percentIndex = percentChange.indexOf("%");
                percentChange = percentChange.substring(0, percentIndex);
                marketData.setPercentChange(Utilities_T.stringToDouble(percentChange));

                marketData.setBidPrice(Utilities_T.stringToDouble(XMLTags_T.simpleParse(quoteData, XMLTags_T.BID)));
                marketData.setAskPrice(Utilities_T.stringToDouble(XMLTags_T.simpleParse(quoteData, XMLTags_T.ASK)));
              
                String bidAsk  = XMLTags_T.simpleParse(quoteData, XMLTags_T.BID_ASK_PRICE);
                // retured as bidXask, eg 200X400
                int xIndex = bidAsk.indexOf("X");
                String bidSize = bidAsk.substring(0, xIndex);
                String askSize = bidAsk.substring(xIndex + 1);
                marketData.setBidSize(Utilities_T.stringToDouble(bidSize));
                marketData.setAskSize(Utilities_T.stringToDouble(askSize));
              
                marketData.setWeekLow52(Utilities_T.stringToDouble(XMLTags_T.simpleParse(quoteData, XMLTags_T.YEAR_LOW)));
                marketData.setWeekHigh52(Utilities_T.stringToDouble(XMLTags_T.simpleParse(quoteData, XMLTags_T.YEAR_HIGH)));
                
                
                //update our snapshot map with the latest data
                //SAL - why??
                lastSnapshot.put(marketData.getSymbolId(), marketData);
                
                //persist our individual quote to the database 
            	// but not for stocks with 0 price or open!!
                //SALxx- do we really need to get the doublevalue?
            	if (marketData.getLastPrice().doubleValue() == 0.00)
            		Log.println("WARNING: NULL price for " + marketData.getSymbol().getId() + "! Not updating EODQuote table");
            	else if (marketData.getOpen().doubleValue() == 0.00)
            		Log.println("WARNING: NULL open for " + marketData.getSymbol().getId() + "! Not updating EODQuote table");
            	else
            		marketData.insertOrUpdate();
                
                // get ready to parse next quote
                int qend = quoteData.indexOf("</quote>");
                if (qend == -1)
                {
                    logger.logText("Cant find end of quote", Level.WARN);
                  break;
                }
                // strip off beginning
                quoteData = quoteData.substring(qend);
                qStart = quoteData.indexOf("<quote>");

              
            }

        }
    }
    


    /**
     * Get todays holdings symbols and query TD for latest data
     * Persist to RTQuotes DB
     * 
     * @return a list of RTData for each holding (empty on return)
     */
    public List<RTData_T> getRealTimeQuotes()
    {
    	Log.println("*** Getting RealTime Data at "+timeManager.TimeNow()+" ***");
    	
    	List <Symbol_T> symbols = databaseManager.getHoldingsSymbols();
    	if (symbols.isEmpty()) Log.println("ERROR: getHoldings returns empty");
    	
    	// this code is very similar to snapshot, except we dont need multiple iterations
        Iterator<Symbol_T> it = symbols.iterator();
        String symbolList = "";
        while (it.hasNext()) {
            
            Symbol_T symbol = it.next();
            
            symbolList += symbol.getSymbol() + ",";
        }
                    	
        String quoteResponse = dataSource.getQuote(symbolList);
//logger.logText(quoteResponse, Level.DEBUG);

		if (quoteResponse.isEmpty()) Log.println("ERROR: getQuote returns null");
        
        //System.out.print("Parsing...");
        
        // parse the RT quote information and persist to RTQuotes DB
		
		// suppress duplicate insert errors (maybe quicker than checking for existence)
		// --- doesnt work --- wrong logger?
		//Level prevLevel = org.apache.log4j.Logger.getLogger("net.sf.hibernate").getLevel();
		//org.apache.log4j.Logger.getLogger("net.sf.hibernate").setLevel(org.apache.log4j.Level.ERROR);
		
        List<RTData_T> rtData = parseRTQuote(quoteResponse);
 
		//org.apache.log4j.Logger.getLogger("net.sf.hibernate").setLevel(prevLevel);

        //System.out.println("Done");
        
        return rtData;
    }

    /**
     * parse the quote data returned from TD
     * persist in RealTimeQuote Table
     * just a subset of all data - symbol, last trade date, last price
     * ask price, bid price and volume
     * 
     * @param quoteData returned from TD
     * @return a list of RTData for each holding.  on error, the list will be empty
     */
    private List<RTData_T> parseRTQuote(String quoteData) {
        
    	List <RTData_T> retData = new ArrayList<RTData_T>();
    	
        int qlStart = quoteData.indexOf(XMLTags_T.QUOTE_LIST);
        
        if (qlStart == -1) {
            logger.logText("Failed to find start of quote list", Level.DEBUG);
            // throw
            return retData;
        }
        
            
        int qStart = quoteData.indexOf("<quote>");
        while (qStart != -1) {
    
        	// strip off beginning
        	quoteData = quoteData.substring(qStart);
        
        	RTData_T rtData = new RTData_T();
          
        	String dateString = XMLTags_T.simpleParse(quoteData, XMLTags_T.LAST_TRADE_DATE);
        	DateFormat df = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
        	Date date = null;
        	try {
        		date = df.parse(dateString);
        	} catch (ParseException e) {
        		// TODO Auto-generated catch block
        		e.printStackTrace();
        	}  
        	rtData.setDate(date);
              
        	Symbol_T symbol = new Symbol_T(XMLTags_T.simpleParse(quoteData, XMLTags_T.SYMBOL));
        	// TODO: this table still uses symbol, not symb id
        	//rtData.setSymbolId(symbol.getId());
        	rtData.setSymbol(symbol.getSymbol());
              
        	rtData.setPrice(Utilities_T.stringToDouble(XMLTags_T.simpleParse(quoteData, XMLTags_T.LAST)));
        	rtData.setAskPrice(Utilities_T.stringToDouble(XMLTags_T.simpleParse(quoteData, XMLTags_T.ASK)));
        	rtData.setBidPrice(Utilities_T.stringToDouble(XMLTags_T.simpleParse(quoteData, XMLTags_T.BID)));
        	rtData.setVolume(Utilities_T.stringToDouble(XMLTags_T.simpleParse(quoteData, XMLTags_T.VOLUME)).longValue());
              
        	//persist our individual quote to the database
        	// note this table use INSERT IGNORE so duplicates arent added
        	rtData.insertOrUpdate();
            
        	retData.add(rtData);
        	
        	// get ready to parse next quote
        	int qend = quoteData.indexOf("</quote>");
        	if (qend == -1)
        	{
        		logger.logText("Cant find end of quote", Level.WARN);
        		break;
        	}

        	// strip off beginning
        	quoteData = quoteData.substring(qend);
        	qStart = quoteData.indexOf("<quote>");
        } // next quote
        
        return retData;
    }

}
