package interfaces;

import exceptions.ConnectionException;


/** 
 *  Defines the interface for connector types
 */
public interface Connector_IF {
  /* {src_lang=Java}*/
  
  public abstract void connect() throws ConnectionException;

  public abstract void disconnect();

  public abstract boolean isConnected();

}