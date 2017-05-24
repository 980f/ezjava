package pers.hal42.thread;


import org.jetbrains.annotations.Contract;
import pers.hal42.lang.ReflectX;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.logging.LogLevelEnum;
import pers.hal42.timer.Ticks;

import java.util.Comparator;

import static java.util.Comparator.reverseOrder;

/**
 * Message Queue processor component
 * todo: should check for null actor. would simplify startup code for users of this class.
 */
public class QAgent<Qtype extends Comparable<Qtype>> implements Runnable {
  protected final PrioritizedQueue<Qtype> fifo;//accessible for input filtering based upon present content
  protected Thread thread;
  /** this entity receives things that are put into the queue, one at a time.*/
  private QActor<Qtype> actor;
  private ErrorLogStream dbg = classdbg;
  // +++ make this an enumeration (except killed)
  private boolean amStopped = true;
  private boolean killed = false; //need class cleanThread
  private boolean paused = false;
  private final String myname;
  private final Waiter waitingForInput;
  /** if the queue is empty then this object (if not null) is passed to the actor */
  private Qtype idleObject = null;
  private static final long failsafeKeepAlive = Ticks.forSeconds(0.01);// a zero keepalive is heinous
  private static ErrorLogStream classdbg;

  /**
   * @param threadname     used solely for debugging
   * @param actor          handles objects taken from storage
   * @param ordering       can be null if the incoming objects are NEVER Comparable
   * @param threadPriority sets the new thread's priority
   */
  public QAgent(String threadname, QActor<Qtype> actor, Comparator<Qtype> ordering, int threadPriority) {
    if (classdbg == null) {
      classdbg = ErrorLogStream.getForClass(QAgent.class);
      if (dbg == null) { // will for the first one created!
        dbg = classdbg;
      }
    }
    myname = threadname;
    thread = new Thread(this, myname);
    thread.setPriority(threadPriority);
    waitingForInput = Waiter.Create(0, false, ErrorLogStream.Null());
    killed = false;
    thread.setDaemon(true);
    this.actor = actor;
    fifo = new PrioritizedQueue<>(ordering);
  }

  public QAgent config(double keepaliveSecs) {//consider mutexing
    return config(Ticks.forSeconds(keepaliveSecs));
  }

  public QAgent config(long keepaliveTix) {//consider mutexing
    if (keepaliveTix <= 0) {
      keepaliveTix = failsafeKeepAlive;
    }
    waitingForInput.set(keepaliveTix);
    return this;
  }

  public QAgent config(ErrorLogStream dbg) {//consider mutexing
    this.dbg = ErrorLogStream.NonNull(dbg);
    dbg.VERBOSE("debugging QAgent:" + thread.getName());
    waitingForInput.dbg = dbg;
    return this;
  }

///////////////////////////

  public QAgent setIdleObject(Qtype demon) {
    idleObject = demon;//will run periodically when there is nothing else to do.
    return this;
  }

  /**
   * user beware, someone else may try to start it.
   * this really should only be called by the person who creates the qagent.
   */
  public final boolean isStopped() {
    return amStopped;
  }

  public final boolean isPaused() {
    return paused;
  }

  public final boolean Post(Qtype arf) {//primary access point
    return put(arf) > 0;
  }

  /** call this when you are ready to wait, which is sometime after prepare AND triggering the agent whose action you are awaiting.
   */
  public void run() {//implements Runnable
    amStopped = false;
    waitingForInput.ensure(failsafeKeepAlive);//we will spin hard if wait time is too small.\
    try (AutoCloseable pop=dbg.Push(myname)){
      while (!killed) {
        try {
          waitingForInput.prepare();
          if (paused) {
            waitingForInput.finishWaiting();//todo:0 test this, or don't pause. former (lack of) code probably spun hard burning cpu cycles.
          } else {
            Qtype todo = fifo.next();
            if (todo != null) {
              dbg.VERBOSE(myname + " about to runone");
              actor.runone(todo);
            } else { //wait awhile to keep from sucking up processor cycles
              if (waitingForInput.run() == Waiter.State.Timedout) {//if we wait for a full idle period then
                if (idleObject != null) {//indicate we have been idle
                  dbg.VERBOSE(myname + " about to run idle object");
                  todo = idleObject;//record idleObject as active element for debug
                  actor.runone(todo);
                }
              }
            }
          }
        } catch (Exception any) {
          dbg.Caught(any);//@todo:2 add some info about 'todo' object, but without potentially causing nested exceptions.
        } finally {
          if (killed) {
            actor.Stop(); //let the actor know that we will no longer be delivering objects.
          }
        }
      }
    } catch (Throwable panic) {//don't stop application just because one agent croaks.
      dbg.Caught(panic);
    } finally {
      amStopped = true;
    }
  }

