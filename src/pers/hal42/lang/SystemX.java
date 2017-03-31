package pers.hal42.lang;

public class SystemX {
  public static String gcMessage(String instigator){
    StringBuffer msg=new StringBuffer(100);
    msg.append("gc() by ");
    msg.append(instigator);
    msg.append(" from:"+memused());
    System.gc();
    msg.append(" to:"+memused());
    return msg.toString();
  }

  public static long memused(){
    return Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
  }

}
