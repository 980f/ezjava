package pers.hal42.lang;
/* usage pattern when ErrorLogStream and a Monitor are used together:
void amethod() {
  try (Autocloseable free=yoreMon.getMonitor();Autocloseable pop=dbg.Enter()){ // if done in reverse the log looks like the monitor didn't work
    //do protected stuff
  }
}
also note that on construction of a Monitor you can supply your own debugger which has nice messages within getMon and freeMon.
*/

import pers.hal42.logging.ErrorLogStream;
import pers.hal42.text.TextList;

import java.util.Collections;
import java.util.Vector;

public final class Monitor implements Comparable<Monitor>, AutoCloseable {
  /**
   * for debug, but also is internally used for locking.
   */
  public final String name;
  private final Vector<Thread> threads = new Vector<>();
  /**
   * the active thread that holds this monitor
   */
  protected Thread thread = null;
  /**
   * monitorCount tracks the nesting level of the owning thread
   */
  protected int monitorCount = 0;
  private ErrorLogStream d1b2g3 = null; // don't use me directly; use dbg() instead !

  // static list stuff, for debug of deadlocks and kin:
  private static final WeakSet<Monitor> list = new WeakSet<>();
  /** shareable lazy initialized debugger */
  private static ErrorLogStream D1B2G3 = null; // don't use me directly; use dbg() instead !

  //each monitor is linkable to some other module's debugger, typically the owner's.
  public Monitor(String name, ErrorLogStream dbg) {
    this.name = ReflectX.stripPackageRoot(name);
    this.d1b2g3 = dbg; // see dbg(), but don't call in here or you will get a loop/construction race condition
    addMonitor(this);
  }

  /**
   * use class debugger, which is normally OFF
   */
  public Monitor(String name) {
    this(name, null);
  }

  // to check to see if we even want to try to get it ...
  public int monitorCount() {
    return monitorCount;
  }

  // this function fixes the loop / race condition which used to exist with this line:
  //  private static final ErrorLogStream DBG = ErrorLogStream.getForClass(Monitor.class);
  // eg: Monitor creates ErrorLogStream, which creates LogFile, which creates Monitor, whose "class" is not yet loaded, so results in an exception and subsequent null pointers.
  private ErrorLogStream dbg() {
    synchronized (Monitor.class) { // sync on the CLASS !
      if (d1b2g3 == null) {
        d1b2g3 = ErrorLogStream.Null(); // --- this line used to say: d1b2g3 = D1B2G3();, which i believe worked.  This new line is suspect of causing the same problem as it was trying to fix!  Why did we change it?  We should document ANY changes in here!
      }
    }
    return d1b2g3;
  }

  public String ownerInfo() {
    synchronized (name) {
      String threadpart = thread != null ? String.valueOf(thread) : "noOwner";
      return name + "/" + threadpart + "*" + monitorCount;
    }
  }

  //getMonitor is called by debug code and infinite loops if you attempt dbg within it.
  public AutoCloseable getMonitor() {
    synchronized (name) {
      try {
        while (!tryMonitor()) {
          try {
            setState(true);
            wait();
          } catch (Exception e) {
            // stub
          } finally {
            setState(false); // +++ should this be in freeMonitor()?
          }
        }
      } catch (Exception e2) {
        //can't deal with anything, used in logging mechanism so can't log 'em
      }
    }
    return this;
  }

  // +++ what happens if someone calls this who didn't call getMonitor()?
  // +++ should thw setState(false) be in here so it can be looked up first?
  // no, cause someone might forget to call freeMonitor() --- maybe
  public void freeMonitor() {
    synchronized (name) {
      try {
        if (getMonitorOwner() == Thread.currentThread()) {
          if ((--monitorCount) == 0) {
            thread = null;
            notify();
          }
        }
      } catch (Exception e2) {
      }
    }
  }

  //noisy versions of the above:
  public AutoCloseable LOCK(String moreinfo) {
    dbg().VERBOSE(moreinfo + " trying to Lock:" + ownerInfo());
    synchronized (name) {
      getMonitor();
      dbg().VERBOSE("Locked by: " + moreinfo + " " + Thread.currentThread());
    }
    return this;
  }

