package managers;

import interfaces.Connector_IF;
import interfaces.Manager_IF;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import marketdata.MarketData_T;
import marketdata.Symbol_T;

import org.apache.log4j.Level;

import trader.Holding_T;
import util.XMLTags_T;
import util.dtLogger_T;
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

    private dtLogger_T Log;  // shorthand   
   
    /** HashMap of orderIds to Holdings so we can easily retrieve holding information based on orderId. */
    private Map<Integer, Holding_T> holdings = new HashMap<Integer, Holding_T>();
    
    /** saved retrieved executed orders */
    private ArrayList<Execution> executedOrders = new ArrayList<Execution>();

    /** saved retrieved open orders */
    private ArrayList<Order> openOrders = new ArrayList<Order>();

    /** Reference to the IB client socket that is used to send requests to IB */
    private EClientSocket ibClientSocket;
    
    
    /** Our Account */
    Account_T account;
    
    /** The next valid order id. */
    private int nextValidId = 0;
    
    /** Reference to a MarketData_T quote that can be used to store requested data. */
    private MarketData_T marketData = new MarketData_T();
    
    /** update complete indicators */
    private boolean nextValidIdComplete = false;
    private boolean marketDataUpdated = false;
    private boolean openOrderEndComplete = false;
    private boolean reqOrderEndComplete = false;
    
    
    /**
     * 
     */
    public BrokerManager_T() {
        
        String accountCode = ((ConfigurationManager_T) DayTrader_T.getManager(ConfigurationManager_T.class)).getConfigParam(XMLTags_T.CFG_ACCOUNT_CODE);
        account = new Account_T(1, accountCode);
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
		if (errorCode == 202)
			Log.println("[INFO] code="+errorCode+" "+errorMsg);
		else if (errorCode >= 2100 && errorCode <= 2120)
			Log.println("[WARNING] code="+errorCode+" "+errorMsg);
		else	
			Log.println("[ERROR] code="+errorCode+" "+errorMsg);		
	}
	
	/***** connection ****/
	
	// TODO we could introduce retry logic or fail completely
	public void connect() throws ConnectionException {
	
	    if (isConnected()) {
	        Log.println("[INFO] Already connected to the IB interface.");
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
	    Log = DayTrader_T.dtLog;
	    
	    try {
	        connect();
	    } catch (ConnectionException e) {
	        // TODO Handle the exception
	        e.printStackTrace();
	        							//TODO - fix this!
	        Log.println("[FATAL] Cant connect to IB");
	        return;						//TODO - we cant do much w/o a connection
	    }
	    
	    
		//empty the current holdings map before getting the open orders from IB
		holdings.clear();
		
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

		System.out.println("BrokerMgr wakeup...");  // dont change this to Log!  it hasnt been initialized yet
		
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
        	// and that updates the timemgr
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
     * then use getAccount() to retrieve the info
     *
     * @return boolean updated
     */
    public boolean updateAccount() {
    	
        account.setUpdated(false);
        //reqAccountUpdates returns through the updateAccountTime(), updateAccountValue(), updatePortfolio()
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

        
        if (waitCntr == MAX_WAIT) {
        	Log.println("[ERROR]: reqAccountUpdates timeout!");
        }
        else {
        	// debugging
        	Log.println("[DEBUG] Cash Balance is $"+account.getBalance());    	
        }

        return account.isUpdated();
    }

	@Override
	public void accountDownloadEnd(String accountName) {
		
		Log.println("{accountDownloadEnd}");
		
		// we've got everything
	    account.setUpdated(true);
	       
		// SALxx - what does this do?
		ibClientSocket.reqAccountUpdates(false, account.getAccountCode());
	}
	
	/**
	 * Callback for the reqAccountUpdates() request
	 */
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
	
	/**
	 * Callback for the reqAccountUpdates() request
	 */
	@Override
	public void updateAccountValue(String key, String value, String currency,
			String accountName) {
	    
	    //TODO: Handle other types of account updates - including time above!
	    if (key.equals("CashBalance")) {
	        account.setBalance(Integer.valueOf(value));
	        account.setUpdated(true);					//SALxx - this was missing
	        											// TODO; set flag on AccountDownloadEnd
	        Log.println("{AccountValue callback} CashBalance= "+value);
	    }

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
		
		System.out.println("[tickPrice] type= "+field+" $"+price);
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
		
		System.out.println("[tickGeneric] Id= "+tickerId+"="+value);
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
	    
		System.out.println("[tickSnapshotEnd] Id= "+reqId);
		marketDataUpdated = true;
	}


    /** 
     * Get the next valid order id from IB
     * This is a blocking call
     * 
     * @return next valid order id, 0 on timeout error
     */
    public int reqNextValidId() {
        nextValidIdComplete = false;
        
        int retryCntr=0;
        int MAX_RETRIES=10;
        
        ibClientSocket.reqIds(1);		// request only 1 id
        while (!nextValidIdComplete && retryCntr < MAX_RETRIES) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            retryCntr++;
        }
        
        if (retryCntr == MAX_RETRIES)
        	return 0;
        
        return nextValidId;
    }
    
	@Override
	public void nextValidId(int orderId) {
		Log.println("{NextValidId} recieved next Valid Id: "+orderId);

		nextValidId = orderId;
		
		nextValidIdComplete = true;
	}
    
	/**
	 * Test to ensure there is a validOrderId
	 * @return
	 */
	public boolean haveValidOrderId()
	{
		return (nextValidIdComplete);
	}
	
	/*
	 * This must be called each time a new order is created to guarantee
	 * a unique Id for this session.  The starting validId is established during
	 * the first connect (or when we loose a connection?).  Then we can
	 * increment it ourselves.
	 * 
	 * DO NOT call this if you arent going to use this value.  To reset it, call
	 * reqNextValidId()
	 */
	public int getNextOrderId()
	{
		return nextValidId++;
	}
	
	
	
	/***** Orders ****/
    
    public boolean placeOrder(Holding_T order)
    {
    	// use the current orderId - that relects what us in order.
        ibClientSocket.placeOrder(order.getCurrentOrderId(), order.getContract(), order.getOrder());
        
        //Log.println("[DEBUG] Placed " + order.getOrder().m_action + " order "+ order.getCurrentOrderId()+" for "+ order.getSymbol().getSymbol());
     	
        // keep our list of placed orders so they can be updated by IB orderStatus callback
        holdings.put(order.getCurrentOrderId(), order);
        
        return true;
    }
    
    /**
     * Cancel an order - the result is returned asynchronously as an OrderStatus
     * @param orderId
     */
    public void cancelOrder(int orderId) {
    	ibClientSocket.cancelOrder(orderId);
    }
    
    /**
     * This is used to initialize the holdings map with unfilled holdings
     * New holdings will be place in the map by placeOrder
     * 
     * @param holding
     */
    public void updateHoldings(Holding_T holding)
    {
        holdings.put(holding.getCurrentOrderId(), holding);    	
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
		
        Log.println("{orderStatus} " + orderId +" "+ status +" ("+ filled +" "+
              remaining +") at $"+ avgFillPrice +"/$"+lastFillPrice+" ["+ permId+"]");

	    Holding_T holding = holdings.get(orderId);
	    if (holding == null) {
	    	Log.println("[ERROR] OrderStatus()  This is bad! no Holding for orderid "+orderId);
	        return;
	    }
	    
	    // update with new data	    
	    holding.setOrderStatus(status);
	    
        
        //determine the necessary action based on the status of our order
	    // any submitted and filled are treated alike
        if (holding.getOrderStatus().equalsIgnoreCase(OrderStatus.PreSubmitted.toString())  ||
        	holding.getOrderStatus().equalsIgnoreCase(OrderStatus.PendingSubmit.toString()) ||
        	holding.getOrderStatus().equalsIgnoreCase(OrderStatus.Submitted.toString())     ||
        	holding.getOrderStatus().equalsIgnoreCase(OrderStatus.Filled.toString()) ) {
  
            // If were buying, set these parameters (filled is counted up in the DB
        	// whereas fill from the orderStatus is just for this execution (it could be
        	// partial)
            if (holding.isBuying() || holding.isOwned()) {
        		holding.setBuyDate(timeManager.getTime());
        		holding.setFilled(holding.getVolume() - remaining);
        		holding.setActualBuyPrice(avgFillPrice);
        	    holding.setPermId(permId);
            }
            else {	
        		holding.setSellDate(timeManager.getTime());
        		holding.setRemaining(remaining);
        		holding.setAvgFillPrice(avgFillPrice);
        	    holding.setPermId2(permId);
        	}
        }
        else if (holding.getOrderStatus().equalsIgnoreCase(OrderStatus.Cancelled.toString())) {
        	holding.setBuyDate(timeManager.getTime());
    		holding.setFilled(holding.getVolume() - remaining);
    		holding.setActualBuyPrice(avgFillPrice);
    	    holding.setPermId(permId);
    	    
    	    // for a cancel, reset the volume - it now matches what was filled
    	    holding.modifyVolume(holding.getVolume() - remaining);
        }
        else
        	Log.println("[DEBUG] orderStatus() Unhandled status: " + holding.getOrderStatus());
        
	    	// update the DB with our holding (note - this has buy/sell logic)
        if (holding.updateOrderPosition() == 0)
        	// note: only a warning, because nothing may have changed
        	// we get duplicate callbacks sometimes
        	Log.println("[WARNING] order status not updated in DB");

	}
	
	@Override
	public void openOrder(int orderId, Contract contract, Order order,
			OrderState orderState) {
		
		Log.println("{openOrder} id="+orderId+": "+orderState.m_status);
			
		// save these for when asked
        openOrders.add(order);
	}
	
	@Override
	public void openOrderEnd() {
		
		Log.println("{openOrderEnd}");

        openOrderEndComplete = true;
        
	}

	/**
	 * Callback for the reqAccountUpdates() request
	 */
	@Override
	public void updatePortfolio(Contract contract, int position,
			double marketPrice, double marketValue, double averageCost,
			double unrealizedPNL, double realizedPNL, String accountName) {
		
		Log.println("{Portfolio} for "+contract.m_symbol+" position="+position+
				" mkt= $"+marketPrice+" mktV="+marketValue+" aveCost= $"+averageCost +
				" PNL= $"+realizedPNL+"/$"+unrealizedPNL);
	}
	
	
    @Override
    public void accountSummary(int reqId, String account, String tag,
            String value, String currency) {

    	Log.println("{Account Summary}");
        
    }

    @Override
    public void accountSummaryEnd(int reqId) {

    	Log.println("{Account Summary End}");        
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
		Log.println("{CommissionReport} ExecId:"+commissionReport.m_execId+
				" $"+commissionReport.m_commission+ " PNL $"+commissionReport.m_realizedPNL);
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
	
	/**
	 * Callback for the reqExecutions() request
	 * Note: this is called by IB when an order is executed - and it could be
	 * a partial fill (but theres nothing that indicates that in this callback)
	 * (m_shares is what was filled this time, m_cumQty is what has been filled so far)
	 */
	@Override
	public void execDetails(int reqId, Contract contract, Execution execution) {
		Log.println("{execDetails} for OrderId "+execution.m_orderId+" (execId: " +execution.m_execId+") "+
					contract.m_symbol+" exec price $"+execution.m_price +"/$"+ execution.m_avgPrice +
					" shares: "+execution.m_shares+"/"+execution.m_cumQty + " at "+execution.m_time);
	
		// save these, becuz...? (for interruptions)
        executedOrders.add(execution);
   
	}
	
	@Override
	public void execDetailsEnd(int reqId) {
		Log.println("{execDetailsEnd}");
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
