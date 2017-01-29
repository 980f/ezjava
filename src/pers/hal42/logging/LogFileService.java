package pers.hal42.logging;


import pers.hal42.lang.MathX;
import pers.hal42.io.LogFile;
import java.lang.Thread;
import pers.hal42.lang.ThreadX;

public class LogFileService extends Service implements Runnable {

  public static final String NAME = "LogFile";

  private static LogFileService one = null;
  public static ServiceConfigurator onecfg = null;
  public static LogFileService one() {
    if(one == null) {
      one = new LogFileService(onecfg);
    }
    return one;
  }

  private LogFileService(ServiceConfigurator onecfg) {
    super(NAME, onecfg);
    up();
  }
  public String svcCnxns() {
    return ""+LogFile.lflistSize();
  }
  public String svcTxns() {
    return ""+LogFile.writes.getCount();
  }
  public String svcPend() {
    return ""+LogFile.allPending();
  }
  public String svcAvgTime() {
    return DateX.millisToSecsPlus((int)MathX.ratio(LogFile.writeTimes.getTotal(), LogFile.writes.getCount()));
  }
  public String svcWrites() {
    return printByteStats(LogFile.writes);
  }
  public String svcNotes() {
    return diskSpaceFree(LogFile.getPath())+" ["+LogFile.getPath()+"]";
  }
  // +++ do something with these?
  public boolean isUp() {
    return true;
  }
  public void down() {
    shouldRun = false;
    Thread localthread = thread;
    if(localthread != null) {
      localthread.interrupt();
    }
    thread = null;
  }

  private Thread thread = null;
  private boolean shouldRun = false;
  private int sleepMillis = 1000;

  public void up() {
    // this line reloads the config!
    sleepMillis = (configger != null) ?
        configger.getIntServiceParam(serviceName(), "sleepMillis", sleepMillis) :
        sleepMillis;
    if(!shouldRun) {
      thread = new Thread(this, "LogFile");
      thread.setDaemon(true);
      shouldRun = true; // must be BEFORE the thread.start() line!
      thread.start();
    }
    System.out.println("LogFileService brought up.  shouldRun="+shouldRun);
  }

  public void run() {
    while(shouldRun) {
      ThreadX.sleepFor(sleepMillis);
//      System.out.println("LogFileService.run() woke from " + sleepMillis + " sleep.  About to runThemAll...");
      LogFile.runThemAll();
    }
  }

/* down()?
      thread.setPriority(Thread.MAX_PRIORITY); // --- questionable activity
thread.interrupt();
try {
  thread.join(); // waits for the thread to die //+_+ needs timeout
} catch (Exception e) {
  // stub
}
*/


}

