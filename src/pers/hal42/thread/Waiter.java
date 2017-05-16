package pers.hal42.thread;

/*
  usage:
  call waiter.prepare() BEFORE triggering behavior that will eventually notify you.
  call waiter.Start(...) to wait for notification. It returns "state" or you can get
  that same value from the state() function.
  All parameters are set by prepare(...) variations, internal defaults used for ones omitted
  Start(...) variations only modify the parameters passed, retaining ones set by the last prepare()

  see end of file for more extensive usage advice

  NOTE: JVM requires that we synch on the same object that is used for wait and notify!
 */

import pers.hal42.logging.ErrorLogStream;
import pers.hal42.text.Ascii;
import pers.hal42.text.Formatter;
import pers.hal42.timer.StopWatch;

public class Waiter {
  /**
   * waitOnMe, safe object to do actual thread wait's on.
   */
  private final Object waitOnMe = new Object();
  long millisecs;
  boolean allowInterrupt;
  ErrorLogStream dbg;
  private int state = Ready;
  /**
   * originally made a member instead of a local to reduce startup overhead in
   * Wait. primary use is to shorten successive timeouts when we sleep again
   * after ignoring an interrupt conveniently also provides a response time
   */
  private StopWatch resleeper = new StopWatch();
  ////////////////////
  public static final int Ready = 0;
  public static final int Notified = 1;
  public static final int Timedout = 2;
  public static final int Interrupted = 3;
  public static final int Excepted = 4;
  public static final int Extending = 5;
  ///////////////////

  /**
   * create a Waiter, with legal but useless configuration.
   *
   * @ see Create(...)
   */
  public Waiter() {
    if (dbg == null) {
      dbg = ErrorLogStream.Null();
    }
    prepare();
  }

  /**
   * toString
   *
   * @return internal state as human readable String
   */
  public String toString() {
    return stateString(state);
  }

  /**
   * something to do a switch upon... you might want to 'synch(myWaiter)' around
   * your switch for clarity, but such a synch is not required for rational use
   * of the waiter.
   *
   * @return psuedo enumeration of internal state
   */
  public int state() {
    return state;
  }

  /**
   * is
   *
   * @return whether present state matches @param possiblestate int
   */
  public boolean is(int possiblestate) {
    return state == possiblestate;
  }

  /**
   * @return milliseconds from wait until notify. only strictly valid if there
   * WAS a notification. will be zero but not negative if notified before
   * wait started.
   */
  public int elapsedTime() {
    return (int) resleeper.millis();
  }

  /**
   * @param millisecs becomes the wait time POSSIBLY IMMEDIATELY i.e. this can
   *                  cause a timeout if changed while running. That is good.
   * @return this
   */
  public Waiter set(long millisecs) {
    this.millisecs = millisecs;
    return this;
  }

  /**
   * set
   *
   * @param allowInterrupt sets whether to allow 'interrupted' state, else thread interrupts are ignored
   * @return this
   */
  public Waiter set(boolean allowInterrupt) {
    this.allowInterrupt = allowInterrupt;
    return this;
  }

  /**
   * set
   *
   * @param dbg ErrorLogStream, set debug output stream, protects itself against defective input.
   * @return this
   */
  public Waiter set(ErrorLogStream dbg) {
    this.dbg = ErrorLogStream.NonNull(dbg);//avert NPE
    return this;
  }

  /**
   * setto, coordinated (synch'ed) setting of parameters
   *
   * @param millisecs      long @see set(long)
   * @param allowInterrupt boolean @see set(boolean)
   * @param dbg            ErrorLogStream @see set(ErrorLogStream)
   * @return this
   */
  Waiter setto(long millisecs, boolean allowInterrupt, ErrorLogStream dbg) {
    synchronized (waitOnMe) {//ensure atomic changing of the 3 args. might be gratuitous but doesn't hurt.
      return set(millisecs).set(allowInterrupt).set(dbg);
    }
  }

  /**
   * note: the synch in prepare is required. Clears any old notification or problem so that we can see a new one.
   *
   * @return this
   */
  public Waiter prepare() {
    synchronized (waitOnMe) {
      state = Ready;
      resleeper.Reset();//lets us discover that we notified before waiting.
      return this;
    }
  }

