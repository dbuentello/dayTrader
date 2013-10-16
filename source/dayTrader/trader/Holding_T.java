package trader;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

import managers.DatabaseManager_T;
import marketdata.Symbol_T;

import org.hibernate.HibernateException;

import util.OrderStatus_T;

import interfaces.Persistable_IF;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderState;

public class Holding_T implements Persistable_IF {
  /* {src_lang=Java}*/
    
    private long id;
    
    private Order order;
    private Contract contract;
    private OrderState orderState;
    private Symbol_T symbol;
    /** Specifies the number of shares that have been executed. */
    private int filled;
    /** Specifies the number of shares still outstanding. */
    private int remaining;
    /** The average price of the shares that have been executed. This parameter is valid
     *  only if the filled parameter value is greater than zero. Otherwise, the price param-
     *  eter will be zero. This can also be considered as the "buy price".
     */
    private double avgFillPrice;
    /**
     * The last price of the shares that have been executed. This parameter is valid only
     *  if the filled parameter value is greater than zero. Otherwise, the price parameter
     *  will be zero.
     */
    private double lastFillPrice;
    /** The order ID of the parent order, used for bracket and auto trailing stop orders. */
    private int parentId;
    /** The timestamp that this holding was purchased */
    private Date buyDate;
    /** The timestamp that this holding was sold. Null if the holding has not yet been sold */
    private Date sellDate = null;
    /** The calculated high sell price that we would like to execute a sell order on this holding. */
    private Integer execSellPriceHigh = null;
    /** The calculated low sell price that we would like to execute a sell order on this holding. */
    private Integer execSellPriceLow = null;
    /** The amount of money gained by selling this order. If the order has not been sold or was sold at a loss
      set this field to zero. */
    private double gains = 0;
    /** The amount of money lost by selling this order. If the order has not been sold or was sold at a gain
      set this field to zero. */
    private double losses = 0;
    
    
    /**
     * 
     */
    protected Holding_T() {
        // TODO Auto-generated constructor stub
    }

    /**
     * @param order
     * @param contract
     */
    public Holding_T(Order order, Contract contract, OrderState orderState) {
        this.order = order;
        this.contract = contract;
        this.orderState = orderState;
    }

    /**
     * The total dollar amount of this holding that have been sold and are now realized gains/losses.
     * 
     * @return realized gains/losses
     */
    public double getSellTotal() {
        return (gains + losses);
    }
    
    //Redundant method
    public double getRealizedGainsLosses() {
        return getSellTotal();
    }
    
    /**
     * @return the id
     */
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    public long getId() {
        return id;
    }
    /**
     * @param id the id to set
     */
    public void setId(long id) {
        this.id = id;
    }
    
    /**
     * @return the order
     */
    @Transient
    public Order getOrder() {
        return order;
    }

    /**
     * @param order the order to set
     */
    public void setOrder(Order order) {
        this.order = order;
    }
    
    @Column ( name = "order_id" )
    public int getOrderId() {
        return order.m_orderId;
    }
    
    /**
     * Return the buy price that was specified for this holding.
     */
    @Column( name = "order_buy_price" )
    public double getBuyPrice() {
        double buyPrice = 0;
        
        if (order.m_auxPrice == 0) {
            buyPrice = order.m_lmtPrice;
        } else if (order.m_lmtPrice == 0) {
            buyPrice = order.m_auxPrice;
        }
        return buyPrice;
    }
    
    public void setBuyPrice(double orderBuyPrice) {
        //Do nothing
    }
    
    public void setOrderId(int orderId) {
        this.order.m_orderId = orderId;
    }

    /**
     * Get the last known status of this holding
     * 
     * @return order status
     */
    @Column( name = "order_status" )
    public String getStatus() {
        return this.orderState.m_status;
    }
    
    public void setOrderStatus(String status) {
        this.orderState.m_status = status;
    }

    /**
     * @return the contract
     */
    @Transient
    public Contract getContract() {
        return contract;
    }

    /**
     * @param contract the contract to set
     */
    public void setContract(Contract contract) {
        this.contract = contract;
    }

    @Column( name = "contract_id" )
    public int getContractId() {
        return contract.m_conId;
    }
    
    public void setContractId(int conId) {
        this.contract.m_conId = conId;
    }
    
    /**
     * @return the orderState
     */
    @Transient
    public OrderState getOrderState() {
        return orderState;
    }

    /**
     * @param orderState the orderState to set
     */
    public void setOrderState(OrderState orderState) {
        this.orderState = orderState;
    }
    
    /**
     * The ticker symbol of this holding.
     * 
     * @return the symbol
     */
    public Symbol_T getSymbol() {
        return symbol;
    }

    /**
     * @param symbol the symbol to set
     */
    public void setSymbol(Symbol_T symbol) {
        this.symbol = symbol;
    }
    
    @Column( name = "symbol_id" )
    public long getSymbolId() {
        return symbol.getId();
    }
    
