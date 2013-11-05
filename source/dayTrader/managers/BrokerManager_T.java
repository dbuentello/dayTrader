package managers;

import interfaces.Connector_IF;
import interfaces.Manager_IF;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import accounts.Account_T;

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

public class BrokerManager_T implements EWrapper, Manager_IF, Connector_IF {
  /* {src_lang=Java}*/

    private final String GATEWAY_HOST = "localhost";
    private final int GATEWAY_PORT = 4001;
    
    
    /** A reference to the DatabaseManager class. */
    private DatabaseManager_T databaseManager;
    /** A reference to the TimeManager class to update the time returned from the broker. */
    private TimeManager_T timeManager;
    /** A reference to the LoggerManager class. */
    private LoggerManager_T logger;
    /** HashMap of orderIds to Holdings so we can easily retrieve holding information based on orderId. */
    private Map<Integer, Holding_T> holdings = new HashMap<Integer, Holding_T>();
    /** Reference to the IB client socket that is used to send requests to IB */
    private EClientSocket ibClientSocket;
    /** Reference to the Trader_T class used to assist in making trades. */
    private Trader_T trader;
    /** List of the accounts being managed. */
    Account_T account;
    /** The next valid order id. */
    private int nextValidId = 0;
    /** Reference to a MarketData_T quote that can be used to store requested data. */
    private MarketData_T marketData = new MarketData_T();
    
    
    /**
     * 
     */
    public BrokerManager_T() {
        
        // ACTUAL TRADER ACCOUNT CODE = "U1235379";
        // PAPER TRADER ACCOUNT CODE = "DU171047";
        account = new Account_T(1, "DU171047");
        ibClientSocket = new EClientSocket(this);
        
    }

    
    /**
     * @return the account
     */
    public Account_T getAccount() {
        return account;
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
	    
	    //TODO: Handle other types of account updates
	    if (key == "CashBalance") {
	        account.setBalance(Integer.valueOf(value));
	    } else {
	        logger.logText("Received key: " + key + " in updateAccountValue() for account: "
	                + accountName + " and don't know how to parse.", Level.INFO);
	    }
		
	}
	
	@Override
	public void updatePortfolio(Contract contract, int position,
			double marketPrice, double marketValue, double averageCost,
			double unrealizedPNL, double realizedPNL, String accountName) {
		
	}
	
	@Override
	public void updateAccountTime(String timeStamp) {
	    SimpleDateFormat df = new SimpleDateFormat();
		try {
            account.setUpdateTime(df.parse(timeStamp));
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	}
	
	@Override
	public void accountDownloadEnd(String accountName) {
		account.setUpdated(true);
		ibClientSocket.reqAccountUpdates(false, account.getAccountCode());
	}
	
	@Override
	public void nextValidId(int orderId) {
		this.nextValidId = orderId;
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
	
	
	public void currentTime(long time) {
		timeManager.setTime(time);
		System.out.println("updating time = " + time);
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
	
	public void connect() throws ConnectionException {
	
	    if (isConnected()) {
	        System.out.println("Already connected to the IB interface.");
	    } else {
	    
	        ibClientSocket.eConnect(GATEWAY_HOST, GATEWAY_PORT, account.getClientId());
		
    		if (!isConnected()) {
    		    throw new ConnectionException("Failed to connect to IB Gateway");
    		}
    		
	    }
		
	}
	
	
	public void disconnect() {
	    ibClientSocket.eDisconnect();
	}
	
	
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
		
	    try {
	        connect();
	    } catch (ConnectionException e) {
	        // TODO Handle the exception
	        e.printStackTrace();
	    }
	    
	    trader = new Trader_T();
	    
		//empty the current holdings map before getting the open orders from IB
		holdings.clear();
		
		//TODO: Will reqOpenOrders() give us the positions we current own? If not we'll need to get the info
		//from the database.
		
		//SALxx - get it first from DB
		
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
        
        if (isConnected()) {
            //the reqCurrentTime() method returns through the currentTime() method
            ibClientSocket.reqCurrentTime();
        } else {
            while (!isConnected()) {
                try {
                    connect();
                    Thread.sleep(500);
                } catch (ConnectionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } 
                
            }
        }
    }
    
    /**
     * Request that our account be updated.
     */
    public void updateAccount() {
        account.setUpdated(false);
        ibClientSocket.reqAccountUpdates(true, account.getAccountCode());
    }
    
    /**
     * Get a snapshot of a single quote from IB.
     * @param tickerId
     */
    public void reqSymbolSnapshot(Symbol_T symbol) {
         
        Contract contract = new Contract();
        contract.m_currency = "USD";
        contract.m_exchange = symbol.getExchange();
        contract.m_localSymbol = symbol.getSymbol();
        contract.m_secType = "STK";
        contract.m_symbol = symbol.getSymbol();
        
        
        ibClientSocket.reqMktData((int) symbol.getId(), contract, "", true);
    }
    
    
    /** 
     * Get the next valid order id from IB
     * @return next valid order id
     */
    public int reqNextValidId() {
        int orderId = nextValidId;
        
        ibClientSocket.reqIds(1);
        while (orderId == nextValidId) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        return orderId;
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
        
        
        orders = (ArrayList<Holding_T>) trader.createSellOrders(orders);
        
        it = orders.iterator();
        while(it.hasNext()) {
            Holding_T sellOrder = it.next();
            ibClientSocket.placeOrder(sellOrder.getOrderId(), sellOrder.getContract(), sellOrder.getOrder());
            logger.logText("Placing sell order #" + sellOrder.getOrderId() + " for symbol: " 
                    + sellOrder.getSymbol().getSymbol(), Level.DEBUG);
        }
    }
    

    /**
     * Calculate the market's biggest losers and then buy those positions.
     */
    public void buyBiggestLosers() {
        
        //update our account to get the latest cash balance, sleep while the account get updated
        //TODO: put in a timeout after so many attempts
        while (account.isUpdated()) {
            updateAccount();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
            
        // SALxx - moved into TimeMgr  List<Symbol_T> biggestLosers = databaseManager.getBiggestLosers();
        // TODO: pass in losers as an arg
        List<Symbol_T> biggestLosers = new ArrayList<Symbol_T>();
        
        List<Holding_T> buyOrders = trader.createMktBuyOrders(biggestLosers, account.getClientId()); 
        
        Iterator<Holding_T> it = buyOrders.iterator();
        while (it.hasNext()) {
            Holding_T buyOrder = it.next();
            ibClientSocket.placeOrder(buyOrder.getOrderId(), buyOrder.getContract(), buyOrder.getOrder());
            logger.logText("Placing buy order #" + buyOrder.getOrderId() + " for symbol: " 
                    + buyOrder.getSymbol().getSymbol(), Level.DEBUG);
        }
        
    }

    @Override
    public void position(String account, Contract contract, int pos,
            double avgCost) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void positionEnd() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void accountSummary(int reqId, String account, String tag,
            String value, String currency) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void accountSummaryEnd(int reqId) {
        // TODO Auto-generated method stub
        
    }
        
}