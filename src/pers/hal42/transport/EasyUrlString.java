package pers.hal42.transport;

import pers.hal42.lang.StringX;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.math.Base64Codec;
import pers.hal42.text.Ascii;
import pers.hal42.text.TextList;

public class EasyUrlString implements isEasy {

  private String key;
  private String rawcontent;
  private String encodedcontent;
  private static final String DEFAULTKEY = "EasyUrlStringContent";

  public EasyUrlString() {
    this(DEFAULTKEY);
  }

  // in case you have two of them, you can name them differently and not do a push & pop in the parent
  public EasyUrlString(String key) {
    this.rawcontent = "";
    this.encodedcontent = "";
    this.key = key;
  }

  public EasyUrlString setrawto(String rawcontent) {
    this.rawcontent = rawcontent;
    encode();
    return this;
  }

  public EasyUrlString setrawto(byte[] ascii) {
    return setrawto(new String(ascii));
  }

  public EasyUrlString setencodedto(String encodedcontent) {
    this.encodedcontent = encodedcontent;
    decode();
    return this;
  }

  public String rawValue() {
    return rawcontent;
  }

  public String encodedValue() {
    return encodedcontent;
  }

  public boolean NonTrivial() {
    return StringX.NonTrivial(rawcontent);
  }

  private void encode() {
    encodedcontent = encode(rawcontent);
  }

  private void decode() {
    rawcontent = decode(encodedcontent);
  }

  public void load(EasyCursor ezp) {
    setencodedto(ezp.getString(key, ""));
  }

  public void save(EasyCursor ezp) {
    ezp.setString(key, StringX.TrivialDefault(encodedcontent, null)); // this prevents the item getting into the ezp if it is trivial (to prevent excess
  }

  public String toString() {
    return encodedValue();
  }

  public static boolean NonTrivial(EasyUrlString eus) {
    return (eus != null) && eus.NonTrivial();
  }

  // base64 is generally compressed better than URL encoding
  public static String encode(String raw) {
    try {
      return Base64Codec.toString(raw.getBytes());
    } catch (Exception e) {
      ErrorLogStream.Global().Caught(e);
      return null;
    }
  }

  public static String decode(String from) {
    try {
      return new String(Base64Codec.fromString(from));
    } catch (Exception e) {
      ErrorLogStream.Global().Caught(e);
      return null;
    }
  }

  ////////////
  // command line utility to demo this class
  private static void barf() {
    System.out.println("EasyUrlString option string\nwhere option is encode | decode.");
  }

  // for finding out what the values would be, if you could read them.  :)
  public static void main(String[] args) {
    if (args.length < 2) {
      barf();
      System.exit(2);
    }
    switch (args[0]) {
    case "encode":
      System.out.println("ascii " + Ascii.bracket(args[1].getBytes()) + " encoded to " + EasyUrlString.encode(args[1]));
      break;
    case "decode":
      System.out.println(args[1] + " decoded to " + Ascii.bracket(EasyUrlString.decode(args[1]).getBytes()));
      break;
    default:
      System.out.println("args = " + TextList.CreateFrom(args));
      barf();
      System.exit(1);
    }
  }

}
