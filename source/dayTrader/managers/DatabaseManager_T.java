package managers;

import interfaces.Connector_IF;
import interfaces.LifeState_IF;

/** 
 *  This class will manage the database connection. 
 *  It will be invoked to persist, update, retrieve and delete items from the database.
 *  In Java it will invoke the methods of the MySQL Connector/J class to interact with the database.
 */
public class DatabaseManager_T implements LifeState_IF, Connector_IF {
  /* {src_lang=Java}*/

    
    /**
     * 
     */
    public DatabaseManager_T() {
        // TODO Auto-generated constructor stub
    }

    public void persist() {
    }
    
    public void retrieve() {
    }
    
    public void update() {
    }
    
    public void delete() {
    }
    
    @Override
    public void connect() {
    	// TODO Auto-generated method stub
    	
    }
    
    @Override
    public void disconnect() {
    	// TODO Auto-generated method stub
    	
    }
    
    @Override
    public void isConnected() {
    	// TODO Auto-generated method stub
    	
    }
    
    @Override
    public void initialize() {
    	// TODO Auto-generated method stub
    	
    }
    
    @Override
    public void run() {
    	// TODO Auto-generated method stub
    	
    }
    
    @Override
    public void sleep() {
    	// TODO Auto-generated method stub
    	
    }
    
    @Override
    public void terminate() {
    	// TODO Auto-generated method stub
    	
    }
    
    @Override
    public void wakeup() {
    	// TODO Auto-generated method stub
    	
    }

}