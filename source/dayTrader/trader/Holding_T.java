package trader;

import javax.persistence.Transient;

import org.hibernate.HibernateException;

import util.OrderStatus_T;

import interfaces.Persistable_IF;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderState;

public class Holding_T implements Persistable_IF {
  /* {src_lang=Java}*/
    
    private Order order;
    private Contract contract;
    private OrderState orderState;
    private OrderStatus_T orderStatus;
    /** Specifies the number of shares that have been executed. */
    private int filled;
    /** Specifies the number of shares still outstanding. */
    private int remaining;
    /** The average price of the shares that have been executed. This parameter is valid
     *  only if the filled parameter value is greater than zero. Otherwise, the price param-
     *  eter will be zero.
     */
    private double avgFillPrice;
    /**
     * The last price of the shares that have been executed. This parameter is valid only
     *  if the filled parameter value is greater than zero. Otherwise, the price parameter
     *  will be zero.
     */
    private double lastFillPrice;
    /**
     * The order ID of the parent order, used for bracket and auto trailing stop orders.
     */
    private int parentId;
    
    /**
     * 
     */
    public Holding_T() {
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
     * @return the order
     */
    public Order getOrder() {
        return order;
    }

    /**
     * @param order the order to set
     */
    public void setOrder(Order order) {
        this.order = order;
    }

    /**
     * @param orderStatus the orderStatus to set
     */
    public void setOrderStatus(OrderStatus_T orderStatus) {
        this.orderStatus = orderStatus;
    }
    
    public String getOrderStatus() {
        return this.orderStatus.getStatus();
    }
    
    public void setOrderStatus(String status) {
        this.orderStatus = new OrderStatus_T(status);
    }

    /**
     * @return the contract
     */
    public Contract getContract() {
        return contract;
    }

    /**
     * @param contract the contract to set
     */
    public void setContract(Contract contract) {
        this.contract = contract;
    }

    /**
     * @return the orderState
     */
    public OrderState getOrderState() {
        return orderState;
    }

    /**
     * @param orderState the orderState to set
     */
    public void setOrderState(OrderState orderState) {
        this.orderState = orderState;
    }
    
    public String getOrderString() {
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
     * @return the filled
     */
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
     * @return the @code{remaining}
     */
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
     * @return the avgFillPrice
     */
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
     * @return the lastFillPrice
     */
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
     * @return the parentId
     */
    public int getParentId() {
        return parentId;
    }

    /**
     * @param parentId the parentId to set
     */
    public void setParentId(int parentId) {
        this.parentId = parentId;
    }
    

}