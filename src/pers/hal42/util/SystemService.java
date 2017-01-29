package pers.hal42.util;

import pers.hal42.util.timer.*; // StopWatch
import pers.hal42.lang.ThreadX;
import pers.hal42.text.Formatter;
// +++ Separate the Service stuff into more "control" type structures and "interface" type structures, and put control in .data or .util or .service, or distribute them, and put the "display" portions in a presentation package, like .web!
//import org.apache.ecs.html.*;// A

public class SystemService extends Service {

  private final StopWatch uptime= new StopWatch();
  public long uptime() {
    return uptime.millis();
  }
  public long upsince() {
    return uptime.startedAt();
  }
  public static final String NAME = "System";
  public SystemService(ServiceConfigurator cfg) {
    super(NAME, cfg);
  }
  public void down() {
    // stub
  }
  public void up() {
    // stub
  }
  public boolean isUp() {
    return true; // always, since if this method will run, the system is up!
  }

  /**
   * Number of processes
   */
  public String svcCnxns() {
    return "1"; // for the number of processes running here
  }

  /**
   * Number of threads
   */
  public String svcTxns() {
    return ""+ThreadX.ThreadCount();
  }

  /**
   * Uptime
   */
  public String svcAvgTime() {
    return DateX.millisToTime(uptime());
  }

  /**
   * Revision info
   */
  public String svcNotes() {
    long freemem = Runtime.getRuntime().freeMemory();
    long ttlmem = Runtime.getRuntime().totalMemory();
    String linkText = "" + (freemem * 100 / ttlmem) + "% freemem [" + of(Formatter.sizeLong(freemem), Formatter.sizeLong(ttlmem)) + " B]";
    return linkText;
  }

}
