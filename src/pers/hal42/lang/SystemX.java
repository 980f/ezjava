package pers.hal42.lang;

public class SystemX {
  public static String gcMessage(String instigator) {
    StringBuilder msg = new StringBuilder(100);
    msg.append("gc() by ").append(instigator).append(" from:").append(memused());
    System.gc();
    msg.append(" to:").append(memused());
    return msg.toString();
  }

  public static long memused() {
    return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
  }

}
