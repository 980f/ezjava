package pers.hal42.math;

import pers.hal42.lang.Monitor;
import pers.hal42.text.Ascii;
import pers.hal42.thread.Counter;
import pers.hal42.transport.EasyCursor;
import pers.hal42.transport.isEasy;

public class Accumulator implements isEasy {

  private long count = 0;
  private long sum = 0;
  private long max = Long.MIN_VALUE;//formerly 0, which didn't allow for negatives
  private long min = Long.MAX_VALUE;
  private Monitor mon = null;
  private static final Counter nameCounter = new Counter();
  public Accumulator() {
    mon = new Monitor("Accumulator." + nameCounter.incr());
  }
  // this is for initializing an Accumulator, so that we can pick up using it where we left off
  public Accumulator(long count, long sum) {
    this();
    set(count, sum);
  }

  public String toString() {
    return String.valueOf(this.sum) + Ascii.bracket(count);
  }

  public String toSpam() {
    return "Cnt:" + getCount() + "/Ttl:" + getTotal() + "/Avg:" + getAverage() + "/Min:" + getMin() + "/Max:" + getMax();
  }

  public void add(long lasttime) {
    try {
      mon.getMonitor();
      count++;
      sum += lasttime;
      max = Math.max(max, lasttime);
      min = Math.min(min, lasttime);
    } catch (Exception e) {
      // +++ bitch
    } finally {
      mon.freeMonitor();
    }
  }

  // add another accumulator in
  public void add(Accumulator that) {
    try (Monitor free = mon.getMonitor();
         Monitor freer = that.mon.getMonitor()) {//gotta gain ownersup of both objects
      count += that.count;
      sum += that.sum;
      max = Math.max(max, that.max);//formerly used average!
      min = Math.min(min, that.min);
    }
    //note TWR releases these two monitors in the reverse order of the original code.
  }

  // this is for initializing an Accumulator, so that we can pick up using it where we left off
  private void set(long count, long sum) {
    try (Monitor free = mon.getMonitor()) {
      this.count = count;
      this.sum = sum;
      max = getAverage();// +_+ should make a function for restoring all four members.
      min = getAverage();// +_+ this is nicer than using extreme values.
    }
  }

  public void zero() {
    set(0, 0);
  }

  /**
   * @param lasttime - what to subtract from the total
   *                 Count is auto-decremented by 1.  Don't change the sign of lasttime before subtracting it.
   *                 Can use this for when you add() something in a moment before you find out that you should not have.
   *                 useful when this is extended into a moving sum rather than an accumulator.
   */
  public void remove(long lasttime) {
    try (Monitor free = mon.getMonitor()) {
      if (count > 0) {
        --count;
        sum -= lasttime;
      }
    }
  }

  public long getTotal() {
    try (Monitor free = mon.getMonitor()) { // longs are not atomic
      return sum;
    }
  }

  public long getCount() {
    try (Monitor free = mon.getMonitor()) { // longs are not atomic
      return count;
    }
  }

  public long getAverage() {
    try (Monitor free = mon.getMonitor()) { // longs are not atomic
      return (count > 0) ? (sum / count) : 0;
    }
  }

  public long getMax() {
    try (Monitor free = mon.getMonitor()) { // longs are not atomic
      return max;
    }
  }

  public long getMin() {
    try (Monitor free = mon.getMonitor()) { // longs are not atomic
      return min;
    }
  }

  //////////////////////////
// isEasy()
  public void save(EasyCursor ezp) {
    try (Monitor free = mon.getMonitor()) {
      ezp.setLong("sum", sum);
      ezp.setLong("num", count);
      ezp.setLong("min", min);
      ezp.setLong("max", max);
    }
  }

  public void load(EasyCursor ezp) {
    try (Monitor free = mon.getMonitor()) {
      sum = ezp.getLong("sum");
      count = ezp.getLong("num");
      min = ezp.getLong("min");
      max = ezp.getLong("max");
    }
  }

}