  public synchronized void Pause() {
    paused = true;
  }

  public synchronized void Resume() {
    paused = false;
    waitingForInput.Stop();
  }

  public synchronized void Start() {
    if (amStopped) {
      fifo.Clear();
      try {
        dbg.ERROR("start thread:" + thread.getName());
        thread.start(); // !!! --- can't start this thread if it was ever run before !!!
      } catch (IllegalThreadStateException ignore) {
        dbg.WARNING("QAgent.Start():" + String.valueOf(ignore));
      }
    } else {
      dbg.VERBOSE("gratuitous attempt to start a running agent");
    }
  }

  private boolean inputNotify() {
    return waitingForInput.Stop();//stop waiting
  }

  //one upon a time the "puts" were protected.
  //see new class 'OrderedVector' for intended cleanup of this class's public interface.
  protected int put(Qtype obj) {
      int size = fifo.put(obj);//note: the object may be pulled from the fifo by the time put returns.
      boolean didnot = inputNotify();
      if (didnot) {
        dbg.WARNING("inputNotify() did NOT have an effect.");
      }
      return size;
  }

  /**
   * @param obj defines uniqueness via .equals(obj)
   *            this version always replaces old with new.
   * @return true if overwrote rather than added
   */
  public boolean PostUnique(Qtype obj) {
    synchronized (fifo) {//#need to synch the remove against the put
      boolean retval = removeAny(obj) != 0;
      put(obj);
      return retval;
    }
  }

  public int removeAny(Qtype obj) {
      return fifo.removeAny(obj);
  }

  // once you call this function, you can never call it again
  public synchronized boolean Stop() /*throws QAgentStopper*/ {
    killed = true;
    if (!amStopped) {
//#NO! restarts thread!    Clear();
      //the following was added to remove these objects from post-mortem memory leak analysis.
      fifo.Clear(); //release objects upon stop. This is NOT a pause :P
      //since we haven't remembered to call Stop() upon orderly program death the above line is at present moot.
      try {
        dbg.WARNING("Stopped(): Trying to stop");
        thread.interrupt();
      } catch (Exception e) {
        dbg.Caught(e);
      }
    } else {
      dbg.VERBOSE("Stopped(): already killed and stopped");
    }
    return Stopped(); //first attempt to stop...
  }
  /**
   * @return whether run loop has exited
   */
  public boolean Stopped() {//and try to make it stop.
    return amStopped;
  }

  /**
   * This exists for reporting purposes only.  DO NOT use it to iterate!
   */
  public final int Size() {
    return fifo.Size();
  }

  public String status() {
    return waitingForInput.toSpam();
  }

  // defaults the ordering to Reversed()
  public static <Qtype extends Comparable<Qtype>> QAgent<Qtype> New(String threadname, QActor<Qtype> agent) {
    return New(threadname, agent, Comparator.reverseOrder());
  }

  // defaults the ThreadPriority to that of the current thread
  public static <Qtype extends Comparable<Qtype>> QAgent<Qtype> New(String threadname, QActor<Qtype> agent, Comparator<Qtype> ordering) {
    return New(threadname, agent, ordering, Thread.currentThread().getPriority());
  }

  public static <Qtype extends Comparable<Qtype>> QAgent<Qtype> New(String threadname, QActor<Qtype> agent, Comparator<Qtype> ordering, int threadPriority) {
    return new QAgent<>(threadname, agent, ordering, threadPriority);
  }

}

