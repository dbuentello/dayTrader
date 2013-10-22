/**
 * 
 */
package util;

/**
 * @author nathan
 *
 */
public class OrderStatus_T {


    /**
     * PendingSubmit - indicates that you have transmitted the order, but have
     *  not yet received confirmation that it has been accepted by the order des-
     *  tination. NOTE: This order status is not sent by TWS and should be explic-
     *  itly set by the API developer when an order is submitted. 
     */
    public static final String PENDING_SUBMIT = "PendingSubmit";
    
    /**
     * Inactive - indicates that the order has been accepted by the system (sim-
     *   ulated orders) or an exchange (native orders) but that currently the order is
     *   inactive due to system, exchange or other issues.
     */
    public static final String INACTIVE = "Inactive";
    
    /**
     * PendingCancel - indicates that you have sent a request to cancel the order
     *  but have not yet received cancel confirmation from the order destination.
     *  At this point, your order is not confirmed canceled. You may still receive
     *  an execution while your cancellation request is pending. NOTE: This
     *  order status is not sent by TWS and should be explicitly set by the API
     *  developer when an order is canceled.
     */
    public static final String PRE_SUBMITTED = "PreSubmitted";
    
    /**
     * Submitted - indicates that your order has been accepted at the order destination and is working.
     */
    public static final String SUBMITTED = "Submitted";
    
    /**
     * Cancelled - indicates that the balance of your order has been confirmed
     *  canceled by the IB system. This could occur unexpectedly when IB or the
     *  destination has rejected your order.
     */
    public static final String CANCELLED = "Cancelled";
    
    /**
     * Filled - indicates that the order has been completely filled.
     */
    public static final String FILLED = "Filled";
    
    /**
     * INVALID - indicates the status string is not a known status and is therefore invalid
     */
    public static final String INVALID = "INVALID";
    
    
    private static final String[] statuses = { PENDING_SUBMIT, INACTIVE, PRE_SUBMITTED, SUBMITTED, CANCELLED, FILLED };
    
    
    private String status;
    
    
    public OrderStatus_T(String status) {
        setStatus(status);
    }
    
    public static boolean isValidStatus(String status) {
        boolean isValid = false;
        
        for (int i = 0; i < statuses.length; i++) {
            if (statuses[i].equalsIgnoreCase(status)) {
                isValid = true;
            }
        }
        
        return isValid;
    }

    /**
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(String status) {
        if(isValidStatus(status)) {
            this.status = status;
        } else {
            this.status = INVALID;
        }
    }
    
    
    
    
}   
