package com.ib.client;

public interface AnyWrapper {
  /* {src_lang=Java}*/


  void error(Exception e);

  void error(String str);

  void error(int id, int errorCode, String errorMsg);

  void connectionClosed();

}