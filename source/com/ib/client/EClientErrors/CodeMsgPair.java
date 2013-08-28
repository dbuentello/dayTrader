package com.ib.client.EClientErrors;

public class CodeMsgPair {
  /* {src_lang=Java}*/


  int m_errorCode;

  String m_errorMsg;

  public int code() {
 return m_errorCode;
  }

  public String msg() {
 return m_errorMsg;
  }

  public CodeMsgPair(int i, String errString) {
            m_errorCode = i;
            m_errorMsg = errString;
  }

}