package pers.hal42.lang;
/* usage pattern when ErrorLogStream and a Monitor are used together:
void amethod() {
  try {
    yoreMon.getMonitor();
    dbg.Enter(); //<<done too soon and the log looks like the monitor didn't work
    //do protected stuff
  } finally {
    dbg.Exit(); //<<precede unlock to keep trace proper.
    yoreMon.freeMonitor();
  }
}
also note that on construction of a Monitor you can supply your own debugger which has nice messages within getMon and freeMon.
*/

import pers.hal42.logging.ErrorLogStream;
import pers.hal42.text.TextList;

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Vector;

public final class Monitor implements Comparable {
  protected Thread thread = null;
  /**
  * monitorCount tracks the nesting level of the owning thread
  */
  protected int monitorCount = 0;
  public String name = "";
  private Vector<Thread> threads = new Vector<>();

  // to check to see if we even want to try to get it ...
  public int monitorCount() {
    return monitorCount;
  }

  // this function fixes the loop / race condition which used to exist with this line:
  //  private static final ErrorLogStream DBG = ErrorLogStream.getForClass(Monitor.class);
  // eg: Monitor creates ErrorLogStream, which creates LogFile, which creates Monitor, whose "class" is not yet loaded, so results in an exception and subsequent null pointers.
  private ErrorLogStream dbg() {
    synchronized(Monitor.class) { // sync on the CLASS !
      if(d1b2g3 == null) {
        d1b2g3 = ErrorLogStream.Null(); // --- this line used to say: d1b2g3 = D1B2G3();, which i believe worked.  This new line is suspect of causing the same problem as it was trying to fix!  Why did we change it?  We should document ANY changes in here!
      }
    }
    return d1b2g3;
  }
  private static ErrorLogStream D1B2G3 = null; // don't use me directly; use dbg() instead !
  private ErrorLogStream d1b2g3 = null; // don't use me directly; use dbg() instead !
  private static ErrorLogStream D1B2G3() { // don't use me directly; use dbg() instead !
    if(D1B2G3 == null) {
      D1B2G3 = ErrorLogStream.getForClass(Monitor.class);
    }
    return D1B2G3;
  }

  //each monitor is linkable to some other module's debugger, typically the owner's.
  public Monitor(String name,ErrorLogStream dbg) {
    this.name = ReflectX.stripPackageRoot(name);
    this.d1b2g3 = dbg; // see dbg(), but don't call in here or you will get a loop/construction race condition
    addMonitor(this);
  }

  public String ownerInfo(){
    synchronized(this){
      String threadpart=thread!=null?String.valueOf(thread):"noOwner";
      return name+"/"+threadpart+"*"+monitorCount;
    }
  }

