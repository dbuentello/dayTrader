package marketdata;

import interfaces.Connector_IF;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import managers.DatabaseManager_T;
import managers.LoggerManager_T;

import org.apache.log4j.Level;

import dayTrader.DayTrader_T;

import util.XMLTags_T;

import exceptions.ConnectionException;



public class TDAmeritradeConnection_T implements Connector_IF {

    private final String BASE_URL = "https://apis.tdameritrade.com/apps/100/";
    private final String USER_ID = "Ladd3113";
    private final String PASSWORD = "139WhitakerRd";
    private final String SOURCE_ID = "NALA";
    private final String VERSION = "1.0";
    private final String ENCODING = "UTF-8";
    /** String used to store the current session id to be used in queries to TDA. */
    private String sessionId = null;
    /** A reference to the LoggerManager class. */
    private LoggerManager_T logger;
    
    public TDAmeritradeConnection_T() {
        logger = (LoggerManager_T) DayTrader_T.getManager(LoggerManager_T.class);
    }
    
    
    
    @SuppressWarnings("finally")
    @Override
    public void connect() throws ConnectionException {
        if (!isConnected()) {
            //CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
            URL loginURL;
            HttpsURLConnection urlConnection = null;
                
                try {
                    loginURL = new URL(BASE_URL + "LogIn?source=" + SOURCE_ID + "&verision=" + VERSION);
                
                    urlConnection = (HttpsURLConnection) loginURL.openConnection();
                    urlConnection.setDoOutput(true);
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty("Accept-Charset", ENCODING);
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    
                    
                    OutputStream output = urlConnection.getOutputStream();
                    try {
                        //TODO: Encode URL string
                        String postData = "userid=" + USER_ID + "&password=" + PASSWORD 
                                + "&source=" + SOURCE_ID + "&version=" + VERSION + "\n\n";
                        output.write(postData.getBytes(ENCODING));
                    } finally {
                         output.close();
                    }
                            
                    java.util.Scanner s = new java.util.Scanner(urlConnection.getInputStream()).useDelimiter("\\A");
                    String response = s.next();
                    sessionId = XMLTags_T.simpleParse(response, XMLTags_T.SESSION_ID);
                    
                    if (sessionId != "") {
                        logger.logText("TDAmeritrade Session ID = " + sessionId, Level.INFO);
                    } else {
                        logger.logText("Failed to get a TDAmeritrade session-id: " + response, Level.INFO);
                    }
         
                
                } catch (MalformedURLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    
                } 
                finally {
                    urlConnection.disconnect();
                }
            
        } 

    }
    
    
    public String getQuote(String symbolList) {
        
        String quoteXml = "";
        URL quoteUrl;
        HttpsURLConnection urlConnection = null;
        
        try {
            //TODO: encode url
            quoteUrl = new URL(BASE_URL + "Quote;jsessionid=" + sessionId + "?source=" + SOURCE_ID + "&symbol=" + symbolList);
            //logger.logText(quoteUrl.toString(), Level.DEBUG);
    
            urlConnection = (HttpsURLConnection) quoteUrl.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("GET"); //redundant as GET is the default method, but makes it easier to read
            urlConnection.setRequestProperty("Accept-Charset", ENCODING);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    
            java.util.Scanner s = new java.util.Scanner(urlConnection.getInputStream()).useDelimiter("\\A");
            quoteXml = s.next();
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            urlConnection.disconnect();
        }
        
        
        return quoteXml;
    }
    

    @Override
    public void disconnect() {

        URL logoutUrl;
        HttpsURLConnection urlConnection = null;
        
        try {
            //TODO: encode url
            logoutUrl = new URL(BASE_URL + "LogOut;jsessionid=" + sessionId + "?source=" + SOURCE_ID);
            logger.logText(logoutUrl.toString(), Level.DEBUG);
    
            urlConnection = (HttpsURLConnection) logoutUrl.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("GET"); //redundant as GET is the default method, but makes it easier to read
            urlConnection.setRequestProperty("Accept-Charset", ENCODING);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    
            urlConnection.connect();
            logger.logText("Logged out from TDAmeritrade", Level.INFO);
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            urlConnection.disconnect();
        }
        
    }

    @Override
    public boolean isConnected() {
        return false;
    }

}
