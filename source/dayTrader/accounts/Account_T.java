package accounts;

import java.util.Date;

/** 
 *  This class will represent the Account. It will be used to manage the account, keep track of current balances, 
 *  make withdrawals and deposits, etc
 */
public class Account_T {
  /* {src_lang=Java}*/

    /** The clientId for this account */
    private int clientId = 1;
    /** The account code for this account. */
    private String  accountCode;
    /** The current balance of this account. */
    private double balance;
    /** The last time this account information was updated. */
    private Date updateTime;
    /** Boolean flag indicated if this account has finished updating since the last request was made to update. */
    private boolean isUpdated;
    
    
    
    /**
     * 
     */
    public Account_T(int clientId, String accountCode) {
    	
        this.clientId = clientId;
        this.accountCode = accountCode;
    }
    
    /**
     * @return the clientId
     */
    public int getClientId() {
        return clientId;
    }

    /**
     * @param clientId the clientId to set
     */
    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    /**
     * @return the accountCode
     */
    public String getAccountCode() {
        return accountCode;
    }

    /**
     * @param accountCode the accountCode to set
     */
    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    /**
     * @return the balance
     */
    public double getBalance() {
        return balance;
    }

    /**
     * @param balance the balance to set
     */
    public void setBalance(double balance) {
        this.balance = balance;
    }

    /**
     * @return the updateTime
     */
    public Date getUpdateTime() {
        return updateTime;
    }

    /**
     * @param updateTime the updateTime to set
     */
    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * @return the isUpdated
     */
    public boolean isUpdated() {
        return isUpdated;
    }

    /**
     * @param isUpdated the isUpdated to set
     */
    public void setUpdated(boolean isUpdated) {
        this.isUpdated = isUpdated;
    }

}