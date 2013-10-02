package managers;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

import marketdata.TDAmeritradeConnection_T;

import exceptions.ConnectionException;
import interfaces.Connector_IF;
import interfaces.Manager_IF;

public class MarketDataManager_T implements Manager_IF, Connector_IF {

    
    TDAmeritradeConnection_T dataSource;
    
    
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
        // TODO Auto-generated method stub

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

}
