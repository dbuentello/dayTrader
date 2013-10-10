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

import managers.LoggerManager_T;

import org.apache.log4j.Level;

import util.XMLTags_T;

import exceptions.ConnectionException;



public class TDAmeritradeConnection_T implements Connector_IF {

    private final String BASE_URL = "https://apis.tdameritrade.com/apps/100/";
    private final String USER_ID = "Ladd3113";
    private final String PASSWORD = "139WhitakerRd";
    private final String SOURCE_ID = "NALA";
    private final String VERSION = "1.0";
    private final String ENCODING = "UTF-8";
    
    //private HttpsURLConnection urlConnection;
    private String sessionId = null;
    
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
                        LoggerManager_T.logText("TDAmeritrade Session ID = " + sessionId, Level.INFO);
                    } else {
                        LoggerManager_T.logText("Failed to get a TDAmeritrade session-id: " + response, Level.INFO);
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
            LoggerManager_T.logText(quoteUrl.toString(), Level.DEBUG);
    
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
            System.out.println("wait");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            //urlConnection.disconnect();
        }
        
        
        return quoteXml;
    }
    

    @Override
    public void disconnect() {

    }

    @Override
    public boolean isConnected() {
        return false;
    }

}
