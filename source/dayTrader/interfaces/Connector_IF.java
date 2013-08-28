package interfaces;


/** 
 *  Defines the interface for connector types
 */
public interface Connector_IF {
  /* {src_lang=Java}*/


  public abstract void connect();

  public abstract void disconnect();

  public abstract void isConnected();

}