  /**
   * starts waiting
   *
   * @return state when waiting is done.
   */
  public int run() {
    synchronized (waitOnMe) {
      dbg.Enter("Wait");
      dbg.VERBOSE("waiter state is:" + this + " toat:" + millisecs);
      try {
        resleeper.Start(); //always a fresh start, no "lap time" on our stopwatches.
        if (state == Extending) {//allow for extending before starting.
          state = Ready;
        }

        while (state == Ready) {
          long waitfor = millisecs - resleeper.millis();
          if (waitfor < 0) {
            dbg.VERBOSE("timed out before waiting");
            state = Timedout;
          } else {
            dbg.VERBOSE("waiting for " + waitfor);
            try {
              waitOnMe.wait(waitfor); //will throw IllegalArgumentException if sleep time is negative...
            } catch (InterruptedException ie) {
              Thread.interrupted();//clear thread's interrupted flag
              if (allowInterrupt) {
                dbg.VERBOSE("interrupted,exiting");
                state = Interrupted;
              } else {
                dbg.VERBOSE("interrupted,ignored");
                if (state == Extending) {//any other state (besides Ready)will result in ending the wait when we do the continue.
                  state = Ready;
                }
                continue;//interrupts are ignored
              }
            }
            dbg.VERBOSE(Formatter.ratioText("proceeds after ", resleeper.Stop(), millisecs));
            if (state == Extending) {
              state = Ready;//but resleeper is left alone, the time set by Extend() begins when that call occurs.
              continue;//if notified we will exit the while, don't need to check for that here.
            }
            if (state == Ready) {
              dbg.VERBOSE("timed out the normal way");
              state = Timedout;
            }
          } //end normal wait
        }//end while still in ready state
      } catch (Exception ex) {
        dbg.Caught(ex);
        state = Excepted;
      } finally {
        resleeper.Stop();//valiant attempt at figuring out when a problem occured.
        dbg.Exit();
        return state();
      }
    }//end synch
  }

  /**
   * stretch a wait in progress, or set wait time for next wait.
   *
   * @param milliseconds time to continue waiting starting from NOW, not original start().
   * @return the state, if not 'extending' then wasn't in a state legal to extend
   * note: if you use this you should always give an argument to prepare().
   */
  public int Extend(long milliseconds) {
    synchronized (waitOnMe) {
      dbg.WARNING("trying to extend from: " + this.millisecs + " to: " + milliseconds);
      if (state == Ready || state == Extending) { //can only extend when already running or extending
        this.millisecs = milliseconds;
        state = Extending;
        dbg.WARNING("restarting internal timer");
        resleeper.Start(); //forget the past
        try {
          waitOnMe.notify(); //internal notify, to make things more brisk.
        } catch (Exception ex) { //especially null pointer exceptions
          state = Excepted;
        }
      } else {
        dbg.WARNING("was not in extendible state:" + state);
      }
      return state();
    }
  }

  /**
   * start waiting, setting all parameters
   *
   * @param millisecs      long @see set(long)
   * @param allowInterrupt boolean @see set(boolean)
   * @param dbg            ErrorLogStream @see set(ErrorLogStream)
   * @return what caused wait to terminate, @see run()
   */
  public int Start(long millisecs, boolean allowInterrupt, ErrorLogStream dbg) {
    setto(millisecs, allowInterrupt, dbg);
    //it is ok if any of the notification functions is called by another thread between these lines of code.
    return run();
  }

  /**
   * Start, retain present 'allowInterrupt' setting
   *
   * @param millisecs long @see set(long)
   * @param somedbg   ErrorLogStream @see set(ErrorLogStream)
   * @return what caused wait to terminate, @see run()
   */
  public final int Start(long millisecs, ErrorLogStream somedbg) {
    return Start(millisecs, this.allowInterrupt, somedbg);
  }

  /**
   * Start, retain all settings except how lont to wait.
   *
   * @param millisecs long @see set(long)
   * @return what caused wait to terminate, @see run()
   */
  public final int Start(long millisecs) {
    return Start(millisecs, this.allowInterrupt, this.dbg);
  }

