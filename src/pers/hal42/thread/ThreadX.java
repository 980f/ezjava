package pers.hal42.thread;

import pers.hal42.lang.Finally;
import pers.hal42.lang.StringX;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.text.Formatter;
import pers.hal42.text.TextList;
import pers.hal42.timer.StopWatch;
import pers.hal42.timer.Ticks;


// +++ get rid of this class altogether, using Waiter instead.
//alh: actually move much of qagent's thread handling here rather than getting rid of this class.

public class ThreadX {

  private static ErrorLogStream dbg;

  /**
   * polite and properly synch'd version of Object.wait()
   * this one swallows interrupts
   *
   * @param millisecs - Use Long.MAX_VALUE for infinity.  Using 0 generates a race condition that might or might not result in an infinite wait.
   * @return true if NOT notified.
   * <p>
   * NOTE!  This function CANNOT reliably tell you if you were notified or timed out.  Do NOT rely on it.
   * <p>
   * change this to return a void! WHy? just document what it does return.
   */
  @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
  public static boolean waitOn(Object obj, long millisecs, boolean allowInterrupt, ErrorLogStream dbg) {
    dbg = ErrorLogStream.NonNull(dbg);//avert NPE
    synchronized (obj) { // +++ in order to make code run faster, this synchronized *might* ought to be placed just around the obj.wait() line.
      try (Finally pop = dbg.Push("waitOn")) {
        StopWatch resleeper = new StopWatch();//to shorten successive timeouts when we sleep again after an interrupt
        while (true) {
          try {
            long waitfor = millisecs - resleeper.millis();
            if (waitfor < 0) {
              dbg.WARNING("timed out");
              return true;
            }
            dbg.VERBOSE("waiting for " + waitfor);
            obj.wait(waitfor); //will throw IllegalArgumentException if sleep time is negative...
            dbg.VERBOSE(Formatter.ratioText("proceeds after ", resleeper.Stop(), millisecs));
            return resleeper.Stop() >= millisecs; //preferred exit
          } catch (InterruptedException ie) {
            if (allowInterrupt) {
              dbg.VERBOSE("interrupted,exiting");
              return true;
            } else {
              dbg.WARNING("interrupted,ignored");
              //noinspection UnnecessaryContinue
              continue;
            }
          } catch (Exception ex) {
            dbg.Caught(ex);
            return true;
          }
        }
      }
    }
  }

  public static boolean waitOn(Object obj, long millisecs, ErrorLogStream somedbg) {
    return waitOn(obj, millisecs, false, ErrorLogStream.NonNull(somedbg));
  }

  public static boolean waitOn(Object obj, long millisecs) {
    return waitOn(obj, millisecs, false, ErrorLogStream.Null());
  }

  /**
   * polite and properly synch'd version of Object.notify()
   *
   * @return true if notify did NOT happen
   */
  @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
  public static boolean notify(Object obj) {
    synchronized (obj) {
      try {
        obj.notify();
        return false;
      } catch (Exception ex) {//especially null pointer exceptions
        return true;
      }
    }
  }

  /**
   * returns true if it completed its sleep (was NOT interrupted)
   */
  public static boolean sleepFor(long millisecs, boolean debugit) {
    StopWatch arf = new StopWatch();
    boolean interrupted = false;
    try {
      if (debugit) {
        dbg.ERROR("sleepFor():" + millisecs);
      }
      Thread.sleep(millisecs > 0 ? millisecs : 0);//sleep(0) should behave as yield()
    } catch (InterruptedException e) {
      interrupted = true;
      if (debugit) {
        dbg.ERROR("sleepFor() got interrupted after:" + arf.millis());
      }
    }
    boolean completed = !(Thread.interrupted() || interrupted);
    if (debugit) {
      dbg.ERROR("sleepFor() sleptFor " + arf.millis() + " ms and was " + (completed ? "NOT " : "") + "interrupted.");
    }
    return completed;
    //return !Thread.interrupted(); // did not work correctly

  }

  public static boolean sleepFor(long millisecs) {
    return sleepFor(millisecs, false);
  }

  public static boolean sleepFor(double seconds) {
    return sleepFor(Ticks.forSeconds(seconds));
  }

  // WARNING!  You will never see this thread again.
  public static void sleepForever() {
    sleepFor(Long.MAX_VALUE);
  }

  /**
   * caller is responsible for trying to make the thread stop()
   */
  public static boolean waitOnStopped(Thread mortal, long maxwait) {
    mortal.interrupt();
    try {
      mortal.join(maxwait);
      return false;
    } catch (Exception ex) {
      dbg.Caught(ex, "waitOnStopped()");//+_+ needs more info
      return true;
    }
  }

  public static boolean wait(Thread me, long maxwait) {
    try {
      me.wait(maxwait);
      return false;
    } catch (InterruptedException ex) {
      return true;
    }
  }

  public static boolean join(Thread it, long maxwait) {
    try {
      it.join(maxwait);
      return false;
    } catch (InterruptedException ex) {
      return true;
    }
  }

  public static ThreadGroup RootThread() {
    ThreadGroup treeTop = Thread.currentThread().getThreadGroup();
    ThreadGroup tmp;
    while ((tmp = treeTop.getParent()) != null) {
      treeTop = tmp;
    }
    return treeTop;
  }

  public static int ThreadCount() {
    return ThreadDump(null).size();
  }

  public static TextList fullThreadDump() {
    return ThreadDump(null);
  }

  public static TextList ThreadDump(ThreadGroup tg) {
    if (tg == null) {
      tg = RootThread();
    }
    TextList ul = new TextList();
    int threadCount = tg.activeCount();
    Thread[] list = new Thread[threadCount * 2]; // plenty of room this way
    int count = tg.enumerate(list);
    for (int i = 0; i < count; i++) {
      Thread t = list[i];
      if (t.getThreadGroup() == tg) {
        String name = t.getName() + "," + t.getPriority() + ((t.isDaemon()) ? ",daemon" : "");
        if (t instanceof ThreadReporter) {
          ThreadReporter sess = (ThreadReporter) t;
          name += sess.status();
        }
        ul.add(name);
      }
    }
    // print the child thread groups
    int groupCount = tg.activeGroupCount();
    ThreadGroup[] glist = new ThreadGroup[groupCount * 2]; // plenty of room this way
    groupCount = tg.enumerate(glist);
    for (int i = 0; i < groupCount; i++) {
      ThreadGroup g = glist[i];
      if (g.getParent() == tg) {
        ul.appendMore(ThreadDump(g));
      }
    }
    return ul;
  }

  /**
   * start a background service type task.
   *
   * @param target task to run
   * @return null on defective target else a running thread running that target
   */
  public static Thread Demonize(Runnable target, String traceName) {
    if (target != null) {
      Thread runner = new Thread(target, StringX.OnTrivial(traceName, "ThreadX.Demonized"));
      runner.setDaemon(true);
      runner.start();
      return runner;
    }
    return null;
  }

  /**
   * start a background worker type task
   *
   * @param target to run
   * @return null on defective target else a running thread running that target
   */
  public static Thread Launch(Runnable target, String traceName) {
    if (target != null) {
      Thread runner = new Thread(target, StringX.OnTrivial(traceName, "ThreadX.Demonized"));
      runner.setDaemon(false);
      runner.start();
      return runner;
    }
    return null;
  }
}

