package pers.hal42.lang;


import pers.hal42.logging.ErrorLogStream;
import pers.hal42.timer.LocalTimeFormat;
import pers.hal42.timer.Ticks;
import pers.hal42.util.Executor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Vector;

import static pers.hal42.lang.StringX.parseLong;
import static pers.hal42.stream.IOX.makeWritable;

public class Safe {
  // not a good idea to have a dbg in here --- see preLoadClass
  public static final int INVALIDINDEX = -1;
  public static final int INVALIDINTEGER = -1; //are likely to change this one
  static final LocalTimeFormat LinuxDateCommand = LocalTimeFormat.Utc("MMddHHmmyyyy.ss");
  ////////////////////
//  create a separate class for this ???
  private static final DecimalFormat secsNMillis = new DecimalFormat("#########0.000");
  private static final Monitor secsNMillisMonitor = new Monitor("secsNMillis");
  private static final StringBuffer sbsnm = new StringBuffer();
  // feel free to optimize this
  private static final double K = 1024;
  private static final double M = K * K;
  private static final double G = M * K;
  private static final double T = G * K;

  //extract a signed number from set of bits within an integer.
  public static int getSignedField(int datum, int start, int end) {
    return (datum << (31 - start)) & ~((1 << end) - 1);
  }


  public static String ObjectInfo(Object obj) {
    return obj == null ? " null!" : " type: " + obj.getClass().getName();//+" "+obj.toString();
  }


// NOT IN USE (when it is needed, extend Vector and put this in the extended class)
/**
 * physically reverse the order of the contents of Vector
 * @param v reverses the actual vector content,
 * @return passing through the reference, does NOT create a reversed one
 */
/*
  public static final Vector Reverse(Vector v){
    if(v!=null){
      int s=0;
      int e=v.size();
      while(s<e){
        Object temp=v.elementAt(e);
        v.setElementAt(v.elementAt(s),e);
        v.setElementAt(temp,s);
        ++s;
        --e;
      }
    }
    return v;
  }
*/

  /**
   * if 1st object is null return second else return it. Kotlin has an operator for this.
   */
  public static <T> T deNull(T nullish, T def) {
    return (nullish != null) ? nullish : def;
  }

  public static int enumSize(Class<? extends Enum> eclass) {
    return eclass.getEnumConstants().length;
  }

  public static int enumSize(Enum eg) {
    return enumSize(eg.getClass());
  }

  //todo:1 move these to IOx
  public static File[] listFiles(File dir) {
    File[] list = dir.listFiles();
    return list != null ? list : new File[0];
  }

  // some byte stuff

  public static File[] listFiles(File dir, FileFilter filter) {
    File[] list = dir.listFiles(filter);
    return list != null ? list : new File[0];
  }

  /**
   * returns true is something actively was deleted.
   */
  public static boolean deleteFile(File f) {
    try {
      return f != null && f.exists() && makeWritable(f) && f.delete();
    } catch (Exception oops) {
      return false; //errors get us here
    }
  }

  /**
   *
   */
  public static int packNibbles(int high, int low) {
    return ((high & 15) << 4) + (low & 15);
  }

  public static String fromStream(ByteArrayInputStream bais, int len) {
    byte[] chunk = new byte[len];
    bais.read(chunk, 0, len);
    return new String(chunk);
  }

  /**
   * debug was removed from the following function as it is called during the initialization
   * of the classes need by the debug stuff. Any debug will have to be raw stdout debugging.
   */
  public static boolean preloadClass(String className, boolean loadObject) {
    boolean ret = false;
    try {
      Class c = Class.forName(className);
      if (loadObject) {
        c.newInstance(); // some drivers don't load completely until you do this
      }
      ret = true;
    } catch (Exception e) {
      // +++ bitch?
    } finally {
      return ret;
    }
  }

  public static boolean preloadClass(String className) {
    return preloadClass(className, false);
  }

  public static Object loadClass(String className) {
    Object ret = null;
    try {
      Class c = Class.forName(className);
      ret = c.newInstance(); // some drivers don't load completely until you do this
    } catch (Exception e) {
      // +++ bitch ?
    } finally {
      return ret;
    }
  }

  public static int lengthOf(String s) {
    return (s != null) ? s.length() : -1;
  }

  public static byte[] newBytes(int length, byte filler) {
    byte[] bytes = new byte[length];
    return fillBytes(bytes, filler);
  }

