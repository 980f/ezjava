/**
* Title:        PasscodeValue
* Description:
* Copyright:    2000 PayMate.net
* Company:      paymate
* @author       paymate
* @version      $Id: PasscodeValue.java,v 1.6 2003/07/27 05:34:58 mattm Exp $
*/
package pers.hal42.data;

import pers.hal42.lang.StringX;

/** todo: replace String with byte[] and write over it as part of Clear. Also a finalizer should erase the data although with java that delay/uncertainty keeps us from doing as good a job of that as we'd like */

public class PasscodeValue extends Value {
  String content;

  public ContentType charType(){
    return ContentType.password;
  }

  public String Image(){
    return StringX.fill("",'*',content.length(),true);
  }

  public boolean setto(String image){
    content= image; //+_+
    return true; //promiscuous type accepts anything superficially textual
  }

  public void Clear(){
    content= "";
  }

  public String Value(){
    return content;
  }

  public PasscodeValue(){
    Clear();
  }

}
