/**
 * Synch's were removed as being gratuitous protection against ridiculous activity.
 * If a particular instance is CONTROLLED by different threads then behavior is
 * already impossible to make any good sense out of. The only remaining reason to
 * Synch is to ensure that "long" is atomic. That can be done by multiple reads at
 * far less expense than locks.
 * Synch'ing is only needed for non-atomic data that might get written while it is being read.
 * @todo: add registry so that we can do the same adjustments done by Alarmer. (true for anything that uses DateX)
 */

package pers.hal42.timer;


import pers.hal42.lang.DateX;

public class StopWatch {
  long started;
  long stopped;
  boolean running;

  public boolean isRunning(){
    return running; //is atomic so no synch needed.
  }

  public long startedAt(){
    return started;
  }

  public double seconds(){ //can be read while running
    return ((double)millis())/ Ticks.perSecond;
  }

  public long millis(){ //can be read while running
    return (running ? DateX.utcNow() : stopped) -started;
  }

  public void Start(){
    stopped=started=DateX.utcNow();
    running=true;
  }

  public long Stop(){
    if(running){
      stopped=DateX.utcNow(); // +++ potentially make this Math.max(started, DateX.utcNow());??WHY
      running=false;
    }
    return millis();
  }

  public void Reset(){
    running=false;
    stopped=started=0;
  }

  public StopWatch(boolean hitTheFloorRunning) {
    Reset();
    if (hitTheFloorRunning) {
      Start();
    }
  }

  public StopWatch() {
    this(true);
  }
}

