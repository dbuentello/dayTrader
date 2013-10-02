package marketdata;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import managers.LoggerManager_T;

import exceptions.ConnectionException;
import interfaces.Connector_IF;

import org.apache.log4j.Level;



public class TDAmeritradeConnection_T implements Connector_IF {

    private final String URL_BASE = "https://apis.tdameritrade.com/apps/100/";
    private final String USER_ID = "Ladd3113";
    private final String PASSWORD = "139WhitakerRd";
    private final String SOURCE_ID = "NALA";
    private final String VERSION = "1.0";
    private final String ENCODING = "UTF-8";
    
    private HttpsURLConnection urlConnection;
    private String sessionId = null;
    
    @SuppressWarnings("finally")
    @Override
    public void connect() throws ConnectionException {
        if (!isConnected()) {
            URL loginURL;
            
                
                try {
                    loginURL = new URL(URL_BASE + "LogIn?source=" + SOURCE_ID + "&verision=" + VERSION);
                
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
                    sessionId = simpleParse(response, "session-id");
                    
                    LoggerManager_T.logText("TDAmeritrade Session ID = " + sessionId, Level.INFO);
                
                } catch (MalformedURLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    
                }
            
        } 

    }
    
    /**
     * 
     * @param xml
     * @param tag
     * @return sessionId
     */
    //TODO: Use a real parser, although this is very effective
    private String simpleParse(String xml, String tag) {

      String val = "";
            
      String taga = "<" + tag + ">";
      String tagb = "</" + tag + ">";
      int tagsize = tag.length() +2;

      int start = xml.indexOf(taga);
      int end   = xml.indexOf(tagb);
      if ( start==-1 || end==-1 ) {
         LoggerManager_T.logText("\nParse error for tag $tag", Level.ERROR);
      } 
      else {
          val = xml.substring(start+tagsize, end-tagsize);
      }

      return val;

    }


    @Override
    public void disconnect() {
        urlConnection.disconnect();
        urlConnection = null;

    }

    @Override
    public boolean isConnected() {
        return (urlConnection == null ? false : true);
    }

}
