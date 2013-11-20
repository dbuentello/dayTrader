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
import accounts.Account_T;

import com.ib.client.CommissionReport;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.EClientSocket;
import com.ib.client.EWrapper;
import com.ib.client.Execution;
import com.ib.client.ExecutionFilter;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.TickType;
import com.ib.client.UnderComp;
import com.ib.controller.OrderStatus;
import com.ib.controller.Types.Action;

import dayTrader.DayTrader_T;
import exceptions.ConnectionException;

public class BrokerManager_T implements EWrapper, Manager_IF, Connector_IF, Runnable {
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
    
    /** saved retrieved executed orders */
    private ArrayList<Execution> executedOrders = new ArrayList<Execution>();

    /** saved retrieved open orders */
    private ArrayList<Order> openOrders = new ArrayList<Order>();

    /** Reference to the IB client socket that is used to send requests to IB */
    private EClientSocket ibClientSocket;
    
    
    /** List of the accounts being managed. */
    Account_T account;
    /** The next valid order id. */
    private int nextValidId = 0;
    /** Reference to a MarketData_T quote that can be used to store requested data. */
    private MarketData_T marketData = new MarketData_T();
    
    /** update complete indicators */
    public boolean marketDataUpdated = false;
    private boolean openOrderEndComplete = false;
    private boolean reqOrderEndComplete = false;
    
    
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
     * required (but not used) for runnables
     */
    public void run()
    {
    	
    }



	@Override
	public void error(Exception e) {
		System.out.println("Error exception="+e.getMessage());		
	}
	
	@Override
	public void error(String str) {
		System.out.println("Error str="+str);
		
	}
	
	@Override
	public void error(int id, int errorCode, String errorMsg) {
		if (errorCode >= 2100 && errorCode <= 2120)
			System.out.println("Warning="+errorCode+" "+errorMsg);
		else	
			System.out.println("Error code="+errorCode+" "+errorMsg);		
	}
	
	/***** connection ****/
	
	// TODO we could introduce retry logic or fail completely
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
	
	@Override
	public void connectionClosed() {
		
	}
	
	
	
	
	
	/**
	 * Initialize the BrokerManager_T by performing the following tasks:
	 * 1. Retrieve all open orders from the broker (IB)
	 * 2. Get the number of open orders we believe we should have based on our database data
	 * 3. Sleep until the wakeup() method is called meaning all open orders have been returned from IB
	 *    SALxx - where is the sleep? and who calls wakeup?
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
	        							//SALxx - fix this!
	        logger.logText("FATAL - cont connect to IB", Level.ERROR);
	        return;						//SALxx - we cant do much w/o a connection
	    }
	    
	    
		//empty the current holdings map before getting the open orders from IB
		holdings.clear();
		
		//TODO: Will reqOpenOrders() give us the positions we current own? If not we'll need to get the info
		//from the database.
		
		
		// Put this in trader...  TODO: here in init or somewhere else?
		// this could take a while, so the other inits may not yet be complete!!!!
		// May be best to put in line before we run() time manager
		/***
		// Open holdings in the DB are those with a buy_date and volume != filled (selldate is null) or
		// sell_date will a volume != filled
		// put this info into our holdings map first, openOrder will confirm and update as necessary
		
		// request all our current holdings from IB. reqOpenOrder() returns through the openOrder()
		// and orderStatus() methods. Once all orders have been received, the openOrderEnd() method is invoked.
        ibClientSocket.reqOpenOrders();
        
        // get any submitted orders to see if they have been filled
        ExecutionFilter filter = new ExecutionFilter();
        int reqId = 25;				//Test
        ibClientSocket.reqExecutions(reqId, filter);
		***/
		
