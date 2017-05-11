package pers.hal42.timer;

import pers.hal42.lang.DateX;
import pers.hal42.lang.Monitor;
import pers.hal42.lang.StringX;
import pers.hal42.math.IntegralPower;
import pers.hal42.math.MathX;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * todo:? Use a monitor for setting/getting? seems to be done
 * todo:x <strike>Merge Ticks.java into this class? NO. ticks are intervals more often than absolute.</strike>
 */

public class UTC implements Comparable {
  /**
   * h_uman r_eadable t_ime f_ormat ==
   */
  private static final LocalTimeFormat hrtf= LocalTimeFormat.Utc();//"yyyyMMddHHmmssSSS"
  private static final Monitor formatting = new Monitor("UTCFormatter");

  public long utc=0;
  /**
   * non null but invalid time.
   */
  public UTC() {
  //make an invalid time
  }
  /**
   * @return new timevalue from absolute millis
   */
  public static UTC New(long time){
    UTC newone=new UTC();
    newone.setto(time);
    return newone;
  }
  /**
   * @return new timevalue from value printed by this classes's toString()
   */
  public static UTC New(String image){
    UTC newone=new UTC();
    newone.setto(image);
    return newone;
  }
  /**
   * @return whether value is not ridiculously trivial
   */
  public boolean isValid(){
    return utc>0;
  }
  /**
   * @return whether @param probate is realistic value.
   */
  public static boolean isValid(UTC probate){
    return probate !=null && probate.isValid();
  }
  /**
   * @return int for ascending sort
   */
  public int compareTo(Object o){
//    System.out.println("comparing " + this + " to " + o);
    if((o != null) && (o instanceof UTC)){
      return MathX.signum(utc-((UTC)o).utc);//or could >>>32.
    }
    return (1-0);//+_+ could throw cast exception.
  }
//////////////////////
// all comparables should have these functions!
  public boolean before(UTC ref){
    return compareTo(ref)<0;
  }
  public boolean after(UTC ref){
    return compareTo(ref)>0;
  }
  public boolean sameas(UTC ref){//'equals' already taken
    return compareTo(ref)==0;
  }
  //end comparable service utilities
  /////////////////
/**
 * @return java version of time
 */
  public long getTime(){
    return utc;
  }
  /**
   * @param howmany decimal digits to return
   * @param lsbs number of decimal digits to discard
   * @return decimal digits as integer
   */
  public long getDigits(int howmany,int lsbs){
    int divider= IntegralPower.raise(10,lsbs).power;
    if(divider<0){
      divider=1;
    }
    int modulus= IntegralPower.raise(10,howmany).power;
    return (utc/divider) % modulus;
  }
  /**
   * @return amount to give to deSKewed such that utc.deSkewed(utc.skew(refclock))== refclock.
   */
  public long skew(UTC refclock){
    return utc-refclock.utc;
  }
  /**
   * @return new UTC which is 'this' corrected for skew.
   * only used on client! Don't think server should use it.
   */
  public UTC deSkewed(long skew){
    return new UTC().setto(utc+skew);
  }

  public String toString(int len){
    return toString(0,len);
  }

  public String toString(int skip,int end){
    return StringX.subString(toString(),skip,end);
  }

  public String toString(){
    try {
      formatting.getMonitor();
      return hrtf.format(new Date(utc));
    }
    catch (Exception e) {
      return "";
    }
    finally {
      formatting.freeMonitor();
    }
  }

  public UTC setto(String dbvalue){
    try {
      formatting.getMonitor();
      utc = hrtf.parse(dbvalue).getTime();
    } finally {
      formatting.freeMonitor();
      return this;
    }
  }

  public UTC setto(long time){
    utc=time;
    return this;
  }

  public UTC setto(Date time){
    return setto(time.getTime());
  }
/**
 * this UTC gets @param rhs's time value, but only the time value-not the formatting
 */
  public UTC setto(UTC rhs){
    return setto(rhs.getTime());
  }
/**
 * return a new UTC initialized with current time
 */
  public static UTC Now(){
    return new UTC().setto(DateX.utcNow());
  }

  // for the database, we need to be able to store time in an integer
  // since an integer is signed in java, we need to fit our time in 2147483647.
  // since there are about 86400 seconds in a day, that means we can fit 24855 days in that number
  // Since there are roughly 365 days in a year, that allows us 68 years.
  // Since genesis of computers is roughly calculated at 19700101
  // if we use that, this scheme is good until 2038
  public static UTC fromSeconds(int seconds) {
    return UTC.New(secondsToMillis(seconds));
  }
  public static int toSeconds(UTC when) {
    return (int)(when.getTime()/1000L);
  }
  public static long secondsToMillis(int seconds) {
    return 1000L * (long)seconds;
  }
  public final int toSeconds() {
    return toSeconds(this);
  }
  public final UTC settoSeconds(int seconds) {
    return setto(secondsToMillis(seconds));
  }

/**
 * modify this to be after @param other
 * @return this
 */
  public UTC ensureAfter(UTC other){
    //if reboot caused the clock to roll back...do SOMETHING:
    if(utc<other.utc){
      utc=other.utc+1;
    }
    return this;
  }

  public boolean changeByDays(int days) {
    UTC utc = ChangeByDays(this, days);
    if(utc == null) {
      return false;
    } else {
      setto(utc.getTime());
      return true;
    }
  }

  public static long Elapsed(UTC first, UTC second){
    return second.utc-first.utc;
  }

  public static UTC ChangeByDays(UTC startdate, int days) { // can be positive or negative
    if(startdate == null) {
      return null;
    }
    Date date = ChangeByDays(new Date(startdate.getTime()), days);
    if(date == null) {
      return null;
    }
    return UTC.New(date.getTime());
  }

  public static Date ChangeByDays(Date startdate, int days) { // can be positive or negative
    if(startdate == null) {
      return null;
    }
    // calculate the end date (start of next day)
    // +++ copy this stuff into and use it in the Search screen!
    GregorianCalendar zoned = (GregorianCalendar)Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    zoned.setTimeInMillis(startdate.getTime());
    zoned.add(GregorianCalendar.DATE, days);
    return zoned.getTime();
  }

}

