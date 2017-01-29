package pers.hal42.util;

public class Counter {
  private Monitor mon = new Monitor(Counter.class.getName());
  private long count = 0; // create a thread-safe counter class so that incrementing and getting are atomic +++
  private long min = 0;
  private long max = Long.MAX_VALUE;
  // +_+ make a constructor that starts with a specific value
  public Counter() {
    // default starts at zero
    // and has the limits [0:Long.MAX_VALUE]
  }

  public Counter(long start) {
    count = start;
  }

  public Counter(long min, long max) {
    this.min = min;
    this.max = max;
    norm();
  }

  public Counter(long min, long max, long start) {
    this(min, max);
    count = start;
    norm();
  }

  public final long max() {
    return max;
  }

  public final long min() {
    return min;
  }

  public final long value() {
    return count;
  }

  /**
   * @return ++count
   */
  public final long incr() {
    return chg(1);
  }

  /**
   * @return --count
   */

  public final long decr() {
    return chg(-1);
  }

  /**
   * interlock modify and read operations
   * @return modified value
   */
  public final long chg(long by) {
    try {
      mon.getMonitor();
      count+=by;
      norm();
    } catch (Exception ex) {
      // +++ bitch !!!
    } finally {
      long ret = count;
      //we must wait until after setting the return value before freeing the monitor,
      //because longs are not guaranteed to be atomic.
      mon.freeMonitor();
      return ret;
    }
  }

  /**
   * @return object after clearing
   */
  public final long Clear(){
    try {
      mon.getMonitor();
      count=min;
    } catch (Exception ex) {
      // +++ bitch !!!
      return norm();
    } finally {
      long retval= norm();
      mon.freeMonitor();
      return retval;
    }
  }

  // only really valid if you are incrementing
  private int rollOverCount = 0;

  /**
   * normalize
   */
  private final long norm() {
    if(count < min) {
      count = max; // ??? roll over ???
      rollOverCount++;
    } else if(count > max) {
      count = min; // roll over
      rollOverCount++;
    }
    return count;
  }

  public final int getRollover() {
    return rollOverCount;
  }
}

