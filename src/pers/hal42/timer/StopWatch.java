package pers.hal42.timer;

import pers.hal42.lang.DateX;

/**
 * No sycnhronized's are used as they would be gratuitous protection against ridiculous activity.
 * If a particular instance is CONTROLLED by different threads then behavior is
 * already impossible to make any good sense out of. The only remaining reason to
 * synch is to ensure that "long" is atomic. If that is important can do  multiple reads at
 * far less expense than locks.
 * Synch'ing is only needed for non-atomic data that might get written while it is being read.
 *
 * todo:2 add registry so that we can do the same adjustments done by Alarmer. (true for anything that uses DateX)
 */
public class StopWatch {
  long started;
  long stopped;
  boolean running;

  /**
   * @param hitTheFloorRunning is whether to start the timer as part of construction.
   */
  public StopWatch(boolean hitTheFloorRunning) {
    Kill();
    if (hitTheFloorRunning) {
      Start();
    }
  }

  public StopWatch() {
    this(true);
  }

  public boolean isRunning() {
    return running; //is atomic so no synch needed.
  }

  public long startedAt() {
    return started;
  }

  /** @returns elapsed time as seconds. */
  public double seconds() { //can be read while running
    return ((double) millis()) / Ticks.perSecond;
  }

  /** @returns elapsed time as milli-seconds. */
  public long millis() { //can be read while running
    return (running ? DateX.utcNow() : stopped) - started;
  }

  /**
   * @returns elapsed time in ms, and (re)starts timer.
   */
  public long roll() {
    try {
      return millis();
    } finally {
      Start();
    }
  }

  /** starts counting. */
  public void Start() {
    stopped = started = DateX.utcNow();
    running = true;
  }

  /** @returns elapsed time and stops counting */
  public long Stop() {
    if (running) {
      stopped = DateX.utcNow(); // +++ potentially make this Math.max(started, DateX.utcNow());??WHY
      running = false;
    }
    return millis();
  }

  /**
   * forget you may have ever been interested in a time
   */
  public void Kill() {
    running = false;
    stopped = started = 0;
  }
}

