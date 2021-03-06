package pers.hal42.lang;


import pers.hal42.stream.ContentType;
import pers.hal42.transport.Value;

public class Bool extends Value implements Comparable {

  private boolean amtrue;
  public static final Bool TRUE = new Bool(true);
  public static final Bool FALSE = new Bool(false);
  public static final String TRUESTRING = Boolean.TRUE.toString();
  public static final String FALSESTRING = Boolean.FALSE.toString();
  private static final String TRUESTR = toString(true);
  private static final String FALSESTR = toString(false);

  public Bool(boolean amtrue) {
    this.amtrue = amtrue;
  }

  public Bool() {
    this(false);
  }

  public boolean testandclear() {
    synchronized (this) {
      try {
        return amtrue;
      } finally {
        amtrue = false;
      }
    }
  }

  public boolean testandset() {
    synchronized (this) {
      try {
        return amtrue;
      } finally {
        amtrue = true;
      }
    }
  }

  //synch set and clear with testandclear
  public synchronized void set() {
    amtrue = true;
  }

  public synchronized void Clear() {
    amtrue = false;
  }

  public boolean Value() {
    return amtrue;
  }

  public ContentType charType() {//for Value hierarchy
    return ContentType.alphanum;
  }

  public String Image() {
    return toString(amtrue);
  }

  public String toString() {
    return Image();
  }

  public long asLong() {
    return amtrue ? 1L : 0L;
  }

  public int asInt() {
    return asInt(amtrue);
  }

  public boolean asBoolean() {
    return amtrue;
  }

  public boolean setto(String image) {
    amtrue = For(image);
    return amtrue;
  }

  public boolean equals(Object obj) {
    if (obj instanceof Boolean) {
      return amtrue == (Boolean) obj;
    }
    return obj instanceof Bool && amtrue == ((Bool) obj).amtrue;
  }

  /**
   * @return false is less than true
   */
  public int compareTo(Object obj) {//override image compare
    if (obj instanceof Bool) {
      return asInt() - ((Bool) obj).asInt();//+_+
    }
    return super.compareTo(obj);//fall back to string compare...
  }

  public static int asInt(boolean b) {
    return b ? 1 : 0;
  }

  public static int stuffMask(int pattern, int bitnum, boolean value) {
    if (value) {
      return pattern | (1 << bitnum);
    } else {
      return pattern & ~(1 << bitnum);
    }
  }

  public static long stuffMask(long pattern, int bitnum, boolean value) {
    if (value) {
      return pattern | (1 << bitnum);
    } else {
      return pattern & ~(1 << bitnum);
    }
  }

  public static boolean bitpick(int pattern, int bitnum) {
    return (pattern & (1 << bitnum)) != 0;
  }

  public static boolean bitpick(long pattern, int bitnum) {
    return (pattern & (1L << bitnum)) != 0;
  }

  ////////////////////////////
// a string as a sparse array with index encoded as a char
  public static boolean flagPresent(int ch, String flagset) {
    return StringX.NonTrivial(flagset) && flagset.indexOf(ch) >= 0;
  }

  public static boolean flagPresent(char ch, String flagset) {
    return flagPresent((int) ch, flagset);
  }

  public static boolean flagPresent(String s, String flagset) {
    return StringX.NonTrivial(s) && flagPresent(s.charAt(0), flagset);
  }

  public static boolean For(int eye) {
    return eye != 0;
  }

  public static boolean isEven(int eye) {
    return (eye & 1) == 0;
  }

  public static boolean isOdd(int eye) {
    return (eye & 1) != 0;
  }

  public static boolean For(long ell) {
    return ell != 0;
  }

  /**
   * @returns best guess as to whether the @param trueorfalse means
   */
  public static boolean For(String trueorfalse) {
    try {
      if (StringX.equalStrings(SHORTTRUE(), trueorfalse)) {
        return true;
      } else if (Boolean.valueOf(trueorfalse)) {
        return true;
      } else if (StringX.equalStrings("t", trueorfalse)) { // the database returns "t" for true!
        return true;
      } else //noinspection RedundantIfStatement
        if (StringX.parseInt(trueorfalse) == 1) {
          return true;
        } else {
          return false;
        }
    } catch (Exception ex) {
      return false;
    }
  }

  public static boolean[] MapFromLong(long ell) {
    boolean[] map = new boolean[64];
    int i = 0;
    for (long bitp = Long.MIN_VALUE; i < 64; bitp >>>= 1) {
      map[i++] = (ell & bitp) != 0;
    }
    return map;
  }

  private static String SHORTTRUE() {
    return "Y";
  }

  private static String SHORTFALSE() {
    return "N";
  }

  public static String toString(boolean isTrue) {
    return isTrue ? TRUESTRING : FALSESTRING;
  }

  public static String TRUE() {
    return TRUESTR;
  }

  public static String FALSE() {
    return FALSESTR;
  }

  /**
   * pack booleans into a 64 bit integer
   */
  public static long LongFromMap(boolean[] map) {
    long ell = 0;
    int i = 0;
    for (long bitp = Long.MIN_VALUE; i < 64; bitp >>>= 1) {
      if (map[i++]) {
        ell |= bitp;
      }
    }
    return ell;
  }

  public static int signum(boolean positive) {
    return positive ? 1 : -1;
  }

  public static char signChar(boolean positive) {
    return positive ? '+' : '-';
  }

  public static char dash(boolean dash) {
    return dash ? '-' : ' ';
  }

}

