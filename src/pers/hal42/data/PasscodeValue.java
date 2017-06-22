package pers.hal42.data;

import pers.hal42.lang.StringX;

/**
 * Value class to hold a password.
 * todo:1 replace String with byte[] and write over it as part of Clear. Also a finalizer should erase the data although with java that delay/uncertainty keeps us from doing as good a job of that as we'd like
 */

public class PasscodeValue extends Value {
  String content;

  public PasscodeValue() {
    Clear();
  }

  public ContentType charType() {
    return ContentType.password;
  }

  public String Image() {
    return StringX.fill("", '*', content.length(), true);
  }

  public boolean setto(String image) {
    content = image; //+_+
    return true; //promiscuous type accepts anything superficially textual
  }

  public void Clear() {
    content = "";
  }

  public String Value() {
    return content;
  }

}
