package pers.hal42.lang;

import pers.hal42.text.Formatter;
import pers.hal42.timer.LocalTimeFormat;
import pers.hal42.timer.Ticks;
import pers.hal42.timer.UTC;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.util.Date;

public class DateX {
  private DateX() {
    // I exist for static reasons
  }

  /////////////////////////////////////////////////////////////////////////////
  // date / time stuff
  static final LocalTimeFormat stamper=LocalTimeFormat.Utc("yyyyMMdd.HHmmss.SSS");
  static final LocalTimeFormat yearlessstamper=LocalTimeFormat.Utc("MMdd.HHmmss.SSS");

  public static String timeStampNow() {
    return timeStamp(Now());
  }

  public static String timeStampNowYearless() {
    return timeStampYearless(Now());
  }

  private static final Monitor timeMon = new Monitor("DateX.timeStamp");
  private static String timeStamp(Date today) {
    String ret = "";
    try {
      timeMon.getMonitor();
      ret = stamper.format(today);
    } catch (Exception e) {
      // +++ deal with this
      // --- CANNOT put any ErrorLogStream stuff here since this is used in ErrorLogStream.
    } finally {
      timeMon.freeMonitor();
    }
    return ret;
  }

  private static final Monitor yearlesstimeMon = new Monitor("DateX.timeStampYearless");
  public static String timeStampYearless(Date today) {
    String ret = "";
    try {
      yearlesstimeMon.getMonitor();
      ret = yearlessstamper.format(today);
    } catch (Exception e) {
      // +++ deal with this
      // --- CANNOT put any ErrorLogStream stuff here since this is used in ErrorLogStream.
    } finally {
      yearlesstimeMon.freeMonitor();
    }
    return ret;
  }

  public static String timeStamp(long millis) {
    return timeStamp(new Date(millis));
  }

  public static String timeStamp(UTC utc) {
    return timeStamp(utc.getTime());
  }

  /**
  * converts raw milliseconds into HH:mm:ss
  * this is special and only seems to work in a certain case (what case is that?)
  * Don't use SimpleDateFormat, as this function is using a dater DIFFERENCE, not an absolute Date
  */
  public static String millisToTime(long millis) {
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
    //millis = millis % secondsDiv; // get the remainder

    return  ((days > 0) ? ("" + days + " ") : "") +
      Formatter.twoDigitFixed(hours) + ":" +
      Formatter.twoDigitFixed(minutes) + ":" +
      Formatter.twoDigitFixed(seconds);
  }

  public static boolean NonTrivial(Date d){
    return d!=null && d.getTime()!=0;
  }

///////////////////////////////
//  create a separate class for this ???
  private static final DecimalFormat secsNMillis = new DecimalFormat("#########0.000");
  private static final Monitor secsNMillisMonitor = new Monitor("secsNMillis");
  private static final StringBuffer sbsnm = new StringBuffer();
  public static String millisToSecsPlus(long millis) {
    String retval = "";
    try {
      secsNMillisMonitor.getMonitor();
      sbsnm.setLength(0);
      double secs = 1.0 * millis / Ticks.perSecond;
      secsNMillis.format(secs, sbsnm, new FieldPosition(NumberFormat.INTEGER_FIELD));
      retval = String.valueOf(sbsnm);
    } catch (Exception e) {
    } finally {
      secsNMillisMonitor.freeMonitor();
      return retval;
    }
  }

  /**
   * store the amount to add to the clock to get outside world's time.
   */
  private static long clockSkew =0;
  private static long ClockSkew(){
    return clockSkew;
  }

  /**
   * @return our best guess as to what the real-world time is
   */
  public static UTC UniversalTime(){
    return UTC.Now().deSkewed(clockSkew);//+_+ makes and wastes objects.
  }

  /**
   * @param utcd is present universal time
   */
  public static void UniversalTimeIs(UTC utcd){
    clockSkew= utcd.skew(UTC.Now());
  }

  public static long utcNow(){
    return System.currentTimeMillis();//+_+ wrapped to simplify use analysis
  }

  public static Date Now(){// got tired of looking this up.
    return new Date();//default Date constructor returns "now"
  }

}
