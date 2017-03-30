package pers.hal42.lang;


/** Much of what is in here seems to have been later broken out to seperate classes. */

import pers.hal42.logging.ErrorLogStream;
import pers.hal42.timer.LocalTimeFormat;
import pers.hal42.timer.Ticks;
import pers.hal42.util.Executor;

import java.io.*;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Vector;

import static pers.hal42.lang.StringX.parseLong;
import static pers.hal42.stream.IOX.makeWritable;

//import pers.hal42.util.LocalTimeFormat;
//import pers.hal42.util.Ticks;

public class Safe {
  // not a good idea to have a dbg in here --- see preLoadClass
  public static final int INVALIDINDEX=-1;
  public static final int INVALIDINTEGER=-1; //are likely to change this one

//extract a signed number from set of bits within an integer.
  public static int getSignedField(int datum,int start,int end){
    return (datum<<(31-start)) & ~((1<<end)-1);
  }


  public static String ObjectInfo(Object obj){
    return obj==null?" null!":" type: "+obj.getClass().getName();//+" "+obj.toString();
  }

  /** if 1st object is null return second else return it. Kotlin has an operator for this.*/
  public static <T> T deNull(T nullish,T def){
    return (nullish==null)?nullish:def;
  }

//  public static File [] listFiles(File dir){
//    File [] list=dir.listFiles();
//    return list!=null ? list: new File[0];
//  }
//
//  public static File [] listFiles(File dir, FileFilter filter){
//    File [] list=dir.listFiles(filter);
//    return list!=null ? list: new File[0];
//  }

///**
// * quickie DOSlike file attribute control
// * @return true if file was made read/write.
// * just CREATING a FilePermission object modifies the underlying file,
// * that is really bogus syntax. Need to wrap all of that in a FileAttr class.
// */
//
//  public static boolean makeWritable(File f){
//    try {
//      FilePermission attr=new FilePermission(f.getAbsolutePath(),"read,write,delete");
//      return true;
//    } catch(Exception oops){
//      return false; //errors get us here
//    }
//  }
///**
// * @see makeWritable for bitches
// * @return true if file was made readonly.
// */
//  public static boolean makeReadonly(File f){
//    try {
//      FilePermission attr=new FilePermission(f.getAbsolutePath(),"read");
//      return true;
//    } catch(Exception oops){
//      return false; //errors get us here
//    }
//  }

/**
 * returns true is something actively was deleted.
 */
  public static boolean deleteFile(File f){
    try {
      return f!=null && f.exists()&& makeWritable(f) && f.delete();
    } catch(Exception oops){
      return false; //errors get us here
    }
  }

  /**
   * @return true if stream closes Ok, or didn't need to.
   */
  public static boolean Close(OutputStream fos){
    if(fos != null) {
      try {
        fos.flush(); //to make this like C
        fos.close();
      } catch (Exception tfos) {
        pers.hal42.logging.ErrorLogStream.Global().Caught(tfos);
        return false;
      }
    }
    return true;
  }

  /**
   * @return true if stream closes Ok, or didn't need to.
   */
  public static final boolean Close(InputStream fos){
    if(fos != null) {
      try {
        fos.close();
      } catch (Exception tfos) {
        return false;
      }
    }
    return true;
  }