		//trader.getOutstandingOrders();
	}
	
	@Override
	public void terminate() {
		disconnect();
	}
	
	
	@Override
	public void sleep() {
			
	}
	
	// SALxx-- check if we want to do this...
	@Override
	public void wakeup() {

		System.out.println("BrokerMgr wakeup...");
		
	    //upon waking up, re-initialize the BrokerManager
	    initialize();
		
	}
	
	/***  open and executed order requests ***/
	
	/**
	 * request all our current holdings from IB. reqOpenOrder() returns through the openOrder()
     * and orderStatus() methods. Once all orders have been received, the openOrderEnd() method is invoked
     * and return what we found (there may not be any)
 	 *
	 * @param waitms  in 250 msec increments
	 * @return ArrayList<Order> of open orders
	 */
	public ArrayList<Order> reqOpenOrders(int waitms) {
		
		openOrderEndComplete = false;
		openOrders.clear();
		ibClientSocket.reqOpenOrders();
		
		// wait for a response
        int waitCntr=0;
        int MAX_WAIT = waitms/250;
        
        while (!openOrderEndComplete && waitCntr < MAX_WAIT) {
            
            try {
                Thread.sleep(250);
                waitCntr++;
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }           
        }	

        // IB seems to make the callback 3x for each open order - and theyre all the same
        // BUT there could be multiple call backs when parameters change, so
        // lets be cautious and save them all
        return openOrders;

	}
	
	/**
	 * Get execution status of this order
	 * 
	 * @param reqId
	 * @param waitms
	 * @return
	 */
	public ArrayList<Execution> reqExecutions(int reqId, int waitms) {
		reqOrderEndComplete = false;
		executedOrders.clear();
	   	
		ExecutionFilter filter = new ExecutionFilter();   // null filter
		ibClientSocket.reqExecutions(reqId, filter);

		// wait for a response
        int waitCntr=0;
        int MAX_WAIT = waitms/250;
        
        while (!reqOrderEndComplete && waitCntr < MAX_WAIT) {
            
            try {
                Thread.sleep(250);
                waitCntr++;
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }           
        }
        
		// TODO: there should only be one, but we get multiple callbacks from IB
        // once we determine its safe, we can delete the dups and only send one back
		return executedOrders;
	}
    
    /** time handling **/
	
	public void currentTime(long time) {
		//SALxx - there is an initialize problem, if bm is initialized before tm is
		// the first request for time will return here while tm is null
	    if (timeManager == null) {
	    	System.out.println("bm:currentTime - Init error - time manager is null");
	    	timeManager = (TimeManager_T) DayTrader_T.getManager(TimeManager_T.class);
	    }
		timeManager.setTime(time);
		System.out.println("updating time = " + time);
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
                    Thread.sleep(5000);		// SALxx - this never gets called if we cant connect
                } catch (ConnectionException e) {
                    e.printStackTrace();
                    break;					// SALxx - and we're stuck in an infinite loop
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                } 
                
            }
        }
    }
    
    
    /*******   Account *****/
    
    /**
     * @return the account
     * 
     * be sure to checked that isUpdated() is true
     */
    public Account_T getAccount() {
        return account;
    }
    
    /**
     * Request that our account be updated.  Wait for the update
     * 
     * updated will be set in the updateAccountValues() and updateAccountTime() callbacks
     *
     * @return boolean updated
     */
    public boolean updateAccount() {
    	
        account.setUpdated(false);
        ibClientSocket.reqAccountUpdates(true, account.getAccountCode());
        
        // wait until response is received
        int waitCntr=0;
        int MAX_WAIT = 10;
        while (!account.isUpdated() && waitCntr < MAX_WAIT) {
            
            try {
                Thread.sleep(250);
                waitCntr++;
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }           
        }

        
        if (waitCntr == MAX_WAIT)
        {
        	System.out.println("ERROR: reqAccountUpdates timeout!");
        }
        
        // debugging
        System.out.println("Cash Balance is $"+account.getBalance());
        System.out.println("WaitCntr="+waitCntr);        	


        return account.isUpdated();
    }

	@Override
	public void accountDownloadEnd(String accountName) {
		
		System.out.println("SALxx- accountDownloadEnd");
		
		// we've got everything
	    account.setUpdated(true);
	       
		// SALxx - what does this do?
		ibClientSocket.reqAccountUpdates(false, account.getAccountCode());
	}
	
	@Override
	public void updateAccountTime(String timeStamp) {
		
		// TODO  throws exception bad date!!!
/***		
	    SimpleDateFormat df = new SimpleDateFormat();
		try {
            account.setUpdateTime(df.parse(timeStamp));
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
***/
	}
	
	
	@Override
	public void updateAccountValue(String key, String value, String currency,
			String accountName) {
	    
	    //TODO: Handle other types of account updates - including time above!
	    if (key.equals("CashBalance")) {
	        account.setBalance(Integer.valueOf(value));
	        account.setUpdated(true);					//SALxx - this was missing
	        											// TODO; set flag on AccountDownloadEnd
	        System.out.println("SALxx -AccountValue callback= "+value);
	    }
/*SALxx	    
	    else {
	        logger.logText("Received key: " + key + " in updateAccountValue() for account: "
	                + accountName + " and don't know how to parse.", Level.INFO);
	    }
**/		
	}
    
	/******* market data and snapshots *******/
	
    /**
     * Get a snapshot of a single quote from IB.
     * @param tickerId
     * 
     * marketDataUpdated will be set in tickGeneric
     * SALxx - not working!!!
     */
    public boolean reqSymbolSnapshot(Symbol_T symbol) {
        
        Contract contract = new Contract();
        contract.m_currency = "USD";
        contract.m_exchange = "SMART";
    //SALxx - try this     - it may give different results based on the version of ibg
    //contract.m_primaryExch = "ISLAND" or symbol.getExchange() ? if m_exchange = "DIRECT"?
    
        //contract.m_localSymbol = symbol.getSymbol();  //SALxx - dont think this is eeded
        contract.m_secType = "STK";
        contract.m_symbol = symbol.getSymbol();
        
        marketDataUpdated = false;
        
        // take a snapshot (true), the genericTicklist must be empty
        // callback is tickGeneric when its got evertyhing its tickSnaphotEnd
        ibClientSocket.reqMktData((int) symbol.getId(), contract, null, true);

        // wait for result
        int waitCntr=0;
        int MAX_WAIT = 10;
        while (!marketDataUpdated && waitCntr < MAX_WAIT) {
            
            try {
                Thread.sleep(1000);
                waitCntr++;
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
        }
        
        if (waitCntr == MAX_WAIT)
        	System.out.println("ERROR: reqMktData timeout!");
       
        // debug
        System.out.println("data for "+symbol.getSymbol()+" = "+marketData.getLastPrice());
    
        return marketDataUpdated;
       
    }
    
    public MarketData_T getSymbolSnapshot()
    {
    	return marketData;
    }

	@Override
	public void tickPrice(int tickerId, int field, double price, int canAutoExecute) {
		
		System.out.println("SALxx- tickPrice type= "+field+" $"+price);
	    if (TickType.getField(field) == TickType.getField(TickType.ASK)) {
	        marketData.setAskPrice(price);
	    }
	    if (TickType.getField(field) == TickType.getField(TickType.LAST)) {
	        marketData.setLastPrice(price);
	    }
	    
		//marketDataUpdated = true;		    
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
		
		System.out.println("SALxx- tickGeneric Id= "+tickerId+"="+value);
		//marketDataUpdated = true;	
	}
	
	@Override
	public void tickString(int tickerId, int tickType, String value) {
		
	}
	
	@Override
	public void tickEFP(int tickerId, int tickType, double basisPoints,
			String formattedBasisPoints, double impliedFuture, int holdDays,
			String futureExpiry, double dividendImpact, double dividendsToExpiry) {
		
	}

	@Override
	public void tickSnapshotEnd(int reqId) {
	    
		System.out.println("SALxx - tickSnapshotEnd Id= "+reqId);
		marketDataUpdated = true;
	}


    /** 
     * Get the next valid order id from IB
     * @return next valid order id
     */
    public int reqNextValidId() {
        int orderId = nextValidId;
        
        int retryCntr=0;
        int MAX_RETRIES=10;
        
        ibClientSocket.reqIds(1);		// request only 1 id
        while (orderId == nextValidId && retryCntr < MAX_RETRIES) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            retryCntr++;
        }
        
        return orderId;
    }
    
	@Override
	public void nextValidId(int orderId) {
		System.out.println("Recieved next Valid Id: "+orderId);
		//this.nextValidId = orderId;
		this.nextValidId = orderId;
	}
    
	
	
    /**
     * Sell any holdings that we currently own
     */
