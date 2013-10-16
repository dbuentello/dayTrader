package managers;

import interfaces.Connector_IF;
import interfaces.Manager_IF;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Level;

import trader.Holding_T;

import com.ib.client.CommissionReport;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.EClientSocket;
import com.ib.client.EWrapper;
import com.ib.client.Execution;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.UnderComp;

import dayTrader.DayTrader_T;

import exceptions.ConnectionException;

public class BrokerManager_T implements Manager_IF, Connector_IF, EWrapper {
  /* {src_lang=Java}*/

    private final String GATEWAY_HOST = "localhost";
    private final int GATEWAY_PORT = 4001;
    private final int CLIENT_ID = 1;
    // ACTUAL TRADER ACCOUNT CODE
    //private final String  ACCOUNT_CODE = "U1235379";
    // PAPER TRADER ACCOUNT CODE
    private final String  ACCOUNT_CODE = "DU171047";
    /** The current time in milliseconds as returned from the broker's server */
    private Date currentTime = new Date();
    /** A reference to the DatabaseManager class. */
    private DatabaseManager_T databaseManager;
    /** A reference to the LoggerManager class. */
    private LoggerManager_T logger;
    
    /** HashMap of orderIds to Holdings so we can easily retrieve holding information 
     *  based on orderId.
     */
    private Map<Integer, Holding_T> holdings = new HashMap<Integer, Holding_T>();
    
    EClientSocket ibClientSocket = null;
    
    /**
     * 
     */
    public BrokerManager_T() {
        // TODO Auto-generated constructor stub
    }

    public void executeTrade() {
    }

	@Override
	public void error(Exception e) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void error(String str) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void error(int id, int errorCode, String errorMsg) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void connectionClosed() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void tickPrice(int tickerId, int field, double price, int canAutoExecute) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void tickSize(int tickerId, int field, int size) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void tickOptionComputation(int tickerId, int field, double impliedVol,
			double delta, double optPrice, double pvDividend, double gamma,
			double vega, double theta, double undPrice) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void tickGeneric(int tickerId, int tickType, double value) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void tickString(int tickerId, int tickType, String value) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void tickEFP(int tickerId, int tickType, double basisPoints,
			String formattedBasisPoints, double impliedFuture, int holdDays,
			String futureExpiry, double dividendImpact, double dividendsToExpiry) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void orderStatus(int orderId, String status, int filled, int remaining,
			double avgFillPrice, int permId, int parentId, double lastFillPrice,
			int clientId, String whyHeld) {
		
	    Holding_T holding = holdings.get(orderId);
		
	    holding.setOrderStatus(status);
	    holding.setFilled(filled);
	    holding.setRemaining(remaining);
	    holding.setAvgFillPrice(avgFillPrice);
	    //the permId is stored in holding.order.m_permId
	    holding.setParentId(parentId);
	    holding.setLastFillPrice(lastFillPrice);
	    //the clientId is stored in holding.order.m_cliendId
	    
	    
	}
	
	@Override
	public void openOrder(int orderId, Contract contract, Order order,
			OrderState orderState) {
		
	    holdings.put(orderId, new Holding_T(order, contract, orderState));
		
	}
	
	@Override
	public void openOrderEnd() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void updateAccountValue(String key, String value, String currency,
			String accountName) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void updatePortfolio(Contract contract, int position,
			double marketPrice, double marketValue, double averageCost,
			double unrealizedPNL, double realizedPNL, String accountName) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void updateAccountTime(String timeStamp) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void accountDownloadEnd(String accountName) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void nextValidId(int orderId) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void contractDetails(int reqId, ContractDetails contractDetails) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void bondContractDetails(int reqId, ContractDetails contractDetails) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void contractDetailsEnd(int reqId) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void execDetails(int reqId, Contract contract, Execution execution) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void execDetailsEnd(int reqId) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void updateMktDepth(int tickerId, int position, int operation, int side,
			double price, int size) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void updateMktDepthL2(int tickerId, int position, String marketMaker,
			int operation, int side, double price, int size) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void updateNewsBulletin(int msgId, int msgType, String message,
			String origExchange) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void managedAccounts(String accountsList) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void receiveFA(int faDataType, String xml) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void historicalData(int reqId, String date, double open, double high,
			double low, double close, int volume, int count, double WAP,
			boolean hasGaps) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void scannerParameters(String xml) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void scannerData(int reqId, int rank, ContractDetails contractDetails,
			String distance, String benchmark, String projection, String legsStr) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void scannerDataEnd(int reqId) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void realtimeBar(int reqId, long time, double open, double high,
			double low, double close, long volume, double wap, int count) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void currentTime(long time) {
		currentTime.setTime(time);
	}
	