  /**
  * use class debugger, which is normally OFF
  */
  public Monitor(String name) {
    this(name,null);
  }

//getMonitor is called by debug code and infinite loops if you attempt dbg within it.
  public void getMonitor() {
    synchronized(this) {
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
      } finally {
      }
    }
  }

  // +++ what happens if someone calls this who didn't call getMonitor()?
  // +++ should thw setState(false) be in here so it can be looked up first?
  // no, cause someone might forget to call freeMonitor() --- maybe
  public void freeMonitor() {
    synchronized(this) {
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
  public void LOCK(String moreinfo){
    dbg().VERBOSE(moreinfo+" trying to Lock:"+ownerInfo());
    synchronized(this){
      getMonitor();
      dbg().VERBOSE("Locked by: "+moreinfo+" "+Thread.currentThread());
    }
  }

  public void UNLOCK(String moreinfo){
    synchronized(this){
      dbg().VERBOSE("Freed by: "+moreinfo+" "+Thread.currentThread());
      freeMonitor();
    }
  }


  public boolean tryMonitor() {
    boolean ret = false;
    synchronized(this) {
      try {
        if (thread == null) {
          ret = true;
          monitorCount = 1;
          thread = Thread.currentThread();
        } else {
          if (thread == Thread.currentThread()) {
            ret = true;
            monitorCount++;
          }
        }
      } catch (Exception e2) {
        dbg().Caught(e2);
      }
    }
    return ret;
  }

  public Thread getMonitorOwner() {
    Thread ret = null;
    synchronized(this) {
      try {
        ret = thread;
      } catch (Exception e2) {
        dbg().Caught(e2);
      }
    }
    return ret;
  }

  public TextList dump() {
    TextList tl = new TextList();
    synchronized(this) {
      try {
        for(int i = threads.size(); i-->0;) {
          try {
            tl.add(String.valueOf(threads.elementAt(i)));
          } catch(ArrayIndexOutOfBoundsException arf){
            //we modified the list while scanning it, start scan over:
            i = threads.size();
            continue;
          }
        }
      } catch (Exception e) {
        dbg().Caught(e);
      }
    }
    return tl;
  }

  // no sync required here
  private boolean setState(boolean add) {
    return setState(Thread.currentThread(), add);
  }

  private boolean setState(Thread thread, boolean add) {
    boolean allThere = true;
    synchronized(this) {
      try {
        Thread threader = findThread(thread);
        if(add) {
          if(threader != null) {
            // ERROR!
          } else {
            threads.add(thread);
          }
        } else {
          allThere = false;
          if(threader == null) {
            // ERROR!
          } else {
            threads.remove(threader);
          }
        }
      } catch (Exception e2) {
        dbg().Caught(e2);
      }
    }
    return allThere;
  }

  private Thread findThread(Thread thread) {
    Thread sThread = null;
    synchronized(this) {
      try {
        for(int i = threads.size(); i-->0;) {
          try {
            sThread = threads.elementAt(i);
          } catch(ArrayIndexOutOfBoundsException arf){
            //we modified the list while scanning it, start scan over:
            i = threads.size();
            continue;
          }
          if(sThread == thread) {
            break;//return sThread;
          }
        }
        if(sThread != thread) {
          sThread = null;
        }
      } catch (Exception e2) {
        dbg().Caught(e2);
      }
    }
    return sThread;
  }

  // static list stuff
  private static final WeakSet list = new WeakSet();

  public static Vector dumpall() {
    Vector v = new Vector(); // for sorting
    synchronized(list) {
      try {
        for(Iterator i = list.iterator(); i.hasNext();) {
          v.add(i.next());
        }
      } catch (ConcurrentModificationException cme) {
        D1B2G3().Caught(cme); // ok
        v.add("--- Report aborted: ConcurrentModificationException (noncritical, just happens with multiple threads sometimes).");
      } catch (Exception e) {
        D1B2G3().Caught(e); // ok
      }
    }
    Collections.sort(v);
    return v;
  }

  protected static void addMonitor(Monitor monitor) {
    synchronized(list) {
      try {
        list.add(monitor);
      } catch (Exception e2) {
        D1B2G3().Caught(e2); // ok
      }
    }
  }

  /*
  protected static final void deleteMonitor(Monitor monitor) {
    synchronized(list) {
      try {
        list.remove(monitor);
      } catch (Exception e2) {
        DBG.Caught(e2);
      }
    }
  }
  */

  /*
  protected static final Monitor findMonitor(Monitor monitor) {
    Monitor mon = null;
    synchronized(list) {
      try {
        for(Iterator i = list.iterator(); i.hasNext();) {
          mon = (Monitor) (i.next());
          if(mon == monitor) {
            break;//return mon;
          }
        }
        if(mon != monitor) {
          mon = null;
        }
      } catch (Exception e2) {
        DBG.Caught(e2);
      }
    }
    return null;
  }
  */

  public int compareTo(Object o) {
    int i = 0;
    try {
      i = name.compareTo(((Monitor)o).name);
    } catch (Exception e) {
      /// +++ bitch
    }
    return i;
  }

}
/*
An example of mutexing via file locking:
import java.io.*;
public class exclude {
  public static void main(String a[]) throws Exception {
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