    public void setSymbolId(long symbolId) {
        this.symbol.setId(symbolId);
    }

    public String toString() {
        String str = "OrderID: " + order.m_orderId;
        str = ", Symbol: " + contract.m_symbol;
        str += ", Status: "  +orderState.m_status;
        str += ", OrderType: " + order.m_orderType;
        
        
        return str;
    }

    @Override
    public long insert() throws HibernateException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void delete() throws HibernateException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void update() throws HibernateException {
        // TODO Auto-generated method stub
        
    }

    /**
     * Specifies the number of shares that have been executed.
     * @return the filled
     */
    @Column( name = "filled" )
    public int getFilled() {
        return filled;
    }

    /**
     * @param filled the filled to set
     */
    public void setFilled(int filled) {
        this.filled = filled;
    }

    /**
     * Specifies the number of shares still outstanding.
     * 
     * @return the @code{remaining}
     */
    @Column( name = "remaining" )
    public int getRemaining() {
        return remaining;
    }

    /**
     * @param remaining the @code{remaining} to set
     */
    public void setRemaining(int remaining) {
        this.remaining = remaining;
    }
    
    /**
     * Get the total number of shares bought and sold for this holding.
     * 
     * @return total number of shares
     */
    public int getNumOfShares() {
        return (filled + remaining);
    }

    /**
     * The average price of the shares that have been executed. This parameter is valid
     *  only if the filled parameter value is greater than zero. Otherwise, the price 
     *  parameter will be zero. This can also be considered as the "buy price".
     *  
     * @return the avgFillPrice
     */
    @Column( name = "avg_fill_price" )
    public double getAvgFillPrice() {
        return avgFillPrice;
    }

    /**
     * @param avgFillPrice the avgFillPrice to set
     */
    public void setAvgFillPrice(double avgFillPrice) {
        this.avgFillPrice = avgFillPrice;
    }

    /**
     * The last price of the shares that have been executed. This parameter is valid only
     *  if the filled parameter value is greater than zero. Otherwise, the price parameter
     *  will be zero.
     *  
     * @return the lastFillPrice
     */
    @Transient
    public double getLastFillPrice() {
        return lastFillPrice;
    }

    /**
     * @param lastFillPrice the lastFillPrice to set
     */
    public void setLastFillPrice(double lastFillPrice) {
        this.lastFillPrice = lastFillPrice;
    }

    /**
     * The order ID of the parent order, used for bracket and auto trailing stop orders.
     * 
     * @return the parentId
     */
    @Column( name = "parent_id" )
    public int getParentId() {
        return parentId;
    }

    /**
     * @param parentId the parentId to set
     */
    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    /**
     * The timestamp that this holding was purchased
     * 
     * @return the buyDate
     */
    @Column( name = "buy_date" )
    public Date getBuyDate() {
        return buyDate;
    }

    /**
     * @param buyDate the buyDate to set
     */
    public void setBuyDate(Date buyDate) {
        this.buyDate = buyDate;
    }

    /**
     * The timestamp that this holding was sold. Null if the holding has not yet been sold
     * 
     * @return the sellDate
     */
    @Column( name = "sell_date" )
    public Date getSellDate() {
        return sellDate;
    }

    /**
     * @param sellDate the sellDate to set
     */
    public void setSellDate(Date sellDate) {
        this.sellDate = sellDate;
    }

    /**
     * The calculated high sell price that we would like to execute a sell order on this holding.
     * 
     * @return the execSellPriceHigh
     */
    @Column ( name = "exec_sell_price_high" )
    public Integer getExecSellPriceHigh() {
        return execSellPriceHigh;
    }

    /**
     * @param execSellPriceHigh the execSellPriceHigh to set
     */
    public void setExecSellPriceHigh(Integer execSellPriceHigh) {
        this.execSellPriceHigh = execSellPriceHigh;
    }

    /**
     * The calculated low sell price that we would like to execute a sell order on this holding.
     * 
     * @return the execSellPriceLow
     */
    @Column ( name = "exec_sell_price_low" )
    public Integer getExecSellPriceLow() {
        return execSellPriceLow;
    }

    /**
     * @param execSellPriceLow the execSellPriceLow to set
     */
    public void setExecSellPriceLow(Integer execSellPriceLow) {
        this.execSellPriceLow = execSellPriceLow;
    }

    /**
     * The amount of money gained by selling this order. If the order has not been sold or was sold at a loss
     * set this field to zero.
     * 
     * @return the gains
     */
    @Column( name = "gains" )
    public double getGains() {
        return gains;
    }

    /**
     * @param gains the gains to set
     */
    public void setGains(double gains) {
        this.gains = gains;
    }

    /**
     * The amount of money lost by selling this order. If the order has not been sold or was sold at a gain
     * set this field to zero.
     * 
     * @return the losses
     */
    @Column( name = "losses" )
    public double getLosses() {
        return losses;
    }

    /**
     * @param losses the losses to set
     */
    public void setLosses(double losses) {
        this.losses = losses;
    }
    

}