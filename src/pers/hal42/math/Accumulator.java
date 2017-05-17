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
    try (AutoCloseable free = mon.getMonitor(); AutoCloseable freer = that.mon.getMonitor()) {//gotta gain ownersup of both objects
      count += that.count;
      sum += that.sum;
      max = Math.max(max, that.max);//formerly used average!
      min = Math.min(min, that.min);
    } catch (Exception e) {
      // +++ bitch
    }
    //note TWR releases these two monitors in the reverse order of the original code.
  }

  // this is for initializing an Accumulator, so that we can pick up using it where we left off
  private void set(long count, long sum) {
    try (AutoCloseable free = mon.getMonitor()) {
      this.count = count;
      this.sum = sum;
      max = getAverage();// +_+ should make a function for restoring all four members.
      min = getAverage();// +_+ this is nicer than using extreme values.
    } catch (Exception e) {
      // +++ bitch
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
    try (AutoCloseable free = mon.getMonitor()) {
      if (count > 0) {
        --count;
        sum -= lasttime;
      }
    } catch (Exception e) {
      // +++ bitch
    }
  }

  public long getTotal() {
    try (AutoCloseable free = mon.getMonitor()) { // longs are not atomic
      return sum;
    } catch (Exception e) {
//      e.printStackTrace();
      return -1;
    }
  }

  public long getCount() {
    try (AutoCloseable free = mon.getMonitor()) { // longs are not atomic
      return count;
    } catch (Exception e) {
//      e.printStackTrace();
      return -1;
    }
  }

  public long getAverage() {
    try (AutoCloseable free = mon.getMonitor()) { // longs are not atomic
      return (count > 0) ? (sum / count) : 0;
    } catch (Exception e) {
      return -1;//there is NO reasonable return at this point.
    }
  }

  public long getMax() {
    try (AutoCloseable free = mon.getMonitor()) { // longs are not atomic
      return max;
    } catch (Exception e) {
      return -1;
    }
  }

  public long getMin() {
    try (AutoCloseable free = mon.getMonitor()) { // longs are not atomic
      return min;
    } catch (Exception e) {
      return -1;
    }
  }

  //////////////////////////
// isEasy()
  public void save(EasyCursor ezp) {
    try (AutoCloseable free = mon.getMonitor()) {
      ezp.setLong("sum", sum);
      ezp.setLong("num", count);
      ezp.setLong("min", min);
      ezp.setLong("max", max);
    } catch (Exception e) {
//      e.printStackTrace();
    }
  }

  public void load(EasyCursor ezp) {
    try (AutoCloseable free = mon.getMonitor()) {
      sum = ezp.getLong("sum");
      count = ezp.getLong("num");
      min = ezp.getLong("min");
      max = ezp.getLong("max");
    } catch (Exception e) {
//      e.printStackTrace();
    }
  }

}

