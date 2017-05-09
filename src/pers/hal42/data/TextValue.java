/**
* Title:        TextValue
* Description:
* Copyright:    2000 PayMate.net
* Company:      paymate
* @author       paymate
* @version      $Id: TextValue.java,v 1.4 2003/07/27 05:34:58 mattm Exp $
*/
package pers.hal42.data;

public class TextValue extends Value {
  protected String content;

  public ContentType charType(){
    return ContentType.alphanum;
  }

  public String Image(){
    return String.valueOf(content);
  }

  public boolean setto(String image){
    content= image; //+_+
    return true; //this promiscuous type accepts anything superficially textual
  }

  public void Clear(){
    content= "";
  }

  public TextValue(String defawlt){
    content= defawlt;
  }

  public TextValue(){
    this("DEFAULT"); //--- just for initial testing
  }

}
