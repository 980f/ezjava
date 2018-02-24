package pers.hal42.timer;

import org.jetbrains.annotations.NotNull;
import pers.hal42.lang.DateX;
import pers.hal42.lang.Monitor;
import pers.hal42.lang.StringX;
import pers.hal42.math.IntegralPower;

import java.util.Date;

/** simple time class. */

public class UTC implements Comparable<UTC> {
  /**
   * h_uman r_eadable t_ime f_ormat ==
   */
  private static final LocalTimeFormat hrtf = LocalTimeFormat.Utc();//"yyyyMMddHHmmssSSS"
  private static final Monitor formatting = new Monitor("UTCFormatter");
  public long utc = 0;

  /**
   * non null but invalid time.
   */
  public UTC() {
    //make an invalid time
  }

  /**
   * @return new timevalue from absolute millis
   */
  public static UTC New(long time) {
    UTC newone = new UTC();
    newone.setto(time);
    return newone;
  }

  /**
   * @return new timevalue from value printed by this classes's toString()
   */
  public static UTC New(String image) {
    UTC newone = new UTC();
    newone.setto(image);
    return newone;
  }

  /**
   * @return whether @param probate is realistic value.
   */
  public static boolean isValid(UTC probate) {
    return probate != null && probate.isValid();
  }

  /**
   * return a new UTC initialized with current time
   */
  public static UTC Now() {
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

  /**
   * @returns @param when as seconds.
   */
  public static double toSeconds(UTC when) {
    return ((double) when.getTime()) / 1000.0;
  }

  public static long secondsToMillis(int seconds) {
    return 1000L * (long) seconds;
  }

  /**
   * @returns milliseconds that is greater than or equal to @param seconds, even dribbles round up to the next milli
   */
  public static long secondsToMillis(double seconds) {
    return (long) Math.ceil(1000 * seconds);
  }

  public static long Elapsed(UTC first, UTC second) {
    return second.utc - first.utc;
  }
  //end comparable service utilities
  /////////////////

  /**
   * @return int for ascending sort
   */
  public int compareTo(@NotNull UTC o) {
    return Long.compare(utc, o.utc);
  }



  /**
   * @return whether value is not ridiculously trivial
   */
  public boolean isValid() {
    return utc > 0;
  }

  /** make a new UTC that differs from @param startdate by @param days, negative for the past. */
  public static UTC ChangeByDays(UTC startdate, int days) { // can be positive or negative
    if (startdate == null) {
      return null;
    }
    return UTC.New(startdate.utc + days * Ticks.forDays(days));
  }

  //////////////////////
// all comparables should have these functions!
  public boolean before(UTC ref) {
    return compareTo(ref) < 0;
  }

  public boolean after(UTC ref) {
    return compareTo(ref) > 0;
  }

  public boolean sameas(UTC ref) {//'equals' already taken
    return compareTo(ref) == 0;
  }

  /**
   * @return java version of time
   */
  public long getTime() {
    return utc;
  }

  /**
   * @param howmany decimal digits to return
   * @param lsbs    number of decimal digits to discard
   * @return decimal digits as integer
   */
  public long getDigits(int howmany, int lsbs) {
    int divider = IntegralPower.raise(10, lsbs).power;
    if (divider < 0) {
      divider = 1;
    }
    int modulus = IntegralPower.raise(10, howmany).power;
    return (utc / divider) % modulus;
  }

  /**
   * @return amount to give to deSKewed such that utc.deSkewed(utc.skew(refclock))== refclock.
   */
  public long skew(UTC refclock) {
    return utc - refclock.utc;
  }

  /**
   * @return new UTC which is 'this' corrected for skew.
   * only used on client! Don't think server should use it.
   */
  public UTC deSkewed(long skew) {
    return new UTC().setto(utc + skew);
  }

  public String toString(int len) {
    return toString(0, len);
  }

  public String toString(int skip, int end) {
    return StringX.subString(toString(), skip, end);
  }

  public String toString() {
    try (Monitor free = formatting.getMonitor()) {
      return hrtf.format(new Date(utc));
    }
  }

  public UTC setto(String dbvalue) {
    try (Monitor free = formatting.getMonitor()) {
      utc = hrtf.parse(dbvalue).getTime();
      return this;
    }
  }

  public UTC setto(long time) {
    utc = time;
    return this;
  }

  public UTC setto(Date time) {
    return setto(time.getTime());
  }

  /**
   * this UTC gets @param rhs's time value, but only the time value-not the formatting
   */
  public UTC setto(UTC rhs) {
    return setto(rhs.getTime());
  }

  public final double toSeconds() {
    return toSeconds(this);
  }

  public final UTC settoSeconds(double seconds) {
    return setto(secondsToMillis(seconds));
  }

  /**
   * modify this to be after @param other
   *
   * @return this
   */
  public UTC ensureAfter(UTC other) {
    //if reboot caused the clock to roll back...do SOMETHING:
    if (utc < other.utc) {
      utc = other.utc + 1;
    }
    return this;
  }

  public boolean changeByDays(int days) {
    UTC utc = ChangeByDays(this, days);
    if (utc == null) {
      return false;
    } else {
      setto(utc.getTime());
      return true;
    }
  }

}

