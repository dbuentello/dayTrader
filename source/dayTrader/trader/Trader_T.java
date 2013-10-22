package trader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import dayTrader.DayTrader_T;

import util.OrderAction_T;

import managers.BrokerManager_T;
import managers.TimeManager_T;
import marketdata.Symbol_T;

public class Trader_T {
  /* {src_lang=Java}*/

    /** Minimum number of shares a security has to trade for us to buy. */
    public static final int MIN_TRADE_VOLUME = 10000;
    /** Minimum price for a security for us to buy it. */
    public static final double MIN_BUY_PRICE = 0.50;
    /** The maximum number of positions we want to buy. */
    public static final int MAX_BUY_POSITIONS = 25;
    
    /** A reference to the BrokerManager_T class. */
    private BrokerManager_T brokerManager = null;
    

    /**
     * 
     */
    public Trader_T() {
        
        brokerManager = (BrokerManager_T) DayTrader_T.getManager(BrokerManager_T.class);
    }

    public void buy() {
    }
    
    /**
     * Create a sell order for all the holdings contained within the List parameter. Returns a list of Holdings_T whose
     * contract and order fields will contain the data necessary to place the sell order
     * 
     * @param holdings
     * @return sell orders
     */
    public List<Holding_T> createSellOrders(ArrayList<Holding_T> orders) {
        
        
        //create a copy of our list, were going to return the same holdings, but
        //with the contract and order fields populated for a sell order
        //ArrayList<Holding_T> orders = new ArrayList<Holding_T>(holdings);
        
        Iterator<Holding_T> it = orders.iterator();
        while(it.hasNext()) {
            Holding_T order = it.next();
            
            //insert a new row in the database to get a new order id
            order.setId(order.insertOrUpdate());
            
            order.getOrder().m_orderId = order.getOrderId();
            //clientId fields should already be populated
            
            order.getOrder().m_action = OrderAction_T.SELL;
            order.getOrder().m_orderType = "MKT";
            order.getOrder().m_totalQuantity = order.getRemaining();
            order.getOrder().m_transmit = true;
            
            order.getOrder().m_lmtPrice = 0;
            order.getOrder().m_auxPrice = 0;
            order.getOrder().m_allOrNone = false;
            order.getOrder().m_blockOrder = false;
            order.getOrder().m_outsideRth = false;
           
            //the contractId field will already be populated
            order.getContract().m_currency = "USD";
            order.getContract().m_exchange = order.getSymbol().getExchange();
            order.getContract().m_primaryExch = order.getSymbol().getExchange();
            //order.getContract().m_expiry = String.valueOf(TimeManager_T.getYear4Digit() + TimeManager_T.getMonthDigit());
            order.getContract().m_localSymbol = order.getSymbol().getSymbol();
            order.getContract().m_symbol = order.getSymbol().getSymbol();
            //order.getContract().m_right = "PUT";
            order.getContract().m_secType = "STK";
            
            
        }
              
        
        return orders;
    }
    
    /**
     * Create market buy orders for the provided list of symbols
     * 
     * @param symbols - list of symbols to buy
     */
    public List<Holding_T> createMktBuyOrders(List<Symbol_T> symbols) {
        
        List<Holding_T> buyOrders = new ArrayList<Holding_T>();
        
        Iterator<Symbol_T> it = symbols.iterator();
        while (it.hasNext()) {
            Symbol_T symbol = it.next();
            
            Holding_T buyOrder = new Holding_T();
            
            //see updateBuyPositions() in GetQuotes.pl
            
            
        }
        
        
        
        return buyOrders;
    }

}