  /**
   * configure waiter but don't wait.
   * call run() to wait using these values.
   *
   * @param millisecs      long @see set(long)
   * @param allowInterrupt boolean @see set(boolean)
   * @param dbg            ErrorLogStream @see set(ErrorLogStream)
   * @return this
   */
  public Waiter prepare(long millisecs, boolean allowInterrupt, ErrorLogStream dbg) {
    setto(millisecs, allowInterrupt, dbg);
    return prepare();
  }

  public final Waiter prepare(long millisecs, ErrorLogStream somedbg) {
    return prepare(millisecs, false, somedbg);
  }

  public final Waiter prepare(long millisecs) {
    return prepare(millisecs, false, ErrorLogStream.Null());
  }

  /**
   * polite and properly synch'd version of Object.notify()
   * private so that only certain states can be indicated.
   *
   * @param newstate what a pending run() will return
   * @return true if notify did NOT happen, mostly of academic interest.
   */
  private boolean notifyThis(int newstate) {
    synchronized (waitOnMe) {
      state = newstate;
      try {
        waitOnMe.notify();
        return false;
      } catch (Exception ex) {//especially null pointer exceptions
        state = Excepted;
        return true;
      } finally {
        resleeper.Stop(); //owner gets a repsonse time from this.
      }
    }
  }

  /**
   * normal wait complete notification, i.e. call this at point where an event has occured when the wait is being used to timeout waiting for that event.
   *
   * @return true if notify did NOT happen, mostly of academic interest.
   */
  public boolean Stop() {
    return notifyThis(Notified); //yes, overrides Interrupted and all other states.
  }

  /**
   * force a timeout NOW
   *
   * @return true if notify did NOT happen, mostly of academic interest.
   */
  public boolean forceTimeout() {
    return notifyThis(Timedout); //yes, overrides Interrupted and all other states.
  }

  /**
   * force an exception indication
   *
   * @return true if notify did NOT happen, mostly of academic interest.
   */
  public boolean forceException() {
    return notifyThis(Excepted); //terminates wait and indicates things are screwed
  }

  /**
   * this exists to allow for the full wait time to pass before code proceeds,
   * useful for preventing "spam on error" loops I.e. when the wait is terminated by an error rather
   * than the preferred condition we might choose to wait for the same amount of time that we would have
   * if the wait had been terminate due to a timeout, so that other errors have the same realtime
   * behavior as 'no response'.
   */
  public void finishWaiting() {
    long remaining = millisecs - resleeper.millis();
    if (remaining > 0) {
      dbg.WARNING("Finishing the wait for " + remaining + " ms.");
      ThreadX.sleepFor(remaining);
    } else {
      dbg.WARNING("Not waiting any further (" + remaining + " ms remaining)");
    }
  }

  /**
   * toSpam
   *
   * @return more info than toString
   */
  public String toSpam() {
    return Ascii.bracket(this.toString() + " elapsed:" + this.elapsedTime());
  }

  public static String stateString(int state) {
    switch (state) {
      case Ready:
        return "Ready";
      case Notified:
        return "Notified";
      case Timedout:
        return "Timedout";
      case Interrupted:
        return "Interrupted";
      case Excepted:
        return "Excepted";
      case Extending:
        return "Extending";
      default:
        return "Unknown";
    }
  }

  /**
   * Create
   *
   * @param millisecs      long @see set(long)
   * @param allowInterrupt boolean @see set(boolean)
   * @param dbg            ErrorLogStream @see set(ErrorLogStream)
   * @return new configured Waiter
   */
  public static Waiter Create(long millisecs, boolean allowInterrupt, ErrorLogStream dbg) {
    return new Waiter().setto(millisecs, allowInterrupt, dbg);
  }
}

/*
you must prepare() to wait
then execute code that will result later in a notify
  (which can do so instantly and on the same or another thread without a problem)
then run().

The functions Start(---) should be renamed run(---).

in each of the following usage guides
  '...' stands for the code that triggers the notify that we are waiting upon
  args stands for the wait time, allow interrupt, debugger values used by run()


usage 1: know wait arguments before trigger event:
prepare(args) ... run()

usage 2: know some wait arguments only after trigger event, such as when some
return value from the trigger code is needed to compute a wait time.
prepare() ... Start(args)

usage 3: args never change
use static Create() function

prepare() ... run()

*/