  // great for erasing passwords
  public static byte[] fillBytes(byte[] bytes, byte filler) {
    for (int i = bytes.length; i-- > 0; ) {
      bytes[i] = filler;
    }
    return bytes;
  }

  public static long littleEndian(byte[] msfirst, int offset, int length) {
    long ell = 0;
    for (int i = length < 8 ? length : 8; i-- > 0; ) {
      ell <<= 8;
      ell += (msfirst[offset + i] & 255);//
    }
    return ell;
  }

  public static long bigEndian(byte[] msfirst, int offset, int length) {
    long ell = 0;
    if (length > 8) {
      length = 8;
    }
    for (int i = 0; i < length; i++) {
      ell <<= 8;
      ell += (msfirst[offset + i] & 255);//
    }
    return ell;
  }

  public static int parseInt(String s, int radix) {
    return (int) (parseLong(s, radix) & 0xFFFFFFFF);
  }

  public static int parseInt(String s) {//default radix 10
    return parseInt(s, 10);
  }

  public static String subString(String s, int start, int end) {
    int length;
    if (s != null && start >= 0 && end > start && start < (length = s.length())) {
      return s.substring(start, Math.min(length, end));
    } else {
      return "";
    }
  }

  public static String subString(StringBuffer s, int start, int end) {
    int length;
    if (s != null && start >= 0 && end > start && start < (length = s.length())) {
      return s.substring(start, Math.min(length, end));
    } else {
      return "";
    }
  }

  public static String restOfString(String s, int start) {
    if (s != null && start >= 0 && start < s.length()) {
      return s.substring(start);
    } else {
      return "";
    }
  }

  public static String tail(String s, int length) {//need an Fstring variant...
    int start = s.length() - length;
    return start > 0 ? restOfString(s, start) : s;
  }

  public static String trim(String dirty) {
    return trim(dirty, true, true);
  }

  public static String trim(String dirty, boolean leading, boolean trailing) {
    if (NonTrivial(dirty)) {
      int start = 0;
      int end = dirty.length();
      if (leading) {
        while (start < end) {
          if (Character.isWhitespace(dirty.charAt(start))) {
            ++start;
          } else {
            break;
          }
        }
      }
      if (trailing) {
        while (end-- > 0) {
          if (!Character.isWhitespace(dirty.charAt(end))) {
            break;
          }
        }
      }
      return Safe.subString(dirty, start, ++end);
    }
    return "";
  }

  public static String proper(String toChange) {
    return Safe.subString(toChange, 0, 1).toUpperCase() + Safe.restOfString(toChange, 1).toLowerCase();
  }

  /**
   * @param s      is a string that we wish to cut out a piece of
   * @param cutter is the separator character
   * @return an index safe for the second operand of string::subString, if the cutter
   * char is not found then we will cut at the end of the string.
   */
  public static int cutPoint(String s, char cutter) {
    try {
      int cutat = s.indexOf(cutter);
      return cutat < 0 ? s.length() : cutat;
    } catch (Exception any) {
      return 0;
    }
  }

  public static StringBuffer delete(StringBuffer sb, int start, int end) {
    if (sb != null && start >= 0 && start <= end) {
      sb.delete(start, end);
    }
    return sb;
  }

  /**
   * simple minded string parser
   *
   * @param parsee words get removed from the front of this
   * @return the next contiguous non-white character grouping.
   */
  public static String cutWord(StringBuffer parsee) {
    String retval = "";//so that we can easily read past the end without getting nulls
    if (NonTrivial(parsee)) {
      int start = 0;
      int end = parsee.length();
      while (start < end) {
        if (Character.isWhitespace(parsee.charAt(start))) {
          ++start;
        } else {
          break;
        }
      }
      int cut = start + 1;
      while (cut < end) {
        if (!Character.isWhitespace(parsee.charAt(cut))) {
          ++cut;
        } else {
          break;
        }
      }
      retval = Safe.subString(parsee, start, cut);
      Safe.delete(parsee, 0, cut);//removes initial separator too
    }
    return retval;
  }

  ///////////////////////////////////////////////
  public static boolean NonTrivial(String s) {
    return s != null && s.length() > 0;
  }


  public static boolean hasSubstance(String s) {
    return s != null && s.trim().length() > 0;
  }

