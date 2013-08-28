package trader;

import com.ib.client.Contract;
import com.ib.client.Order;

public class Trade_T {
  /* {src_lang=Java}*/
    
    Order order;
    Contract contract;
    
    /**
     * 
     */
    public Trade_T() {
        // TODO Auto-generated constructor stub
    }

    /**
     * @param order
     * @param contract
     */
    public Trade_T(Order order, Contract contract) {
        this.order = order;
        this.contract = contract;
    }
    

}