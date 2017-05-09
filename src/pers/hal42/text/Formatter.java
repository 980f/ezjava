package pers.hal42.text;


import pers.hal42.lang.StringX;

public class Formatter {
  private Formatter() {
    // I'm here for static purposes
  }

  /**
   * @param b 8 bit number
   * @return human readable rendition of that number in base 16
   */
  public static String ox2(byte b) {
    char[] chars = new char[2]; //char rather than byte because that is what we can make a String from.
    chars[0] = hexDigit(b >> 4);
    chars[1] = hexDigit(b);
    return new String(chars);
  }

  /**
   * @return hex representation of lsbyte.
   */
  public static String ox2(int i) {
    return ox2( (byte) (i & 255));
  }

  public static String ox2(long l) {
    return ox2( (byte) (l & 255));
  }

  public static char hexDigit(int b) {
    return Character.forDigit(b & 15, 16);
  }

  public static StringBuffer hexImage(byte[] buffer, int offset, int length) {
    //+++ parameter checks needed.
    StringBuffer hexy = new StringBuffer(2 * length);
    length += offset; //now is end index
    for (int i = offset; i < length; i++) {
      hexy.append(ox2(buffer[i]));
    }
    return hexy;
  }

  public static StringBuffer hexImage(byte[] buffer, int offset) {
    return hexImage(buffer, offset, buffer.length - offset);
  }

  public static StringBuffer hexImage(byte[] buffer) {
    return hexImage(buffer, 0, buffer.length);
  }

  public static StringBuffer hexImage(String s) {
    return hexImage(s.getBytes());
  }

  public static String twoDigitFixed(long smallNumber) {
    return ((smallNumber <= 9) ? "0" : "") + smallNumber;
  }

  public static String twoDigitFixed(String smallNumber) {
    return twoDigitFixed(StringX.parseLong(smallNumber));
  }

  public static String ratioText(String prefix, String num, String denom) {
    return StringX.bracketed(prefix, num + "/" + denom);
  }

  /**
   * @return text for one integer over another.
   */
  public static String ratioText(String prefix, long num, long denom) {
    return ratioText(prefix, String.valueOf(num), String.valueOf(denom));
  }

  public static String ratioText(String prefix, int num, int denom) {
    return ratioText(prefix, String.valueOf(num), String.valueOf(denom));
  }

  public static String ratioText(int num, int denom) {
    return ratioText("(", num, denom);
  }

  public static final String scihexponent = " KMGTPEZY"; //for sizeLong
  private static final long Base1K = 1024;
  private static final long MSB1K = Base1K / 2;
  //below: 64 is bits in a long, 50 is bits we are going to discard, make a mask that retains the 14 that are left
  private static final long mask14bits=(2 << ((64-50) - 1)) - 1;//inline so that we don't have to access other packages

  /**
   * @return exponential representation of @param size in base 1024.
   * 0..1023 print as is, for other numbers drop bits in multiples of 10
   * and if the last 10 dropped bits are >512 add one to what is left, i.e. 1.5k becomes 2k while 1.4k becomes 1k.
   * negative numbers are treated as unsigned longs
   */
  public static String sizeLong(long size) {
    // treat size as unsigned
    long divided = size;
    boolean roundup = false;
    int hexponent = 0;
    if (size < 0) { //correction for really big numbers, ones > 1/2 max_value, i.e. we want to treat longs as unsigned 64 bit integer
      //alh: 50 is the smallest number that is a multiple of ten and for which 64-that number <20, the optimal number for fastest execution.
      divided >>= 50; //do the bulk of the dividing, leaves 14 bits behind
      hexponent = 5;
      //and wipe the extra copies of the msb, created by the shift being arithmetic rather than logical.
      divided &= mask14bits; //hoping compiler resolves constant>>constant to a constant ;P
    }
    while (divided >= Base1K) {
      roundup = (divided & MSB1K) != 0; //the 10 bits we are about to toss into oblivion
      divided>>=10;
      ++hexponent;
    }
    if (roundup) {
      ++divided;
    }
    StringBuilder ret = new StringBuilder();
    ret.append(divided);
    if (hexponent > 0) {
      ret.append(" ");
      ret.append(scihexponent.charAt(hexponent));
    }
    return ret.toString();
  }

  public static String ValueFromCvstag(String tagged) {
    int first = tagged.indexOf(':');
    if (first >= 0) {
      int second = tagged.indexOf('$', first);
      if (second >= 0) { //remove the single spaces on either side
        tagged = tagged.substring(++first, --second);
      }
    }
    return tagged.trim();
  }
}
//$Id: Formatter.java,v 1.3 2004/01/09 23:45:08 andyh Exp $
