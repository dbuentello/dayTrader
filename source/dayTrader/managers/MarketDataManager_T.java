package managers;


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
