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

import trader.Trader_T;
import util.Exchange_T;
import util.Utilities_T;
import util.XMLTags_T;

import marketdata.MarketData_T;
import marketdata.Symbol_T;
import marketdata.TDAmeritradeConnection_T;

import exceptions.ConnectionException;
import interfaces.Connector_IF;
import interfaces.Manager_IF;

public class MarketDataManager_T implements Manager_IF, Connector_IF, Runnable {

    
    private TDAmeritradeConnection_T dataSource = null;
    /** A reference to the DatabaseManager class. */
    private DatabaseManager_T databaseManager;
    /** A reference to the time manager. */				//SALxx for TimeNow (may be overkill)
    private TimeManager_T timeManager;
    /** A reference to the LoggerManager class. */
    private LoggerManager_T logger;
    /** Map to hold the most recent snapshot taken of the market. **/
    private HashMap<Long, MarketData_T> lastSnapshot = new HashMap<Long, MarketData_T>();
    
    
    public MarketDataManager_T() {
    
    }
    
    
    @Override
    public void initialize() {
        
        logger          = (LoggerManager_T) DayTrader_T.getManager(LoggerManager_T.class);
        databaseManager = (DatabaseManager_T) DayTrader_T.getManager(DatabaseManager_T.class);
        timeManager     = (TimeManager_T) DayTrader_T.getManager(TimeManager_T.class);
        dataSource = new TDAmeritradeConnection_T();
        
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
        
        System.out.print("Getting Quote Data");
        
        //get the symbols for an exchange from the database
        //TODO: update to query all exchanges
        String exchange = Exchange_T.NASDAQ;
        List<Symbol_T> symbols = databaseManager.getSymbolsByExchange(exchange);
        
        logger.logText("Found " + symbols.size() + " for exchange " + exchange, Level.DEBUG);
        
        List<String> quoteDataList = new ArrayList<String>();
        Iterator<Symbol_T> it = symbols.iterator();
        String symbolList = "";
        int count = 1;
        while (it.hasNext()) {
            
            Symbol_T symbol = it.next();
            
            symbolList += symbol.getSymbol() + ",";
            
            //Get 100 quotes at a time. If we're on the last item in the symbol list, get the remaining symbols 
            if ((count % 100) == 0 || it.hasNext() == false) {
            	System.out.print(".");		// progress
            	
            	String quoteResponse = dataSource.getQuote(symbolList);
                //logger.logText(quoteResponse, Level.DEBUG);
            	
                quoteDataList.add(quoteResponse);
                symbolList = "";               
            }
            
            count++;
        }
        
        System.out.println("Done");
        System.out.print("Parsing");
        
        //loop through our retrieved quotes and parse out the quote information
        for(int i = 0; i < quoteDataList.size(); i++) {
            parseQuote(quoteDataList.get(i));
            if (i%100==0) { System.out.print("."); }
        }
        
        System.out.println("Done");
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        
    }

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
     * Get todays End of Day Price for this symbol from EODQuote database
     * 
     * @return (double) price
     */ 
    public double getEODPrice(Symbol_T symbol)
    {
    	//"SELECT price from EndOfDayQuotes where symbol = \"$symbol\" AND DATE(date) = \"$date\"";
        Session session = databaseManager.getSessionFactory().openSession();
        
        Criteria criteria = session.createCriteria(MarketData_T.class)
            .add(Restrictions.ge("lastTradeTimestamp", timeManager.Today() ))
            .add(Restrictions.eq("symbolId", symbol.getId()));

        @SuppressWarnings("unchecked")
        List<MarketData_T> quoteData = criteria.list();
        
        session.close();
        
        if (quoteData.size() != 1) { logger.logText("Bad EOD price for "+symbol.getSymbol(), Level.ERROR); return 0.0;}
        
        double price = quoteData.get(0).getLastPrice();
        return price;
            	
    }
    
}