  public void UNLOCK(String moreinfo) {
    synchronized (name) {
      dbg().VERBOSE("Freed by: " + moreinfo + " " + Thread.currentThread());
      freeMonitor();
    }
  }


  public boolean tryMonitor() {

    synchronized (name) {  //## must use same synch as getMonitor.
      try {
        if (thread == null) {
          monitorCount = 1;
          thread = Thread.currentThread();
          return true;
        } else if (thread == Thread.currentThread()) {
          monitorCount++;
          return true;
        } else {
          return false;
        }
      } catch (Exception e2) {
        dbg().Caught(e2);
        return false;
      }
    }
  }

  public Thread getMonitorOwner() {
    synchronized (name) {
      try {
        return thread;
      } catch (Exception e2) {
        dbg().Caught(e2);
        return null;
      }
    }
  }

  public TextList dump() {
    TextList tl = new TextList();
    synchronized (threads) {
      for (Thread thread : threads) {
        tl.add(String.valueOf(thread));
      }
    }
    return tl;
  }

  // no sync required here
  private boolean setState(boolean add) {
    return setState(Thread.currentThread(), add);
  }


  private boolean setState(Thread thread, boolean add) {
    synchronized (threads) {
      try {
        if (add) {
          if (threads.indexOf(thread) >= 0) {
            return true;//already on the thread
          } else {
            threads.add(thread);
            return true;
          }
        } else {
          return threads.removeElement(thread);
        }
      } catch (Exception e2) {
        dbg().Caught(e2);
        return false;
      }
    }
  }

  private Thread findThread(Thread thread) {
    synchronized (threads) {
      for (Thread sThread : threads) {
        if (sThread == thread) {
          return sThread;
        }
      }
      return null;
    }
  }

  public int compareTo(Monitor o) {
    try {
      return name.compareTo(o.name);
    } catch (Exception e) {
      /// +++ bitch
      return 0;
    }
  }

  @Override
  public void close() throws Exception {
    freeMonitor();
  }

  private static ErrorLogStream D1B2G3() {
    if (D1B2G3 == null) {
      D1B2G3 = ErrorLogStream.getForClass(Monitor.class);
    }
    return D1B2G3;
  }

  public static Vector<Monitor> dumpall() {
    Vector<Monitor> v = new Vector<>(); // for sorting
    synchronized (list) {
      v.addAll(list);
    }
    Collections.sort(v);
    return v;
  }

  protected static void deleteMonitor(Monitor monitor) {
    synchronized(list) {
      try {
        list.remove(monitor);
      } catch (Exception e2) {
        D1B2G3.Caught(e2);
      }
    }
  }


  protected static Monitor findMonitor(Monitor monitor) {

    synchronized(list) {
      try {
        for (Monitor mon : list) {
          if (mon == monitor) {//#same object
            return mon;
          }
        }
        return null;
      } catch (Exception e2) {
        D1B2G3.Caught(e2);
        return null;
      }
    }
  }


  protected static void addMonitor(Monitor monitor) {
    synchronized (list) {
      try {
        list.add(monitor);
      } catch (Exception e2) {
        D1B2G3().Caught(e2); // ok
      }
    }
  }

}
/*
An example of mutexing via file locking:
import java.io.*;
public class exclude {
  public static void main(String a[]) {
    File locker = new File("/tmp/mailfile.lock");
    boolean IgotIt = false;
    AcquireLockLoop: for ( ; ; ) { // repeat forever
      IgotIt = locker.createNewFile();
      if (IgotIt) { // we created the file (got the lock)
        break AcquireLockLoop;
      } else { // otherwise, sleep for a bit and try again
        System.out.println(a[0] + " didn't get file, trying again");
        Thread.sleep(3000);
      }
    }
    if (IgotIt) { // do regular work here,
      // assured that we have sole access to the resource.
      System.out.println(a[0]+ " got the file!");
      locker.deleteOnExit();
      Thread.sleep(2000);
    }
  }
}
*/

