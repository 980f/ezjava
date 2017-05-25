package pers.hal42.data;

import pers.hal42.lang.StringX;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.timer.UTC;

public class TimeRange extends ObjectRange<UTC> {

  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(TimeRange.class);
  private static final int MAXIMAGELENGTH = 14;
  private final static String fieldnamekey = "fieldname";

  public UTC start() {
    return one();
  }

  public UTC end() {
    return singular() ? start() : two();
  }

  public Comparable filter(String input) {
    return UTC.New(input);
  }

  //////////////
  // queries
  public long milliSpan() {
    return broad() ? end().getTime() - start().getTime() : 0;
  }

  public final String oneImage() {
    return StringX.left(super.oneImage(), MAXIMAGELENGTH);
  }

  public final String twoImage() {
    return StringX.left(super.twoImage(), MAXIMAGELENGTH);
  }

  public TimeRange setStart(UTC d) {
    dbg.VERBOSE("setStart(UTC):" + d);
    return (TimeRange) setOne(d);
  }

  public TimeRange setEnd(UTC d) {
    dbg.VERBOSE("setEnd(UTC):" + d);
    return (TimeRange) setTwo(d);
  }

  /**
   * for finding the bounds of an unordered set.
   */
  public TimeRange include(UTC d) {
    dbg.VERBOSE("1: d=" + d + ", this=" + this);
    if (d != null) {
      if (two == null || end().compareTo(d) < 0) {
        setTwo(d);
        dbg.VERBOSE("2: d=" + d + ", this=" + this);
      }
      if (one == null || d.compareTo(start()) < 0) {
        setOne(d);
        dbg.VERBOSE("3: d=" + d + ", this=" + this);
      }
    }
    dbg.VERBOSE("4: d=" + d + ", this=" + this);
    return this;
  }

  /**
   * @return a non-null TimeRange whose end points are both non-trivial
   */
  public static TimeRange makeSafe(TimeRange probate) {
    if (probate == null) {
      probate = Create();
    }
    probate.analyze();
    if (!probate.broad()) {
      if (!probate.singular()) {
        probate.setStart(UTC.Now());//is trivial
      }
      probate.setEnd(probate.start());//is or was made singular
    }
    return probate;
  }

  public static TimeRange Create() {
    return new TimeRange();
  }

//  public static TimeRange NewFrom(String key, EasyCursor ezp) {
//    TimeRange newone = Create();
//    ezp.getBlock(newone, key);
//    return newone;
//  }

  public static TimeRange Forever() {
    TimeRange newone = Create();
    UTC start = UTC.New(0L);
    UTC end = UTC.New(Long.MAX_VALUE);
    newone.setStart(start);
    newone.setEnd(end);
    return newone;
  }

  public static TimeRange copy(TimeRange rhs) {//+_+ extract a clone() function
    TimeRange newone = Create();
    if (rhs != null) {
      newone.copyMembers(rhs);
    }
    return newone;
  }

}
