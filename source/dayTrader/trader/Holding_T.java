package trader;

import org.hibernate.HibernateException;

import interfaces.Persistable_IF;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderState;

public class Holding_T implements Persistable_IF {
  /* {src_lang=Java}*/
    
    private Order order;
    private Contract contract;
    private OrderState orderState;
    
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
    

}