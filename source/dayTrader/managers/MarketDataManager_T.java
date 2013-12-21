package managers;


import interfaces.Connector_IF;
import interfaces.Manager_IF;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import marketdata.MarketData_T;
import marketdata.RTData_T;
import marketdata.Symbol_T;
import marketdata.TDAmeritradeConnection_T;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import util.Exchange_T;
import util.Utilities_T;
import util.XMLTags_T;
import util.dtLogger_T;
import dayTrader.DayTrader_T;
import exceptions.ConnectionException;

public class MarketDataManager_T implements Manager_IF, Connector_IF, Runnable {


    private TDAmeritradeConnection_T dataSource = null;
    /** A reference to the DatabaseManager class. */
    private DatabaseManager_T databaseManager;
    /** A reference to the time manager. */
    private TimeManager_T timeManager;
    /** A reference to the LoggerManager class. */

    private dtLogger_T Log;

    public MarketDataManager_T() {

    }


    @Override
    public void initialize() {

        databaseManager = (DatabaseManager_T) DayTrader_T.getManager(DatabaseManager_T.class);
        timeManager     = (TimeManager_T) DayTrader_T.getManager(TimeManager_T.class);
        dataSource      = new TDAmeritradeConnection_T();

        Log = DayTrader_T.dtLog;

        try {
            connect();
        } catch (ConnectionException e) {
            Log.println("[FATAL ERROR] Could not connect to TDAmeritrade API");
            
            // TODO... throw this error
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        // TODO Auto-generated method stub

    }

    @Override
    public void sleep() {

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
    public int takeMarketSnapshot() {

    	int nQuotes = 0;
    	
        //get the symbols for an exchange from the database
        //TODO: update to query all exchanges
        String exchange = Exchange_T.NASDAQ;
        List<Symbol_T> symbols = databaseManager.getSymbolsByExchange(exchange);

        Log.print("\n*** Getting End of Day Quote Data");

        // delete any previous data for today
        deletePreviousData();

        
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

                //NAL
                //InputStream quoteIS = dataSource.getQuoteIS(symbolList);
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
            nQuotes += parseQuote(quoteDataList.get(i));

            System.out.print(".");		// progress
        }

        System.out.println("Done "+nQuotes+" added to EOD Table");
        
        return nQuotes;
    }


    /**
     * parse the quote data returned from TD
     * persist in EndOfDayQuote Table
     * 
     * @param quoteData
     */
    private int parseQuote(String quoteString) {

        ArrayList<MarketData_T> snapshot = new ArrayList<MarketData_T>();
        
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        try {

            InputStream quoteIS = new ByteArrayInputStream(quoteString.getBytes());

            dBuilder = dbFactory.newDocumentBuilder();
            Document quoteDoc = dBuilder.parse(quoteIS);

            quoteDoc.getDocumentElement().normalize();

            NodeList quotes = quoteDoc.getElementsByTagName(XMLTags_T.TDA_QUOTE);
            for( int i = 0; i < quotes.getLength(); i++) {

                
                Node quote = quotes.item(i);

                if (quote.getNodeType() == Node.ELEMENT_NODE) {
                    MarketData_T marketData = new MarketData_T();

                    Element quoteData = (Element) quote;
                    
                    Symbol_T symbol = new Symbol_T(quoteData.getElementsByTagName(XMLTags_T.TDA_SYMBOL).item(0).getTextContent());
                    marketData.setSymbolId(symbol.getId());
                    
                    try {
                        String dateString = quoteData.getElementsByTagName(XMLTags_T.TDA_LAST_TRADE_DATE).item(0).getTextContent();
                        DateFormat df = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
                        Date date = null;
                        try {
                            date = df.parse(dateString);
                        } catch (ParseException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }  
                        marketData.setLastTradeTimestamp(date);

                    } catch (Exception e) {
                        //Log.println("[INFO] NULL last-trade-date for " + symbol.getId() + " - " + symbol.getSymbol() + "! using NULL for date");
                        //Log.println("DEBUG]"+quoteString);
                    }

                    marketData.setOpen(Utilities_T.stringToDouble(quoteData.getElementsByTagName(XMLTags_T.TDA_OPEN).item(0).getTextContent()));
                    marketData.setClose(Utilities_T.stringToDouble(quoteData.getElementsByTagName(XMLTags_T.TDA_CLOSE).item(0).getTextContent()));
                    marketData.setOpen(Utilities_T.stringToDouble(quoteData.getElementsByTagName(XMLTags_T.TDA_OPEN).item(0).getTextContent()));
                    marketData.setHigh(Utilities_T.stringToDouble(quoteData.getElementsByTagName(XMLTags_T.TDA_HIGH).item(0).getTextContent()));
                    marketData.setLastPrice(Utilities_T.stringToDouble(quoteData.getElementsByTagName(XMLTags_T.TDA_LAST).item(0).getTextContent()));
                    marketData.setVolume(Utilities_T.stringToDouble(quoteData.getElementsByTagName(XMLTags_T.TDA_VOLUME).item(0).getTextContent()));
                    marketData.setChange(Utilities_T.stringToDouble(quoteData.getElementsByTagName(XMLTags_T.TDA_CHANGE).item(0).getTextContent()));
                    String percentChange = quoteData.getElementsByTagName(XMLTags_T.TDA_CHANGE_PERCENT).item(0).getTextContent();
                    // TD returns a string with a trailing % we dont want that
                    int percentIndex = percentChange.indexOf("%");
                    percentChange = percentChange.substring(0, percentIndex);
                    marketData.setPercentChange(Utilities_T.stringToDouble(percentChange));

                    marketData.setBidPrice(Utilities_T.stringToDouble(quoteData.getElementsByTagName(XMLTags_T.TDA_BID).item(0).getTextContent()));
                    marketData.setAskPrice(Utilities_T.stringToDouble(quoteData.getElementsByTagName(XMLTags_T.TDA_ASK).item(0).getTextContent()));

                    String bidAsk  = quoteData.getElementsByTagName(XMLTags_T.TDA_BID_ASK_PRICE).item(0).getTextContent();
                    // returned as bidXask, eg 200X400
                    int xIndex = bidAsk.indexOf("X");
                    String bidSize = bidAsk.substring(0, xIndex);
                    String askSize = bidAsk.substring(xIndex + 1);
                    marketData.setBidSize(Utilities_T.stringToDouble(bidSize));
                    marketData.setAskSize(Utilities_T.stringToDouble(askSize));

                    marketData.setWeekLow52(Utilities_T.stringToDouble(quoteData.getElementsByTagName(XMLTags_T.TDA_YEAR_LOW).item(0).getTextContent()));
                    marketData.setWeekHigh52(Utilities_T.stringToDouble(quoteData.getElementsByTagName(XMLTags_T.TDA_YEAR_HIGH).item(0).getTextContent()));

                    //update our snapshot map with the latest data
                    //SAL - not used
                    //--lastSnapshot.put(marketData.getSymbolId(), marketData);

                    //persist our individual quote to the database 
                    // but not for stocks with 0 price or open!!
                    //SALxx- do we really need to get the doublevalue?
                    if (marketData.getLastPrice().doubleValue() == 0.00) {
                        //--Log.println("[INFO] NULL price for " + marketData.getSymbol().getId() + "! Not updating EODQuote table");
                    } else if (marketData.getOpen().doubleValue() == 0.00) {
                        //--Log.println("[INFO] NULL open for " + marketData.getSymbol().getId() + "! Not updating EODQuote table");
                    } else {
                        snapshot.add(marketData);
                    }
                }

            }



        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return 0;
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return 0;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return 0;
        }

        databaseManager.bulkMarketDataInsert(snapshot);
        
        return snapshot.size();

    }

    /**
     * Delete any previous entries for today - we can only have one set of
     * EOD data
     */
    private void deletePreviousData() {
    	
    	Date date = timeManager.getCurrentTradeDate();
    	
        Session session = databaseManager.getSessionFactory().openSession();

        // deletes must be within a transaction
        Transaction tx = null;
        
        try {
            tx = session.beginTransaction();

            //SALxx TODO date between??
            String hql = "DELETE FROM marketdata.MarketData_T WHERE date >= :date";
            Query query = session.createQuery(hql);
            query.setDate("date", date);

            query.executeUpdate();
 
            tx.commit();
        } catch (HibernateException e) {
            //TODO: for now just print to stdout, we'll change this to a log file later
            e.printStackTrace();
            if (tx != null) tx.rollback();
        } finally {
            session.close();
        }  
    }

    /**
     * Get todays holdings symbols and query TD for latest data
     * Persist to RTQuotes DB - this may only be necessary if we want to save historical data
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
    private List<RTData_T> parseRTQuote(String quoteString) {

        List <RTData_T> retData = new ArrayList<RTData_T>();
        
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        try {

            InputStream quoteIS = new ByteArrayInputStream(quoteString.getBytes());

            dBuilder = dbFactory.newDocumentBuilder();
            Document quoteDoc = dBuilder.parse(quoteIS);

            quoteDoc.getDocumentElement().normalize();

            NodeList quotes = quoteDoc.getElementsByTagName(XMLTags_T.TDA_QUOTE);
            for( int i = 0; i < quotes.getLength(); i++) {

                
                Node quote = quotes.item(i);

                if (quote.getNodeType() == Node.ELEMENT_NODE) {
                    
                    Element quoteData = (Element) quote;

                    RTData_T rtData = new RTData_T();

                    String dateString = quoteData.getElementsByTagName(XMLTags_T.TDA_LAST_TRADE_DATE).item(0).getTextContent();
                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
                    Date date = null;
                    try {
                        date = df.parse(dateString);
                    } catch (ParseException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }  
                    rtData.setDate(date);

                    Symbol_T symbol = new Symbol_T(quoteData.getElementsByTagName(XMLTags_T.TDA_SYMBOL).item(0).getTextContent());
                    // TODO: this table still uses symbol, not symb id
                    //rtData.setSymbolId(symbol.getId());
                    rtData.setSymbol(symbol.getSymbol());

                    rtData.setPrice(Utilities_T.stringToDouble(quoteData.getElementsByTagName(XMLTags_T.TDA_LAST).item(0).getTextContent()));
                    rtData.setAskPrice(Utilities_T.stringToDouble(quoteData.getElementsByTagName(XMLTags_T.TDA_ASK).item(0).getTextContent()));
                    rtData.setBidPrice(Utilities_T.stringToDouble(quoteData.getElementsByTagName(XMLTags_T.TDA_BID).item(0).getTextContent()));
                    rtData.setVolume(Utilities_T.stringToDouble(quoteData.getElementsByTagName(XMLTags_T.TDA_VOLUME).item(0).getTextContent()).longValue());
        
                    //persist our individual quote to the database
                    // note this table use INSERT IGNORE so duplicates arent added
                    rtData.insertOrUpdate();
        
                    retData.add(rtData);
                } // next quote
            }
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return retData;
    }
    
    
    public void calcAvgVol() {
                
        ArrayList<Symbol_T> symbols = new ArrayList<Symbol_T>(databaseManager.getSymbolsByExchange(Exchange_T.NASDAQ));
        //TODO: Compute for all exchanges
        //symbols.addAll(databaseManager.getSymbolsByExchange(Exchange_T.AMEX));
        //symbols.addAll(databaseManager.getSymbolsByExchange(Exchange_T.NYSE));
        
        for(Symbol_T symbol : symbols) {
            
            ArrayList<MarketData_T> recentQuotes = (ArrayList<MarketData_T>) databaseManager.getRecentQuotes(symbol.getId(), 15);
            
            long vol = 0;
            if (recentQuotes != null) {
                for(MarketData_T quote : recentQuotes) {
                    vol += quote.getVolume();
                }
            }

            symbol.setAvgVolume15day(vol/15);
        }
        
        databaseManager.bulkSymbolUpdate(symbols);
    }

}
