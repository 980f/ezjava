package pers.hal42.timer;

import pers.hal42.logging.ErrorLogStream;
import pers.hal42.logging.LogLevelEnum;
import pers.hal42.transport.EasyProperties;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class CronComputer {
  private final GregorianCalendar zoned;
  private Date jre13hack = new Date();//made a member instead of a local for performance reasons.
  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(CronComputer.class, LogLevelEnum.VERBOSE);
  ///////////////
  private static final int DAY = 4;
  private static final int HOUR = 3;
  private static final int MINUTE = 2;
  private static final int SECOND = 1;
  private static final int MILLISECOND = 0;

  public CronComputer(TimeZone tz) {
    zoned = (GregorianCalendar) Calendar.getInstance(tz);
  }

  public TimeZone getZone() {
    return zoned.getTimeZone();
  }

  // the private parts aren't synch'd because <b>all</b> the public ones are!
  private void clearRange(int range) {
    switch (range) {
      case HOUR:
        zoned.set(Calendar.HOUR_OF_DAY, 0);
      case MINUTE:
        zoned.set(Calendar.MINUTE, 0);
      case SECOND:
        zoned.set(Calendar.SECOND, 0);
      case MILLISECOND:
        zoned.set(Calendar.MILLISECOND, 0);
    }
  }

  private long startOfPeriod(int range, UTC now) {
    jre13hack.setTime(now.getTime());
    zoned.setTime(jre13hack);//#setTimeInMillis is protected in early jre's
    //zoned is now.
    this.clearRange(range);
    return zoned.getTime().getTime();
  }

  private long nextEvent(int range, int unit, int interval, UTC now) {
    return startOfPeriod(range, now) + ticksFor(unit, interval);
  }

  public long startOfHour(UTC now) {
    synchronized (zoned) {
      return startOfPeriod(MINUTE, now);
    }
  }
  // ... can do that as we synch all public members to the same object.

  public long startOfDay(UTC now) {
    synchronized (zoned) {
      return startOfPeriod(HOUR, now);
    }
  }

  public long nextDaily(int interval, UTC now) {
    synchronized (zoned) {
      return nextEvent(HOUR, MINUTE, interval, now);//every day on the minute
    }
  }

  public boolean dailyDue(int interval, UTC lastTime, UTC now) {
    synchronized (zoned) {
      return isDue(nextDaily(interval, now), now, lastTime);
    }
  }

  public long nextHourly(int interval, UTC now) {
    synchronized (zoned) {
      return nextEvent(MINUTE, MINUTE, interval, now);//every hour on the minute
    }
  }

  public boolean hourlyDue(int interval, UTC lastTime, UTC now) {
    synchronized (zoned) {
      return isDue(nextHourly(interval, now), now, lastTime);
    }
  }
////////////////

  public static CronComputer New(TimeZone tz) {
    return new CronComputer(tz);
  }

  public static CronComputer UTC() {
    return New(TimeZone.getTimeZone("UTC"));
  }

  private static long ticksFor(int unit, int interval) {
    switch (unit) {
      case HOUR:
        return Ticks.forHours(interval);
      case MINUTE:
        return Ticks.forMinutes(interval);
      case SECOND:
        return Ticks.forSeconds(interval);
      case MILLISECOND:
        return interval;
    }
    return 0;//gross defect
  }

  /**
   * @param done
   * @param now
   * @return whether now is past the next scheduled time
   */
  private static boolean isDue(long due, UTC now, UTC done) {
    boolean isDue = now.getTime() > due && due > done.getTime();
    EasyProperties ezp = new EasyProperties();
    ezp.setUTC("now", now);
    ezp.setUTC("waiter", done);
    ezp.setUTC("due", UTC.New(due));
    dbg.VERBOSE("isDue is " + isDue + " since:\n" + ezp);
    return isDue;
  }

}