  /**
   *
   */
  public static final int packNibbles(int high,int low){
    return ((high&15)<<4) + (low&15);
  }

///**
// * create new array inserting byte @param toinsert into @param src byte array at @param offset location
// */
//  public static byte[] insert(byte [] src, int toinsert,int offset){
//    byte []target;
//    if(src!=null){
//      if(offset<=src.length){
//        target=new byte[src.length+1];
//        System.arraycopy(src,0,target,0,offset);
//        System.arraycopy(src,offset,target,offset+1,src.length-offset);
//      } else {//stretch until new offset is a legal position.
//        target=new byte[offset+1];
//        System.arraycopy(src,0,target,0,src.length);
//        //new byte[] will have filled the rest with 0.
//      }
//      target[offset]=(byte)toinsert;
//    } else {
//      target= new byte[1];
//      target[0]=(byte)toinsert;
//    }
//    return target;
//  }
//
//  public static  byte[] insert(byte [] src, int toinsert){
//    return insert(src, toinsert,0);
//  }

//  public static  long utcNow(){
//    return System.currentTimeMillis();//+_+ wrapped to simplify use analysis
//  }

//  public static  long fileSize(String filename){
//    try {
//      return (new File(filename)).length();
//    } catch(Exception anything){
//      return -1;
//    }
//  }
//
//  public static long fileModTicks(String filename){
//    try {
//      return (new File(filename)).lastModified();
//    } catch(Exception anything){
//      return -1;
//    }
//  }
//
//  public static Date fileModTime(String filename){
//    return new Date(fileModTicks(filename));
//  }

//  /**
//   * returns true if dir now exists
//   */
//  public static boolean createDir(String filename) {
//    return createDir(new File(filename));
//  }
//  public static boolean createDir(File file) {
//    if(!file.exists()) {
//      return file.mkdir();
//    }
//    return true;
//  }

//  public static Date Now(){// got tired of looking this up.
//    return new Date();//default Date constructor returns "now"
//  }

