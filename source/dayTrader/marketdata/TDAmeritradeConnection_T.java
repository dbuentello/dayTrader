package marketdata;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import exceptions.ConnectionException;
import interfaces.Connector_IF;

public class TDAmeritradeConnection_T implements Connector_IF {

    private final String URL_BASE = "https://apis.tdameritrade.com/apps/100/";
    private final String SOURCE_ID = "NALA";
    private final String VERSION = "1.0";
    private final String USER_ID = "Ladd3113";
    private final String PASSWORD = "139WhitakerRd";
    private final String CHARSET = "UTF-8";
    
    private HttpsURLConnection urlConnection;
    private int sessionId = 0;
    
    @Override
    public void connect() throws ConnectionException {
        if (!isConnected()) {
            URL loginURL;
            try {
                loginURL = new URL(URL_BASE);
                urlConnection = (HttpsURLConnection) loginURL.openConnection();
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Accept-Charset", CHARSET);
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                
                
                OutputStream output = urlConnection.getOutputStream();
                try {
                    String urlQuery = "LogIn?source=" + SOURCE_ID + "&verision=" + VERSION; 
                    output.write(urlQuery.getBytes(CHARSET));
                } finally {
                     try { output.close(); } catch (IOException e) {}
                }
                InputStream response = urlConnection.getInputStream();
                
                
            } catch (Exception e) {
                throw new ConnectionException("Could not connect to TDAmeritrade");
            }
        } 

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
