package pers.hal42.timer;

import pers.hal42.lang.DateX;
import pers.hal42.lang.ReflectX;

public class Alarmum {
/**
 * what to do when time is up
 */
  TimeBomb dynamite=null;
  /**
   * when true dynamite should not be executed.
   */
  boolean defused=false;
  /**
   * absolute time that dynamite should execute
   */
  long blowtime;//gets compared to System.currentTimeMillis()
  int ticks=0; //what we were asked to wait for
  /**
   * to execute dyanmite at priority that was active when alarm was set.
   */
  int ThreadPriority;
  /**
   * true when on some Alarmer's active list
   */
   boolean listed=false;


  private Alarmum(){
    //for cloner
  }

  /**
   * you should discard this object after getting status info from it.
   * despite being marked listed it is not linked to a list.
   */
  public Alarmum Clone(){
    Alarmum clown=new Alarmum();
    clown.dynamite        = this.dynamite;
    clown.defused         = this.defused;
    clown.blowtime        = this.blowtime;
    clown.ticks           = this.ticks;
    clown.ThreadPriority  = this.ThreadPriority;
    clown.listed          = this.listed;//looks screwy but we are getting a snapshot, see method header
    return clown;
  }

  boolean blow(){
    dynamite.onTimeout();
    return false; //onTimeouts not yet upgraded for periodic stuff.
  }

  void defuse(){
    defused=true;
    listed=false;//COB
  }

  boolean isEnabled(){
    return blowtime>0 && !defused;
  }

  boolean isTicking(){
    return listed;
  }

  /**
   * for human display only, not totally reliable (may not be live)
   */
  int timeleft(){
    return (int)(blowtime- DateX.utcNow());
  }

  Alarmum refuse(int fuse){
    ticks=fuse; //record f4debug
    defused= ticks<=0;
    blowtime= defused? 0:ticks+DateX.utcNow();
    return this;
  }

  public Alarmum(TimeBomb dynamite, int fuse, int priority) {
    this.dynamite=dynamite;
    refuse(fuse);
    ThreadPriority = priority;
  }

  public Alarmum(TimeBomb dynamite, int fuse) {
    this(dynamite, fuse,Thread.currentThread().getPriority());
  }

  public String toSpam(){
    return "Ticking:"+isTicking()+" expires in:"+timeleft()+" type:"+ReflectX.shortClassName(dynamite)+" "+String.valueOf(dynamite);
  }

}

