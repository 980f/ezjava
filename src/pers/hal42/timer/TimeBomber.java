package pers.hal42.timer;

/**
 * Intended use: timeout on background activity outside of Java
 * set a timebomb ticking, when it blows it calls your function.
 * defuse() it if the desired event happens in a timely fashion.
 * int is used for timer so that we don't have to worry about non-atomicity
 * of longs.
 */

import pers.hal42.logging.ErrorLogStream;
import pers.hal42.thread.ThreadX;

public class TimeBomber implements Runnable {
  protected TimeBomb dynamite = null;
  /**
   * while system timing is done with longs they are not atomic so we use ints
   * and live with the restricted range, which is enormous for a timeout value.
   */
  protected int fuse = 0;
  Thread timer;
  boolean defused = false;
  static final ErrorLogStream dbg = ErrorLogStream.getForClass(TimeBomber.class);
  static int idCounter = 0;


  /**
   * make a background timer that will call upon the dynamite's onTimeout method
   * when time is up.
   *
   * @param fuse     is the number of milliseconds to delay, if zero then
   *                 we just create an object and you must use "startFuse(...)" to get something to happen
   * @param dynamite is the thing to call when time is up.
   */

  private TimeBomber(int fuse, TimeBomb dynamite) {
    this.dynamite = dynamite;
    this.fuse = fuse;
    startFuse();
  }

  /**
   * this is the background timing part
   * after the fuse # of millseconds the payload gets called unless
   * the thread gets interrupted in which case we stop the thread by exiting run.
   */
  public void run() {//executed by the thread "timer"
    // this will get run whenever the thread is done, but not interrupted (defused)
    if (ThreadX.sleepFor(fuse) && (dynamite != null) && !defused) {//thread wasn't always dying in a timely fashion
      dynamite.onTimeout();
    }
  }

  /**
   * turn off a possibly ticking timebomb
   */
  public void defuse() {
    try {
      defused = true;
      timer.interrupt();
    } catch (NullPointerException ignored) {
      //no timer yet, so we just don't care to do anything about this.
    } catch (IllegalThreadStateException ignored) {
      //don't care what happens so long as thread has quit running
    }
  }

  public boolean isTicking() {
    return timer != null && timer.isAlive();
  }

  /**
   * @return fuse time, not to be confused with time remaining.
   */
  public int fuse() {
    return fuse;
  }

  /**
   * start ticking for a new timer value.
   * question: will a small enough value potentially allow blowup before we return?
   */
  public void startFuse(int newfuse) {
    fuse = newfuse;
    startFuse();
  }

  /**
   * start ticking with stored timer value
   */
  public boolean startFuse() {
    try {
      defuse();//stop timer , in case it is already ticking.
      if (fuse > 0) {
        timer = new Thread(this /*as Runnable*/, "TimeBomb#" + dynamite.toString() + "#" + idCounter++ + "#");
        //all uses are as timeouts, it doesn't matter if the timer takes extra time before firing
//#-- when the timeout got set by the callback that **it** calls, the priority racheted down ...
//...until it was MIN and never ran  ---timer.setPriority(Math.min(Thread.currentThread().getPriority(),Thread.NORM_PRIORITY)-1);
        timer.setPriority(Thread.NORM_PRIORITY);//%%TimeoutPriority
        timer.start();
      }
      defused = false;
      return true;
    } catch (IllegalThreadStateException caught) {
      dbg.WARNING("in TimeBomber.startFuse:" + caught);
      return false;
    }
  }

  /**
   * turn off a possibly ticking timebomb
   */
  public static void Defuse(TimeBomber thisone) {
    if (thisone != null) {
      thisone.defuse();
    }
  }

  public static TimeBomber New(int fuse, TimeBomb dynamite) {
    return new TimeBomber(fuse, dynamite);
  }

}