  public static  String fromStream(ByteArrayInputStream bais,int len){
    byte [] chunk=new byte[len];
    bais.read(chunk,0,len);
    String s=new String(chunk);
    return s;
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
 * debug was removed from the following function as it is called during the initialization
 * of the classes need by the debug stuff. Any debug will have to be raw stdout debugging.
 */
  public static final boolean preloadClass(String className, boolean loadObject) {
    boolean ret = false;
    try {
      Class c = Class.forName(className);
      if(loadObject) {
        c.newInstance(); // some drivers don't load completely until you do this
      }
      ret = true;
    } catch (Exception e) {
      // +++ bitch?
    } finally {
      return ret;
    }
  }

  public static final boolean preloadClass(String className) {
    return preloadClass(className, false);
  }

  public static final Object loadClass(String className) {
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

  public static final int lengthOf(String s){
    return (s!=null)?s.length(): -1;
  }

  // some byte stuff

  public static byte [] newBytes(int length, byte filler) {
    byte [] bytes = new byte[length];
    return fillBytes(bytes, filler);
  }

  // great for erasing passwords
  public static byte [] fillBytes(byte [] bytes, byte filler) {
    for(int i = bytes.length; i-->0;) {
      bytes[i] = filler;
    }
    return bytes;
  }
//
//  public static final byte [] subString(byte [] s,int start,int end){
//    int length;
//    if(s!=null && start>=0 && end>start && start<(length=s.length)){
//      if(end>start+length){
//        end=length;
//      }
//      length=end-start;
//      if(length>0){
//        byte [] sub=new byte[length];
//        System.arraycopy(s,start,sub,0,length);
//        return sub;
//      }
//    }
//    return new byte[0];
//  }

//  /**
//   * I believe this either prepends or appends characters to extend a string to length
//   */
//  public static  String fill(String source, char filler, int length, boolean left) {
//    source.trim();
//    if(length == source.length()) {
//      return source;
//    }
//    int more = length - source.length();
//    if(more < 1) {
//      return source.substring(0, length); // +++ check
//    }
//    StringBuffer str = new StringBuffer(source);
//    if(left) {
//      for(int i = more; i-->0;) {
//        str.insert(0, filler);
//      }
//    } else {
//      for(int i = more; i-->0;) {
//        str.append(filler);
//      }
//    }
//    return str.toString();
//  }


//  /** java.lang.Long.parseLong throws spurious exceptions,
//  *  and doesn't accept unsigned hex numbers that are otherwise acceptible to java.
//  */
//  public static long parseLong(String s,int radix){
//    long acc=0;
//    boolean hadSign=false;
//    if(NonTrivial(s)){
//      s=s.trim();
//      int numchars=lengthOf(s);
//
//      if( numchars<=0 || radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
//        return 0;
//      }
//
//      int nextChar=0;
//      if(s.charAt(nextChar)=='-'){
//        ++nextChar;
//        hadSign=true;
//      }
//
//      while(nextChar<numchars){
//        int digit = Character.digit(s.charAt(nextChar++),radix);
//        if(digit<0){
//          break; //give em what we got so far
//        }
//        acc*=radix;
//        acc+=digit;
//        /* could throw overflow:
//        if(acc<0){//marginal overflow)
//          if(!hadSign){
//            hadSign=true;
//            acc=80000...000 - acc;
//          } else {
//            real overflow...
//          }
//        }
//        */
//      }
//      return hadSign? -acc : acc;
//    }
//    return 0;
//  }
//
//  public static final long parseLong(String s){//default radix 10
//    return parseLong(s,10);
//  }

  public static final long littleEndian(byte [] msfirst,int offset,int length){
    long ell=0;
    for(int i=length<8?length:8; i-->0;){
      ell<<=8;
      ell+= (msfirst[offset+i]&255);//
    }
    return ell;
  }

  public static final long bigEndian(byte [] msfirst,int offset,int length){
    long ell=0;
    if(length>8){
      length=8;
    }
    for(int i=0;i<length;i++){
      ell<<=8;
      ell+= (msfirst[offset+i]&255);//
    }
    return ell;
  }

  public static int parseInt(String s,int radix){
    return (int) (parseLong(s,radix) & 0xFFFFFFFF) ;
  }

  public static int parseInt(String s){//default radix 10
    return parseInt(s,10);
  }

  public static String subString(String s,int start,int end){
    int length;
    if(s!=null && start>=0 && end>start && start<(length=s.length())){
      return s.substring(start, Math.min(length, end));
    } else {
      return "";
    }
  }

  public static String subString(StringBuffer s,int start,int end){
    int length;
    if(s!=null && start>=0 && end>start && start<(length=s.length())){
      return s.substring(start, Math.min(length, end));
    } else {
      return "";
    }
  }


  public static final String restOfString(String s,int start){
    if(s!=null && start>=0 && start<s.length()){
      return s.substring(start);
    } else {
      return "";
    }
  }

  public static final String tail(String s,int length){//need an Fstring variant...
    int start=s.length()-length;
    return start>0?restOfString(s,start):s;
  }

  public static final String trim(String dirty){
    return trim(dirty,true,true);
  }

  public static final String trim(String dirty,boolean leading,boolean trailing){
    if(NonTrivial(dirty)){
      int start=0;
      int end=dirty.length();
      if(leading){
        while(start<end){
          if(Character.isWhitespace(dirty.charAt(start))){
            ++start;
          } else {
            break;
          }
        }
      }
      if(trailing){
        while(end-->0){
          if(!Character.isWhitespace(dirty.charAt(end))){
            break;
          }
        }
      }
      return Safe.subString(dirty,start,++end);
    }
    return "";
  }

  public static final String proper(String toChange) {
    return Safe.subString(toChange, 0, 1).toUpperCase() + Safe.restOfString(toChange, 1).toLowerCase();
  }

  /**
  * @return an index safe for the second operand of string::subString, if the cutter
  * char is not found then we will cut at the end of the string.
  * @param s is a string that we wish to cut out a piece of
  * @param cutter is the separator character
  */
  public static final int cutPoint(String s,char cutter){
    try {
      int cutat=s.indexOf(cutter);
      return cutat<0?s.length():cutat;
    } catch(Exception any){
      return 0;
    }
  }

  public static final StringBuffer delete(StringBuffer sb,int start,int end){
    if(sb!=null&&start>=0&&start<=end){
      sb.delete(start,end);
    }
    return sb;
  }

  /** simple minded string parser
  *  @param parsee words get removed from the front of this
  *  @return the next contiguous non-white character grouping.
  */
  public static final String cutWord(StringBuffer parsee){
    String retval="";//so that we can easily read past the end without getting nulls
    if(NonTrivial(parsee)){
      int start=0;
      int end=parsee.length();
      while(start<end){
        if(Character.isWhitespace(parsee.charAt(start))){
          ++start;
        } else {
          break;
        }
      }
      int cut=start+1;
      while(cut<end){
        if(!Character.isWhitespace(parsee.charAt(cut))){
          ++cut;
        } else {
          break;
        }
      }
      retval=Safe.subString(parsee,start,cut);
      Safe.delete(parsee,0,cut);//removes initial separator too
    }
    return retval;
  }

  ///////////////////////////////////////////////
  public static  boolean NonTrivial(String s){
    return s!=null && s.length()>0;
  }

   public static  boolean hasSubstance(String s){
    return s!=null && s.trim().length()>0;
  }

  public static  boolean NonTrivial(Date d){
    return d!=null && d.getTime()!=0;
  }

  public static  boolean NonTrivial(StringBuffer sb){
    return sb!=null && NonTrivial((String)sb.toString());
  }

  public static  boolean NonTrivial(byte []ba){//oh for templates ....
    return ba!=null && ba.length>0;
  }

  public static  boolean NonTrivial(Vector v){
    return v!=null && v.size()>0;
  }

  public static  boolean NonTrivial(Object o){
    if(o instanceof String){
      return NonTrivial((String)o);
    }
    if(o instanceof StringBuffer){
      return NonTrivial((StringBuffer)o);
    }
    if(o instanceof Date){
      return NonTrivial((Date)o);
    }
    if(o instanceof byte []){
      return NonTrivial((byte [])o);
    }
    if(o instanceof Vector){
      return NonTrivial((Vector)o);
    }
    return o!=null;
  }

  public static  String TrivialDefault(Object o) {
    return TrivialDefault(o, null);
  }

  public static  String TrivialDefault(Object o, String def) {
    def = TrivialDefault(def, "");
    return NonTrivial(o) ? o.toString() : def;
  }

//  public static boolean equalStrings(String one, String two){
//    return equalStrings(one, two, false);
//  }

//  public static boolean equalStrings(String one, String two, boolean ignoreCase){
//    if(NonTrivial(one)){
//      if(NonTrivial(two)){
//        return ignoreCase ? one.equalsIgnoreCase(two) : one.equals(two);
//      } else {
//        return false;
//      }
//    } else {
//      return !NonTrivial(two);//both trivial is a match
//    }
//  }
//
//  /**
//   * trivial strings are sorted to start of list?
//   */
//  public static int compareStrings(String one, String two){
//    if(NonTrivial(one)){
//      if(NonTrivial(two)){
//        return one.compareTo(two);
//      } else {
//        return -1;
//      }
//    } else {
//      return +1;
//    }
//  }
//

//
//  //////////
//  public static  boolean FileExists(File f){
//    return f!=null && f.exists();
//  }
//
//  /** enhanced TrivialDefault
//  * @return a string that is not zero length
//  * @param s proposed string, usually from an untrusted variable
//  * @param defaultReplacement fallback string, usually a constant
//  */
//  public static String OnTrivial(String s, String defaultReplacement) {
//    //this would of course be an infinite loop if the constant below were changed to be trivial...
//    return NonTrivial(s) ? s : OnTrivial(defaultReplacement," ");
//  }
//
//  public static String unNull(String s) {
//    return TrivialDefault(s, ""); // kinda redundant
//  }
//
//  public static String TrivialDefault(String s, String defaultReplacement) {
//    return NonTrivial(s) ? s : defaultReplacement;
//  }
//
//  public static String clipEOL(String s, String EOL) {
//    //if final eol is undesirable clip it off.
//    String returner = unNull(s);
//    if(returner.length() > 0) {
//      int index = returner.lastIndexOf(EOL);
//      if(index == (returner.length()-EOL.length())) {
//        returner = returner.substring(0, index);
//      }
//    }
//    return returner;
//  }
//
//  public final static String removeAll(String badchars,String source){
//    if(!NonTrivial(badchars)) {
//      return source;
//    }
//    StringBuffer copy=new StringBuffer(source.length());
//    for(int i=0;i<source.length();i++){
//      char c=source.charAt(i);
//      if(badchars.indexOf(c)<0){
//        copy.append(c);
//      }
//    }
//    return copy.toString();
//  }
//
//  protected static final String [][] replacers = {
//      { "\\\\", "\\" },
//        { "\\b", "\b" },
//        { "\\t", "\t" },
//        { "\\n", "\n" },
//        { "\\f", "\f" },
//        { "\\\"", "\"" },
//        { "\\r", "\r" },
//      { "\\'", "\'" },
//  };
//
//  public static final String unescapeAll(String source) {
//    if(NonTrivial(source)) {
//      for(int i = replacers.length; i-->0;) {
//        source = Safe.replace(source, replacers[i][0], replacers[i][1]); // not really efficient, but a kludge anyway ---
//      }
//    }
//    return source;
//  }

  // --- extremely inefficient.  improve!
  //alh has a state machine in some old C code, til then this is nicely compact source.
  public static String replace(String source, String toReplace, String with) {
    return replace(new StringBuffer(TrivialDefault(source, "")), TrivialDefault(toReplace, ""), TrivialDefault(with, ""));
  }

  public static String replace(StringBuffer source, String toReplace, String with) {
    boolean recurse = false;  //todo: elevate to parameter
    if(source == null) {
      return null;
    }
    if(!NonTrivial(toReplace) || toReplace.equals(with)) {
      return source.toString();
    }
    with = TrivialDefault(with, "");
    int lookLen = toReplace.length();
    int withLen = with.length();
    int searchFrom = 0;
    int foundAt = 0;
    while((foundAt = source.substring(searchFrom).indexOf(toReplace)) > -1) {
      int reallyAt = foundAt + searchFrom;//foundat is relative to serachFrom
      // +_+ improve this by dealing with cases separately (srcLen == repLen, srcLen < repLen, srcLen > repLen)
//      String old = source.toString();
      source.delete(reallyAt, reallyAt+lookLen);
      source.insert(reallyAt, with);
      searchFrom  = recurse ? 0 : reallyAt + withLen; // would recurse==true go infinite?  maybe
      //would only be infinite if with contains toReplace. would exahust memory if so.
    }
    return source.toString();
  }

//  // +++ why promote to only float, but then promote to double later?
//  public static double ratio(int num, int denom){
//    return (denom==0)?0:((float)num)/((float)denom);//+_+ crude, need to get spectro's spec
//  }
//
//  public static double ratio(long num, long denom){
//    return (denom==0)?0:((float)num)/((float)denom);//+_+ crude, need to get spectro's spec
//  }
////
//  public static final long percent(long dividend, long divisor) {
//    return Math.round(Safe.ratio(dividend * 100, divisor));
//  }
//

//  /////////////////////////////////////////////////////////////////////////////
//  // date / time stuff
//  static final LocalTimeFormat stamper= LocalTimeFormat.Utc("yyyyMMdd.HHmmss.SSS");
//
//  public static String timeStampNow() {
//    return timeStamp(Now());
//  }
//
//  private static final Monitor timeMon = new Monitor("Safe.timeStamp");
//  public static  String timeStamp(Date today) {
//    String ret = "";
//    try {
//      timeMon.getMonitor();
//      ret = stamper.format(today);
//    } catch (Exception e) {
//      // +++ deal with this
//      // --- CANNOT put any ErrorLogStream stuff here since this is used in ErrorLogStream.
//    } finally {
//      timeMon.freeMonitor();
//    }
//    return ret;
//  }
//
//  public static final String timeStamp(long millis) {
//    return timeStamp(new Date(millis));
//  }

  /**
  * converts raw milliseconds into HH:mm:ss
  * this is special and only seems to work in a certain case
  * Don't use SimpleDateFormat, as this function is using a dater DIFFERENCE, not an absolute Date
  */
  public static final String millisToTime(long millis) {
    long secondsDiv = Ticks.forSeconds(1);
    long minutesDiv = secondsDiv * 60;
    long hoursDiv   = minutesDiv * 60;
    long daysDiv    = hoursDiv * 24;

    long days = millis / daysDiv;
    millis = millis % daysDiv; // get the remainder
    long hours = millis / hoursDiv;
    millis = millis % hoursDiv; // get the remainder
    long minutes = millis / minutesDiv;
    millis = millis % minutesDiv; // get the remainder
    long seconds = millis / secondsDiv;
    millis = millis % secondsDiv; // get the remainder

    return  ((days > 0) ? ("" + days + " ") : "") +
      StringX.twoDigitFixed(hours) + ":" +
      StringX.twoDigitFixed(minutes) + ":" +
      StringX.twoDigitFixed(seconds);
  }

  static final LocalTimeFormat LinuxDateCommand=LocalTimeFormat.Utc("MMddHHmmyyyy.ss");

  public static void setSystemClock(long millsfromepoch){ //exec something to set the system clock
    String forlinuxdateprogram=LinuxDateCommand.format(new Date(millsfromepoch));
    ErrorLogStream.Global().ERROR("setting time to:"+forlinuxdateprogram);
    String progname= pers.hal42.lang.OS.isUnish()?"setClock ":"setClock.bat ";
    // Executor.runProcess("date -s -u "+busybox.format(now),"fixing clock",0,0,null,false);
    Executor.ezExec(progname+forlinuxdateprogram,0);
  }

///////////////////////////////

////////////////////
//  create a separate class for this ???
  private static final DecimalFormat secsNMillis = new DecimalFormat("#########0.000");
  private static final Monitor secsNMillisMonitor = new Monitor("secsNMillis");
  private static final StringBuffer sbsnm = new StringBuffer();
  public static final String millisToSecsPlus(long millis) {
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

  public static final FileOutputStream fileOutputStream(String filename) {
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(filename);
    } catch (Exception e) {
      // +++ bitch
    }
    return fos;
  }

  /**
   * Creates a unique filename given a particular pattern
   *
   * @param pathedPrefix - the path (just directory) where the file will be located + the first part of the filename
   * @param suffix - the last part of the filename
   *
   * ie: filename = path + datetimestamp + suffix
   *
   * eg: createUniqueFilename("c:\temp", "myfile", ".txt") = c:\temp\myfile987654321.txt"
   */
  public static final String createUniqueFilename(String pathedPrefix, String suffix) {
    File file = null;
    String filename = null;
    int attempts = 0;
    try {
      do {
        filename = pathedPrefix + timeStampNow() + suffix;
        file = new File(filename);
      } while (((file != null) || file.exists()) && (++attempts < 20));//no infinite loops
    } catch (Exception e) {
      // +++ bitch
    }
    return filename;
  }

  // feel free to optimize this
  private static final double K = 1024;
  private static final double M = K*K;
  private static final double G = M*K;
  private static final double T = G*K;
  public static final String sizeLong(long size) {
    // size is always positive
    double fat = size;
    String ret = "";
    switch(3) {
      case 3:
        if(fat > T) {
          ret += Math.round(fat / T)+" T";
          break;
        }
      case 2:
        if(fat > G) {
          ret += Math.round(fat / G)+" G";
          break;
        }
      case 1:
        if(fat > M) {
          ret += Math.round(fat / M)+" M";
          break;
        }
      case 0:
        if(fat > K) {
          ret += Math.round(fat / K)+" K";
          break;
        }
      default: // leave it as size
        ret += size;
    }
    return ret;
  }

  public static final int diskfree(String moreParams, TextList msgs) {
    String filename = "C:\\CYGWIN\\BIN\\df.exe";//+_+ move to OS specific classes
    int timeout = 5;
    int displayrate = 1;
    if(Safe.fileSize(filename)==0) {
      filename = "df";
      timeout = -1;
      displayrate = -1;
    }
    filename = filename+" -k "+TrivialDefault(moreParams, "");
    int c = Executor.runProcess(filename, "", displayrate /* -1 */, timeout /* was never returning when set to -1 for my machine (not sure why) */, msgs);
    return c;
  }

  public static final void main(String [] args) {
    TextList msgs = new TextList();
    System.out.println("diskfree = " + diskfree("", msgs));
    System.out.println(msgs.asParagraph());
  }

}

