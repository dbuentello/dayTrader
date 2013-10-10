package managers;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Level;

import util.Exchange_T;
import util.Utilities_T;
import util.XMLTags_T;

import marketdata.MarketData_T;
import marketdata.Symbol_T;
import marketdata.TDAmeritradeConnection_T;

import exceptions.ConnectionException;
import interfaces.Connector_IF;
import interfaces.Manager_IF;

public class MarketDataManager_T implements Manager_IF, Connector_IF {

    
    final TDAmeritradeConnection_T dataSource = new TDAmeritradeConnection_T();
    
    
    @Override
    public void initialize() {
        
        try {
            connect();
        } catch (ConnectionException e) {
            // TODO Auto-generated catch block
            LoggerManager_T.logFault("Could not connect to TDAmeritrade API", e);      
        }

    }

    @Override
    public void run() {
        getEndOfDayQuotes();

    }

    @Override
    public void sleep() {
        // TODO Auto-generated method stub

    }

    @Override
    public void terminate() {
        // TODO Auto-generated method stub

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
    
    public void getEndOfDayQuotes() {
        
        //TODO: Use a real getDate function, right now it's just using the current system time
        //Date date = new Date(System.currentTimeMillis());
        
        //get the symbols for an exchange from the database
        //TODO: update to query all exchanges
        String exchange = Exchange_T.AMEX;
        List<Symbol_T> symbols = DatabaseManager_T.getSymbolsByExchange(exchange);
        
        LoggerManager_T.logText("Found " + symbols.size() + " for exchange " + exchange, Level.DEBUG);
        
        List<String> quoteDataList = new ArrayList<String>();
        Iterator<Symbol_T> it = symbols.iterator();
        String symbolList = "";
        int count = 1;
        while (it.hasNext()) {
            
            Symbol_T symbol = it.next();
            
            symbolList += symbol.getSymbol() + ",";
            
            //Get 100 quotes at a time. If we're on the last item in the symbol list, get the remaining symbols 
            if ((count % 100) == 0 || it.hasNext() == false) {
                String quoteResponse = dataSource.getQuote(symbolList);
                LoggerManager_T.logText(quoteResponse, Level.DEBUG);
                quoteDataList.add(quoteResponse);
                symbolList = "";
                
            }
            
            count++;
        }
        
        //loop through our retrieved quotes and parse out the quote information
        for(int i = 0; i < quoteDataList.size(); i++) {
            String quoteData = quoteDataList.get(i);
            int qlStart = quoteData.indexOf(XMLTags_T.QUOTE_LIST);
            
            if (qlStart == -1) {
                LoggerManager_T.logText("Failed to find start of quote list", Level.DEBUG);
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
                    marketData.setLastTimestamp(date);
                  
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
                    
                    marketData.insert();
                    
                    // get ready to parse next quote
                    int qend = quoteData.indexOf("</quote>");
                    if (qend == -1)
                    {
                      LoggerManager_T.logText("Cant find end of quote", Level.WARN);
                      break;
                    }
                    // strip off beginning
                    quoteData = quoteData.substring(qend);
                    qStart = quoteData.indexOf("<quote>");


                  
                }

            }
        }
        
    }
    

}
