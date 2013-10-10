/**
 * 
 */
package util;

/**
 * Enumeration of the different exchanges se can trade on
 * 
 * @author nathan
 *
 */
public class Exchange_T {
    
    //TODO: enum values shouldn't be lowercase as that goes against good practices, but when hibernate
    //TODO: stores the enum as a string is uses the value name. Our db currently is all lowercase
    //TODO: either need to create a user type for the exchange enum, have the db use uppercase, or do this
    public final static String AMEX = "amex";
    public final static String NASDAQ = "nasdaq";
    public final static String NYSE = "nyse";
    
    private final String value;
    
    Exchange_T(String s) {
        value = s;
    }
    
    public String getExchange() {
        return value;
    }
    
}
