package pers.hal42.timer;

import pers.hal42.lang.StringX;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.text.TextList;
import pers.hal42.transport.EasyCursor;
import pers.hal42.transport.isEasy;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class LocalTimeFormat implements isEasy {
  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(LocalTimeFormat.class);

  private SimpleDateFormat wrapped;
  private String internalFormat;//cached for making new ones

  public String getFormat(){
    return internalFormat;
  }

  public TimeZone getZone(){
    return wrapped.getTimeZone();
  }
  /**
   * for when construction and use are widely separated.
   */
  private LocalTimeFormat setFormat(String s){
    wrapped.applyPattern(internalFormat=s);
    return this;
  }

  public static Date dateFromInts(int year, int month, int day) {
    Calendar cal = Calendar.getInstance();
    cal.set(year, month, day);
    return cal.getTime();
  }

  private void setZone(TimeZone fromServer){
//can't debug this function, used by a static!!!    dbg.ERROR("Setting tz:"+fromServer);
    wrapped= new SimpleDateFormat();
    wrapped.set2DigitYearStart(dateFromInts(2000,1,1));
    wrapped.setTimeZone(fromServer);
  }

  protected LocalTimeFormat(TimeZone fromServer,String format){
    setZone(fromServer);
    //+_+ pathological default, but stops exceptions
    setFormat(StringX.OnTrivial(format,"z"));
  }

  public static LocalTimeFormat copy(LocalTimeFormat rhs){
    return new LocalTimeFormat(rhs.wrapped.getTimeZone(),rhs.internalFormat);
  }

  public static LocalTimeFormat cousin(LocalTimeFormat rhs, String otherformat){
    return new LocalTimeFormat(rhs.wrapped.getTimeZone(),otherformat);
  }

  public LocalTimeFormat Clone(){
    return copy(this);
  }

  public void save(EasyCursor ezp){
    ezp.setString("timezone",this.getZone().getDisplayName());
    ezp.setString("format",this.getFormat());
  }

  public void load(EasyCursor ezp){
    //order matters here!
    setZone(TimeZone.getTimeZone(ezp.getString("timezone")));
    setFormat(ezp.getString("format"));
  }

  /**
   * quite often the format is constant for the life of the object,
   * and known at the point of creations
   */
  public static LocalTimeFormat New(TimeZone fromServer, String format) {
    return new LocalTimeFormat(fromServer,format);
  }

  public static LocalTimeFormat New(String tz, String format) {
    return New(TimeZone.getTimeZone(tz),format);
  }

  public static LocalTimeFormat New(TimeZone fromServer) {
    return New(fromServer,"");
  }

  public static LocalTimeFormat Utc(String format) {
    return New(TimeZone.getTimeZone("UTC"),format);
  }

  public static final String DESCENDINGTIMEFORMAT = "yyyyMMddHHmmssSSS";

  public static LocalTimeFormat Utc() {
    return Utc(DESCENDINGTIMEFORMAT);//decimalized but covers full span
  }

  public String format(Date d){//
    return wrapped.format(d);
  }

  public String format(UTC utc){//
    return utc!=null? wrapped.format(new Date(utc.utc)): "null";
  }

  public Date parse(String image, Date onError){
    try {
      return wrapped.parse(image);
    } catch (java.text.ParseException arf){
      dbg.Caught(arf);
      return onError;
    }
  }

  public Date parseFill(String image, Date onError){
// hack --- pad with zeroes if image is too short
    if(image.length() < internalFormat.length()) {
      image = StringX.fill(image, '0', internalFormat.length(), false);
    }
    return parse(image,onError);
  }

  public static final Date onError=new Date();//+_+ need a better fallback
  public Date parse(String image){
    return parseFill(image,onError);
  }

  public UTC parseUtc(String image) {
    return UTC.New(parse(image).getTime());
  }

  public UTC parseUtcNullError(String image) {
    return UTC.New(parseFill(image, null).getTime());
  }

  public String toString() {
    return "zone="+getZone()+", format="+getFormat();
  }

   /**
  * Year:         yyyy, yy (<--lower case)
  * Month:        MMM, MM, M
  * Day of week:  EEEE, EE
  * Day of Month: dd, d
  * Hour (1-12):  hh, h
  * Hour (0-23):  HH, H
  * Hour (0-11):  KK, K
  * Hour (1-24):  kk, k
  * Minute:       mm
  * Second:       ss
  * Millisecond:  SSS
  * AM/PM:        a
  * Time zone:    zzzz, zz
  * DOW in Month: F (eg: 3rd Thursday)
  * Day in Year:  DDD, D
  * Week in Year: ww
  * Era:          G (eg: BC/AD)
  */

 public static void main(String [] args) {
   // output the zone names
   String [] zones = TimeZone.getAvailableIDs();
   TextList tl = new TextList(zones);
   System.out.println(tl);
 }

}