  public static boolean NonTrivial(Date d) {
    return d != null && d.getTime() != 0;
  }


  public static boolean NonTrivial(StringBuffer sb) {
    return sb != null && NonTrivial(sb.toString());
  }

  public static boolean NonTrivial(byte[] ba) {//oh for templates ....
    return ba != null && ba.length > 0;
  }

///////////////////////////////

  public static boolean NonTrivial(Vector v) {
    return v != null && v.size() > 0;
  }

  public static boolean NonTrivial(Object o) {
    if (o instanceof String) {
      return NonTrivial((String) o);
    }
    if (o instanceof StringBuffer) {
      return NonTrivial((StringBuffer) o);
    }
    if (o instanceof Date) {
      return NonTrivial((Date) o);
    }
    if (o instanceof byte[]) {
      return NonTrivial((byte[]) o);
    }
    if (o instanceof Vector) {
      return NonTrivial((Vector) o);
    }
    return o != null;
  }

  public static String TrivialDefault(Object o) {
    return TrivialDefault(o, "");
  }

  public static String TrivialDefault(Object o, String def) {
    return NonTrivial(o) ? o.toString() : (TrivialDefault(def, ""));
  }

  // --- extremely inefficient.  improve!
  //alh has a state machine in some old C code, til then this is nicely compact source.
  public static String replace(String source, String toReplace, String with) {
    return replace(new StringBuffer(TrivialDefault(source, "")), TrivialDefault(toReplace, ""), TrivialDefault(with, ""));
  }

  public static String replace(StringBuffer source, String toReplace, String with) {
    boolean recurse = false;  //todo: elevate to parameter
    if (source == null) {
      return null;
    }
    if (!NonTrivial(toReplace) || toReplace.equals(with)) {
      return source.toString();
    }
    with = TrivialDefault(with, "");
    int lookLen = toReplace.length();
    int withLen = with.length();
    int searchFrom = 0;
    int foundAt = 0;
    while ((foundAt = source.substring(searchFrom).indexOf(toReplace)) > -1) {
      int reallyAt = foundAt + searchFrom;//foundat is relative to serachFrom
      // +_+ improve this by dealing with cases separately (srcLen == repLen, srcLen < repLen, srcLen > repLen)
//      String old = source.toString();
      source.delete(reallyAt, reallyAt + lookLen);
      source.insert(reallyAt, with);
      searchFrom = recurse ? 0 : reallyAt + withLen; // would recurse==true go infinite?  maybe
      //would only be infinite if with contains toReplace. would exahust memory if so.
    }
    return source.toString();
  }

  public static void setSystemClock(long millsfromepoch) { //exec something to set the system clock
    String forlinuxdateprogram = LinuxDateCommand.format(new Date(millsfromepoch));   //todo:1 move this line into DateX
    ErrorLogStream.Global().ERROR("setting time to:" + forlinuxdateprogram);
    String progname = pers.hal42.lang.OS.isUnish() ? "setClock " : "setClock.bat ";
    // Executor.runProcess("date -s -u "+busybox.format(now),"fixing clock",0,0,null,false);
    Executor.ezExec(progname + forlinuxdateprogram, 0);
  }

  public static String millisToSecsPlus(long millis) {
    String retval = "";
    try {
      secsNMillisMonitor.getMonitor();
      sbsnm.setLength(0);
      double secs = 1.0 * millis / Ticks.perSecond;
      secsNMillis.format(secs, sbsnm, new FieldPosition(NumberFormat.INTEGER_FIELD));
      retval = sbsnm.toString();
    } catch (Exception e) {
    } finally {
      secsNMillisMonitor.freeMonitor();
      return retval;
    }
  }

  public static FileOutputStream fileOutputStream(String filename) {
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(filename);
    } catch (Exception e) {
      // +++ bitch
    }
    return fos;
  }


  public static String sizeLong(long size) {
    // size is always positive
    //noinspection UnnecessaryLocalVariable     but convert to double just once instead for each test.
    double fat = size;
    String ret = "";
    if (fat > T) {
      return ret + Math.round(fat / T) + " T";
    }
    if (fat > G) {
      return ret + Math.round(fat / G) + " G";

    }
    if (fat > M) {
      return ret + Math.round(fat / M) + " M";

    }
    if (fat > K) {
      return ret + Math.round(fat / K) + " K";
    }
    return ret + size;

  }

}

