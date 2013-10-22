package managers;

import interfaces.Connector_IF;
import interfaces.Manager_IF;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import marketdata.MarketData_T;
import marketdata.Symbol_T;

import org.apache.log4j.Level;

import trader.Holding_T;
import trader.Trader_T;
import util.OrderAction_T;
import util.OrderStatus_T;

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
    
    /** A reference to the DatabaseManager class. */
    private DatabaseManager_T databaseManager;
    /** A reference to the TimeManager class to update the time returned from the broker. */
    private TimeManager_T timeManager;
    /** A reference to the LoggerManager class. */
    private LoggerManager_T logger;
    /** HashMap of orderIds to Holdings so we can easily retrieve holding information based on orderId. */
    private Map<Integer, Holding_T> holdings = new HashMap<Integer, Holding_T>();
    /** Reference to the IB client socket that is used to send requests to IB */
    EClientSocket ibClientSocket = null;
    /** Reference to the Trader_T class used to assist in making trades. */
    Trader_T trader;
    
    /**
     * 
     */
    public BrokerManager_T() {
        
    }

    public void executeTrade() {
    }

	@Override
	public void error(Exception e) {
		
	}
	
	@Override
	public void error(String str) {
		
	}
	
	@Override
	public void error(int id, int errorCode, String errorMsg) {
		
	}
	
	@Override
	public void connectionClosed() {
		
	}
	
	@Override
	public void tickPrice(int tickerId, int field, double price, int canAutoExecute) {
		
	}
	
	@Override
	public void tickSize(int tickerId, int field, int size) {
		
	}
	
	@Override
	public void tickOptionComputation(int tickerId, int field, double impliedVol,
			double delta, double optPrice, double pvDividend, double gamma,
			double vega, double theta, double undPrice) {
		
	}
	
	@Override
	public void tickGeneric(int tickerId, int tickType, double value) {
		
	}
	
	@Override
	public void tickString(int tickerId, int tickType, String value) {
		
	}
	
	@Override
	public void tickEFP(int tickerId, int tickType, double basisPoints,
			String formattedBasisPoints, double impliedFuture, int holdDays,
			String futureExpiry, double dividendImpact, double dividendsToExpiry) {
		
	}
	
	/**
	 * This method is invoked by the IB server every time an order status changes, upon connecting
	 * to TWS, or after invoking the reqOpenOrders() method.
	 * 
	 */
	@Override
	public void orderStatus(int orderId, String status, int filled, int remaining,
			double avgFillPrice, int permId, int parentId, double lastFillPrice,
			int clientId, String whyHeld) {
		
	    //if we already have a reference to this holding, just update the fields
	    //otherwise add the new holding to our map
	    Holding_T holding = holdings.get(orderId);
	    if (holding == null) {
	        holding = new Holding_T(orderId);
	        holdings.put(orderId, holding);
	        	        
	        holding.getOrder().m_permId = permId;
	        holding.getOrder().m_clientId = clientId;
	        
	    }
	    
	    holding.setOrderStatus(status);
        holding.setFilled(filled);
        holding.setRemaining(remaining);
        holding.setAvgFillPrice(avgFillPrice);
        holding.setParentId(parentId);
        holding.setLastFillPrice(lastFillPrice);
        //the permId is stored in holding.order.m_permId
        //the clientId is stored in holding.order.m_cliendId
                
        
        //determine the necessary action based on the status of our order
        if (holding.getOrderStatus() == OrderStatus_T.FILLED) {
            
            //if it was sell order with no shared remaining, the order is complete and we no longer own this position
            //so update the holding sellDate. If the sellDate if populated, we will consider all shares of this holding 
            //are sold so be careful about how we update the sellDate
            
            if(holding.getOrder().m_action == OrderAction_T.SELL) {
                holding.setSellDate(timeManager.getTime());
            }
          //If a buy order has been filled, update the buy time
            if(holding.getOrder().m_action == OrderAction_T.BUY) {
                holding.setBuyDate(timeManager.getTime());
            }
            
            
        } 
        else if (holding.getOrderStatus() == OrderStatus_T.SUBMITTED) {
            
            //If a buy order has been at least partially filled, update the buy time
            if(holding.getOrder().m_action == OrderAction_T.BUY && filled > 0) {
                holding.setBuyDate(timeManager.getTime());
            }
        } else if (holding.getOrderStatus() == OrderStatus_T.CANCELLED) {
            //TODO: Perform actions for cancelled orders
        }
        
        
	    //update the DB with our holding
	    holding.insertOrUpdate();
	}
	
	@Override
	public void openOrder(int orderId, Contract contract, Order order,
			OrderState orderState) {
		
	    holdings.put(orderId, new Holding_T(order, contract, orderState));
		
	}
	
	@Override
	public void openOrderEnd() {
		
	    //Log our current holdings if the appropriate logging level is set
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
        
	}
	
	@Override
	public void updateAccountValue(String key, String value, String currency,
			String accountName) {
		
	}
	
	@Override
	public void updatePortfolio(Contract contract, int position,
			double marketPrice, double marketValue, double averageCost,
			double unrealizedPNL, double realizedPNL, String accountName) {
		
	}
	
	@Override
	public void updateAccountTime(String timeStamp) {
		
	}
	
	@Override
	public void accountDownloadEnd(String accountName) {
		
	}
	
	@Override
	public void nextValidId(int orderId) {
		
	}
	
	@Override
	public void contractDetails(int reqId, ContractDetails contractDetails) {
		
	}
	
	@Override
	public void bondContractDetails(int reqId, ContractDetails contractDetails) {
		
	}
	
	@Override
	public void contractDetailsEnd(int reqId) {
		
	}
	
	@Override
	public void execDetails(int reqId, Contract contract, Execution execution) {
		
	}
	
	@Override
	public void execDetailsEnd(int reqId) {
		
	}
	
	@Override
	public void updateMktDepth(int tickerId, int position, int operation, int side,
			double price, int size) {
		
	}
	
	@Override
	public void updateMktDepthL2(int tickerId, int position, String marketMaker,
			int operation, int side, double price, int size) {
		
	}
	
	@Override
	public void updateNewsBulletin(int msgId, int msgType, String message,
			String origExchange) {
		
	}
	
	@Override
	public void managedAccounts(String accountsList) {
	
	}
	
	@Override
	public void receiveFA(int faDataType, String xml) {
		
	}
	
	@Override
	public void historicalData(int reqId, String date, double open, double high,
			double low, double close, int volume, int count, double WAP,
			boolean hasGaps) {
		
	}
	
	@Override
	public void scannerParameters(String xml) {
		
	}
	
	@Override
	public void scannerData(int reqId, int rank, ContractDetails contractDetails,
			String distance, String benchmark, String projection, String legsStr) {
		
	}
	
	@Override
	public void scannerDataEnd(int reqId) {
		
	}
	
	@Override
	public void realtimeBar(int reqId, long time, double open, double high,
			double low, double close, long volume, double wap, int count) {
		
	}
	
	@Override
	public void currentTime(long time) {
		timeManager.setTime(time);
	}
	
	@Override
	public void fundamentalData(int reqId, String data) {
		
	}
	
	@Override
	public void deltaNeutralValidation(int reqId, UnderComp underComp) {
		
	}
	
	@Override
	public void tickSnapshotEnd(int reqId) {
		
	}
	
	@Override
	public void marketDataType(int reqId, int marketDataType) {
		
	}
	
	@Override
	public void commissionReport(CommissionReport commissionReport) {
		
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
	    timeManager = (TimeManager_T) DayTrader_T.getManager(TimeManager_T.class);
	    logger = (LoggerManager_T) DayTrader_T.getManager(LoggerManager_T.class);
		
	    trader = new Trader_T();
	    
		try {
            connect();
        } catch (ConnectionException e) {
            // TODO Handle the exception
            e.printStackTrace();
        }
		
		//empty the current holdings map before getting the open orders from IB
		holdings.clear();
		
		//TODO: Will reqOpenOrders() give us the positions we current own? If not we'll need to get the info
		//from the database.
		
		
		// request all our current holdings from IB. reqOpenOrder() returns through the openOrder()
		// and orderStatus() methods. Once all orders have been received, the openOrderEnd() method is invoked.
        ibClientSocket.reqOpenOrders();
        

	}
	
	@Override
	public void sleep() {
			
	}
	
	@Override
	public void terminate() {
		disconnect();
	}
	
	/**
	 *  
	 * 
	 */
	@Override
	public void wakeup() {

	    //upon waking up, re-initialize the BrokerManager
	    initialize();
		
	}


	/**
	 * Request the current time from the broker.
	 */
    public void reqCurrentTime() {
        //the reqCurrentTime() method returns through the currentTime() method
        ibClientSocket.reqCurrentTime();
    }
    
    /**
     * Sell any holdings that we currently own
     */
    public void liquidateHoldings() {
        
        ArrayList<Holding_T> orders = new ArrayList<Holding_T>();
        Iterator<Holding_T> it = holdings.values().iterator();
        while (it.hasNext()) {
            Holding_T holding = it.next();
            if(holding.isOwned()) {
                orders.add(holding);
            }
        }
        
        
        trader.createSellOrders(orders);
        
        it = orders.iterator();
        while(it.hasNext()) {
            Holding_T sellOrder = it.next();
            ibClientSocket.placeOrder(sellOrder.getOrderId(), sellOrder.getContract(), sellOrder.getOrder());
        }
    }
    

    /**
     * Calculate the market's biggest losers and then buy those positions.
     */
    public void buyBiggestLosers() {
        
        List<Symbol_T> biggestLosers = databaseManager.getBiggestLosers();
        
        List<Holding_T> buyOrders = trader.createMktBuyOrders(biggestLosers); 
        
        
    }
        
}