/***	
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
***/    

    /**
     * Calculate the market's biggest losers and then buy those positions.
     */
	
/***	
    public void buyBiggestLosers() {
        
        //update our account to get the latest cash balance, sleep while the account get updated
        //TODO: put in a timeout after so many attempts
        while (account.isUpdated()) {
            updateAccount();			// SALxx FIX THIS!!! I added wait logic in updateAcct 
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
***/
	/***** Orders ****/
    
    public boolean placeOrder(Holding_T order)
    {
        ibClientSocket.placeOrder(order.getOrderId(), order.getContract(), order.getOrder());
        
        //logger.logText("Placing " + order.getOrder().m_action + " order #" + order.getOrderId() + " for symbol: " + order.getSymbol().getSymbol(), Level.DEBUG);

        System.out.println("Placed " + order.getOrder().m_action + " order "+ order.getOrderId()+" for "+ order.getSymbol().getSymbol());

        // TODO? wait for (first) acknowledgment
     	
        // keep our list of placed orders so they can be updated by IB orderStatus callback
        holdings.put(order.getOrderId(), order);
        
        return true;
    }
    
    /**
     * This is used to initialize the holdings map with unfilled holdings
     * 
     * @param holding
     */
    public void updateHoldings(Holding_T holding)
    {
        holdings.put(holding.getOrderId(), holding);    	
    }
	
	/**
	 * This method is invoked by the IB server every time an order status changes, upon connecting
	 * to TWS, or after invoking the reqOpenOrders() method.
	 * 
	 * We get the updated IB info and update our Holding
	 * 
	 */
	@Override
	public void orderStatus(int orderId, String status, int filled, int remaining,
			double avgFillPrice, int permId, int parentId, double lastFillPrice,
			int clientId, String whyHeld) {
		
        System.out.println("[orderStatus] " + orderId +" "+ status +" ("+ filled +" "+
              remaining +") at $"+ avgFillPrice +"/$"+lastFillPrice+" ["+ permId +" "+ parentId +" "+ clientId +"]");


	
        //N...if we already have a reference to this holding, just update the fields
	    //otherwise add the new holding to our map
        
	    Holding_T holding = holdings.get(orderId);
	    if (holding == null) {
	    	System.out.println("OrderStatus:  This is bad! no Holding for orderid "+orderId);
	        return;
	    }
	    
	    // update with new data
	    holding.getOrder().m_permId = permId;
	    holding.getOrder().m_clientId = clientId;
        //holding.setParentId(parentId);    
	    holding.setParentId(permId);		// this is the Id we can use in TWS
	    
	    holding.setOrderStatus(status);
        holding.setFilled(filled);
        holding.setRemaining(remaining);
        holding.setAvgFillPrice(avgFillPrice);
        holding.setLastFillPrice(lastFillPrice);
        
        //determine the necessary action based on the status of our order
        if (holding.getOrderStatus().equalsIgnoreCase(OrderStatus.Submitted.toString())) {
            
        	// SALxx check this!!  WHY?
            //If a buy order has been at least partially filled, update the buy time
            //if(holding.getOrder().m_action.equalsIgnoreCase(Action.BUY.toString()) && filled > 0) {
            if(holding.isOwned() && filled > 0) {
        		holding.setBuyDate(timeManager.getTime());
            }
        }
		else if (holding.getOrderStatus().equalsIgnoreCase(OrderStatus.Filled.toString())) {
            
            //if it was sell order with no shares remaining, the order is complete and we no longer own this position
            //so update the holding sellDate. If the sellDate if populated, we will consider all shares of this holding 
            //are sold so be careful about how we update the sellDate
			//SALxx - check this, too  FILLED should be a good enough trigger (and remaining should be 0)
            
            //if(holding.getOrder().m_action.equalsIgnoreCase(Action.SELL.toString())) {
            if(!holding.isOwned()) {
                holding.setSellDate(timeManager.getTime()); 
            }
            
            //If a buy order has been filled, update the buy time and the actual buy price
            // (AvgFillPrice in the DB will be the actual sell price)
            //if(holding.getOrder().m_action.equalsIgnoreCase(Action.BUY.toString())) {
            if(holding.isOwned()) {
                holding.setBuyDate(timeManager.getTime());
                //holding.setActualBuyPrice(avgFillPrice);  updateMarketPosition does this for us
            }
            
        }
        else if (holding.getOrderStatus().equalsIgnoreCase(OrderStatus.Cancelled.toString())) {
            //TODO: Perform actions for cancelled orders
        }
        else
        	System.out.println("orderStatus() Unhandled status: " + holding.getOrderStatus());
        
	    // update the DB with our holding (note - this has buy/sell logic)
        holding.updateMarketPosition();


	}
	
	@Override
	public void openOrder(int orderId, Contract contract, Order order,
			OrderState orderState) {
		
		System.out.println("[openOrder] id="+orderId+": "+orderState.m_status);
		
	    //SALxx-- N: holdings.put(orderId, new Holding_T(order, contract, orderState));
		
		// save these for when asked
        openOrders.add(order);
	}
	
	@Override
	public void openOrderEnd() {
		
		System.out.println("[openOrderEnd]");

/****		
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
***/
		
        openOrderEndComplete = true;
        
	}

	
	@Override
	public void updatePortfolio(Contract contract, int position,
			double marketPrice, double marketValue, double averageCost,
			double unrealizedPNL, double realizedPNL, String accountName) {
		
	}
	
	
    

    @Override
    public void accountSummary(int reqId, String account, String tag,
            String value, String currency) {
        // TODO Auto-generated method stub
    	System.out.println("SALxx - Account Summary");
        
    }

    @Override
    public void accountSummaryEnd(int reqId) {
        // TODO Auto-generated method stub
    	System.out.println("SALxx - Account Summary End");        
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
    
    // Not used...
    
    public void executeTrade() {
    }
    
	@Override
	public void fundamentalData(int reqId, String data) {
		
	}
	
	@Override
	public void deltaNeutralValidation(int reqId, UnderComp underComp) {
		
	}
	
	@Override
	public void marketDataType(int reqId, int marketDataType) {
		
	}
	
	@Override
	public void commissionReport(CommissionReport commissionReport) {
		
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
		System.out.println("[execDetails] for contract for reqId: " +reqId);
		System.out.println(contract.m_symbol+": exec price $"+execution.m_price +"/$"+ execution.m_avgPrice +
				" shares: "+execution.m_shares + " at "+execution.m_time);
	
		// save these
        executedOrders.add(execution);
	}
	
	@Override
	public void execDetailsEnd(int reqId) {
		System.out.println("[execDetailsEnd]");
		reqOrderEndComplete = true;	
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


}