	@Override
	public void fundamentalData(int reqId, String data) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void deltaNeutralValidation(int reqId, UnderComp underComp) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void tickSnapshotEnd(int reqId) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void marketDataType(int reqId, int marketDataType) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void commissionReport(CommissionReport commissionReport) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void connect() throws ConnectionException {
		
	    ibClientSocket = new EClientSocket(this);
	    
	    if (isConnected()) {
	        System.out.println("Already connected to the IB interface.");
	    } else {
	    
	        ibClientSocket.eConnect(GATEWAY_HOST, GATEWAY_PORT, CLIENT_ID);
		
    		if (!isConnected()) {
    		    throw new ConnectionException("Failed to connect to IB Gateway");
    		}
    		
	    }
		
	}
	
	@Override
	public void disconnect() {
	    ibClientSocket.eDisconnect();
	}
	
	@Override
	public boolean isConnected() {
		
		return ibClientSocket.isConnected();
	}
	
	/**
	 * Initialize the BrokerManager_T by performing the following tasks:
	 * 1. Retrieve all open orders from the broker (IB)
	 * 2. Get the number of open orders we believe we should have based on our database data
	 * 3. Sleep until the wakeup() method is called meaning all open orders have been returned from IB
	 * 
	 */
	@Override
	public void initialize() {
	    
	    databaseManager = (DatabaseManager_T) DayTrader_T.getManager(DatabaseManager_T.class);
	    logger = (LoggerManager_T) DayTrader_T.getManager(LoggerManager_T.class);
		
		try {
            connect();
        } catch (ConnectionException e) {
            // TODO Handle the exception
            e.printStackTrace();
        }
		
		//empty the current holdings map before getting the open orders from IB
		holdings.clear();
		
		// request all our current holdings from IB
		//reqOpenOrder returns through the openOrder() and orderStatus() methods
		ibClientSocket.reqOpenOrders();
		

		int dbNumHoldings = databaseManager.numOpenHoldings();
        
        //if the broker hasn't returned all the open holdings, sleep five more seconds
        while (dbNumHoldings != holdings.size()) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                // Do nothing
            }
        } 
        
				
	}
	
	@Override
	public void sleep() {
		
	    boolean sleep = true;
		
		while (sleep) {
    	    try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                wakeup();
                sleep = false;
            }
		}
		
	}
	
	@Override
	public void terminate() {
		disconnect();		
	}
	
	/**
	 * Wake this thread up from the sleep state. Called when:
	 * 1. The broker has returned all our open orders
	 * 
	 */
	@Override
	public void wakeup() {
	  
	    int dbNumHoldings = databaseManager.numOpenHoldings();
        
        //if the broker hasn't returned all the open holdings, sleep five more seconds
        while (dbNumHoldings != holdings.size()) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                //Do nothing
            }
        }  
	    
        //Log our current holding if the appropriate logging level is set
        if (logger.logLevel().toInt() >= Level.INFO_INT) {
            if (holdings.size() > 0) {
                Iterator<Holding_T> it = holdings.values().iterator();
                while (it.hasNext()) {
                    Holding_T holding = (Holding_T) it.next();
                    logger.logText(holding.toString(), Level.INFO);
                }
            } else {
                logger.logText("There are no currently open orders", Level.INFO);
            }
        }
        
        
        //go back to sleep
        sleep();
		
	}

    @Override
    public void run() {
                
        while (!Thread.interrupted()) {
            sleep();
        }
    }

    public long getBrokerTime() {
        // TODO Auto-generated method stub
        return 0;
    }

}