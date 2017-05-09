package pers.hal42.thread;

/**
 * @todo syncronized methods should be changed to synch on fifo.
 * @todo: investigate using a Server as the thread
 * @todo: should check for null actor. would simplify startup code for users of this class.
 */

import pers.hal42.lang.ReflectX;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.logging.LogLevelEnum;
import pers.hal42.timer.Ticks;

import java.util.Comparator;

public class QAgent implements Runnable {
  private static ErrorLogStream classdbg;

  private static final long failsafeKeepAlive= Ticks.forSeconds(100);// a zero keepalive is heinous

  private QActor actor;
  protected PrioritizedQueue fifo;//accessible for input filtering based upon present content

  private ErrorLogStream dbg=classdbg;

  // +++ make this an enumeration (except killed)
  private boolean amStopped=true;
  private boolean killed=false; //need class cleanThread
  private boolean paused=false;
  protected Thread thread;

  private String myname;
  private Waiter waitingForInput;
  private Object idleObject=null;

  public QAgent config(double keepaliveSecs){//consider mutexing
    return config(Ticks.forSeconds(keepaliveSecs));
  }

  public QAgent config(long keepaliveTix){//consider mutexing
    if(keepaliveTix<=0){
      keepaliveTix=failsafeKeepAlive;
    }
    waitingForInput.set(keepaliveTix);
    return this;
  }

  public QAgent config(ErrorLogStream dbg){//consider mutexing
    this.dbg=ErrorLogStream.NonNull(dbg);
    dbg.VERBOSE("debugging QAgent:"+thread.getName());
    waitingForInput.dbg=dbg;
    return this;
  }

  public QAgent setIdleObject(Object demon){
    idleObject=demon;//will run periodically when there is nothing else to do.
    return this;
  }

///////////////////////////
  /**
   * user beware, someone else may try to start it.
   * this really should only be called by the person who creates the qagent.
   */
  public final boolean isStopped(){
    return amStopped;
  }

  public final boolean isPaused() {
    return paused;
  }

  public final synchronized boolean Post(Object arf){//primary access point
    return put(arf)>0; //not checking types yet, just whether there is room in fifo
  }

  /**
   * @todo try to close the hole. Hole begins after the fifo.next() and ends @ waitOn().
   * this can be done by copying out a piece of waitOn.
   * can we synchronize on inputWakeupLock, then wait on it in waitOn?
   * i.e. will the release in waitOn release all synchronizations on that object?
   * Need an example of what you think is a hole, I.e. waht can happen in that hole that is not
   * acceptible?
   *
   */
  public void run(){//implements Runnable
    // @IPFIX@ if there is an exception in lines 2 & 3, the thread dies!!!  put them ALL in the try!
    // alh: those lines can't except unless 'this' is grossly defective. Moving them would't hurt.
    amStopped=false;
    if(waitingForInput.millisecs<=0){//we will spin hard if this is true!
      waitingForInput.set(failsafeKeepAlive);
    }
    try{
      dbg.VERBOSE(myname+" run() entered ");
      Object todo=null;
      while(!killed){ // @IPFIX@ if this loop dies, the thread dies.  put the try inside the loop ???  kill the whole program instead (restart) ???
        try {
          waitingForInput.prepare();
          //even though the above clears the input notified flag, the following
          //checks whether there is input.
          //alh:Maybe this closes the hole mentioned in the javadoc?
          todo=null;
          if( ! paused){
            todo=fifo.next();
            if(todo!=null){
              dbg.VERBOSE(myname+" about to runone");
              actor.runone(todo);
            } else { //wait awhile to keep from sucking up processor cycles
              if(waitingForInput.run()==Waiter.Timedout){//if we wait for a full idle period then
                if(idleObject!=null){//indicate we have been idle
                  dbg.VERBOSE(myname+" about to run idle object");
                  actor.runone(todo=idleObject);//record idleObject as active element for debug
                  //queueing the object leads to timing ambiguities if the fifo is a prioritized queue rather than a simple fifo.
                }
              }
            }
          }
        } catch (Exception any) {
          dbg.Caught(any);//@todo: add some info about 'todo' object, but without potentially causing nested exceptions.
        } finally {
          if(killed) {
            actor.Stop(); //let the actor know that we will no longer be delivering objects.
          }
        }
      }
    } catch (Throwable panic){
      dbg.Caught(panic);
    } finally {
      amStopped=true;
      dbg.VERBOSE(myname+" run() exits");
    }
  }

  public synchronized void Pause() {
    paused = true;
  }

  public synchronized void Resume() {
    paused = false;
    waitingForInput.Stop();
  }

  public synchronized void Start(){
    if(amStopped) {
      fifo.Clear();
      // +++ have this guy reload configuration, etc? -- what configuration?
      try {
        dbg.ERROR("start thread:" + thread.getName());
        thread.start(); // !!! --- can't start this thread if it was ever run before !!!
      }
      catch (IllegalThreadStateException ignore) {
        dbg.WARNING("QAgent.Clear():" + String.valueOf(ignore));
      }
    } else {
      // +++ bitch
    }
  }

  private boolean inputNotify(){
    return waitingForInput.Stop();//stop waiting
  }

  //one upon a time the "puts" were protected.
  //see new class 'OrderedVector' for intended cleanup of this class's public interface.
  protected synchronized int put(Object obj){
    if(dbg.willOutput(LogLevelEnum.VERBOSE.ordinal())){
      dbg.VERBOSE("Posting:"+ReflectX.ObjectInfo(obj));
    }
    int size=fifo.put(obj);
    boolean didnot = inputNotify();
    if(didnot) {
      dbg.WARNING("inputNotify() did NOT have an effect.");
    }
    return size;
  }

  /**
   * @return true if overwrote rather than added
   * @param obj defines uniqueness via .equals(obj)
   * this version always replaces old with new.
   */
  public synchronized boolean putUnique(Object obj){
    boolean retval = removeAny(obj)!=0;
    put(obj);
    return retval;
  }

  public synchronized int removeAny(Object obj){
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

  // +++ add a pause/resume feature where the thread doesn't runone, but just sleeps over and over again?  this causes the queue to fill, but nothing to be done about the items.
  // +++ separate start/clear
  // +++ create a clearAndStart()

  /**
   * @param threadname used solely for debugging
   * @param actor handles objects taken from fifo
   * @param ordering can be null if the incoming objects are NEVER Comparable
   * @param threadPriority sets the new thread's priority
   */
  protected QAgent (String threadname, QActor actor, Comparator ordering, int threadPriority){
    if(classdbg==null) {
      classdbg = ErrorLogStream.getForClass(QAgent.class);
      if(dbg == null) { // will for the first one created!
        dbg = classdbg;
      }
    }
    myname=threadname;
    thread=new Thread(this,myname);
    thread.setPriority(threadPriority);
    waitingForInput=Waiter.Create(0,false,ErrorLogStream.Null());
    killed=false;
    thread.setDaemon(true);
    this.actor=actor;
    fifo=new PrioritizedQueue(ordering);
  }

  // defaults the ordering to PriorityComparator.Reversed()
  public static QAgent New(String threadname, QActor agent){
    return New(threadname, agent, PriorityComparator.Reversed());
  }

  // defaults the ThreadPriority to that of the current thread
  public static QAgent New(String threadname, QActor agent, Comparator ordering){
    return New(threadname, agent, ordering, Thread.currentThread().getPriority());
  }

  public static QAgent New(String threadname, QActor agent, Comparator ordering, int threadPriority){
    return new QAgent(threadname, agent, ordering, threadPriority);
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

}

