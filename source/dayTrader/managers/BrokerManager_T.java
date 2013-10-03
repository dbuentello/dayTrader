package managers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Level;

import trader.Holding_T;

import interfaces.Connector_IF;
import interfaces.Manager_IF;

import com.ib.client.CommissionReport;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.EClientSocket;
import com.ib.client.EWrapper;
import com.ib.client.Execution;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.UnderComp;

import exceptions.ConnectionException;

public class BrokerManager_T implements Manager_IF, Connector_IF, EWrapper {
  /* {src_lang=Java}*/

    private final String GATEWAY_HOST = "localhost";
    private final int GATEWAY_PORT = 4001;
    private final int CLIENT_ID = 1;
    private final String  ACCOUNT_CODE = "DU171047";
    
    private List<Holding_T> holdings = new ArrayList<Holding_T>();
    
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
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void openOrder(int orderId, Contract contract, Order order,
			OrderState orderState) {
		
	    holdings.add(new Holding_T(order, contract, orderState));
		
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
		// TODO Auto-generated method stub
		
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
	
	@Override
	public void initialize() {
		
	    ibClientSocket = new EClientSocket(this);
		try {
            connect();
        } catch (ConnectionException e) {
            // TODO Handle the exception
            e.printStackTrace();
        }
		
		// request all our current holdings from IB
		ibClientSocket.reqOpenOrders();
		
		Iterator<Holding_T> it = holdings.iterator();
		while (it.hasNext()) {
		    Holding_T openTrade = (Holding_T) it.next();
		    LoggerManager_T.logText(openTrade.getOrderString(), Level.INFO);
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
		disconnect();
		
	}
	
	@Override
	public void wakeup() {
		// TODO Auto-generated method stub
		
	}

}