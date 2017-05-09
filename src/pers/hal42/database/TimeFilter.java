package pers.hal42.database;

/**
 * <p>Title: $Source: /cvs/src/net/paymate/database/TimeFilter.java,v $</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: PayMate.net</p>
 * @author PayMate.net
 * @version $Revision: 1.2 $
 */

import pers.hal42.data.DateInput;
import pers.hal42.data.StringRange;
import pers.hal42.data.TimeRange;
import pers.hal42.logging.ErrorLogStream;

public class TimeFilter {
  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(TimeFilter.class);

  public TimeRange time;

  public boolean NonTrivial(){
    // do them ALL so that we can debug the output (change back after satisfied)
    return ntprint("time"  , StringRange.NonTrivial(time));
  }

  protected boolean ntprint(String fieldname, boolean is) {
    dbg.ERROR(fieldname + " is " + (is ? "Non" : "") + "Trivial");
    return is;
  }

  public void setTimeRange(DateInput date1, DateInput date2) {
    time = createTimeRange(date1, date2);
  }

  public static TimeRange createTimeRange(DateInput date) {
    return createTimeRange(date, null);
  }

  public static TimeRange createTimeRange(DateInput date1, DateInput date2) {
    if(!(DateInput.nonTrivial(date1) || DateInput.nonTrivial(date2))) {
      dbg.VERBOSE("settimeRange sees that dates are both trivial");
      return null;
    }
    // if no date1, but has date2, swap
    if(DateInput.nonTrivial(date2) && !DateInput.nonTrivial(date1)) {
      dbg.VERBOSE("settimeRange sees that date1 trivial and date2 nontrivial");
      DateInput date3 = date1;
      date1 = date2;
      date2 = date3;
    }
    if(DateInput.nonTrivial(date1) && !DateInput.nonTrivial(date2)) {     // if only one is set
      dbg.VERBOSE("settimeRange sees that date1 nontrivial and date2 trivial");
      if(!date1.nonTrivialTime()) {
        dbg.VERBOSE("settimeRange setting date2 based on date1 values");
        // if only the date is set, but not the time, create a range for the day
        date1.beginningOfDay();
        if(date2 == null) {
          date2 = new DateInput(date1);
        }
        date2.setDayTo(date1);
        date2.beginningNextDay(); // roll date so times are: 20031131000000 - 20031201000000
      } else { // only select a day
        // fine like it is
      }
    } else { // more than one is set
      dbg.VERBOSE("settimeRange sees that date1 nontrivial and date2 nontrivial");
      // make the full range
      // fine like it is
    }
    TimeRange time = TimeRange.Create();
    // if the date is not trivial, fix its format, convert to a Date, and stuff it in the range
    time.include(DateInput.toUTC(date1));
    time.include(DateInput.toUTC(date2));
    return time;
  }

  public TimeFilter() {
    //leaves things null
  }

}
