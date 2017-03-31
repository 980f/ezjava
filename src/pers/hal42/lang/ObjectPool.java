package pers.hal42.lang;

import  java.util.Hashtable;

public class ObjectPool extends Hashtable {

  private Hashtable table = new Hashtable();

  private int incr = -1;
  public String setObject(Object rs) {
    String retval = null;
    if(rs != null) {
      Monitor mon = new Monitor("ObjectPool");//TODO:0 THIS SERVES NO Purpose, we need to lock incrementing the counter and returning it as a string.
      try {
        mon.getMonitor();
        // if(containsValue(stmt) // presume it isn't in here for now; code the check later +++
        incr++;
        retval = "" + incr;
        table.put(retval, rs);
      } catch (Exception e) {
      } finally {
        if(mon != null) {
          mon.freeMonitor();
        }
      }
    }
    return retval;
  }
  public Object getObject(String key) {
    Object rs = table.get(key);
    table.remove(key);
    return rs;
  }

  private String name = "AnonymousObjectPool";

  public String name() {
    return name;
  }

  public ObjectPool(String name) {
    this.name = name;
  }

  public Hashtable cloneList() {
    return (Hashtable)table.clone();
  }
}
