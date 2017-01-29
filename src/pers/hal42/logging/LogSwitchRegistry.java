package pers.hal42.util;

import java.util.Vector;

public class LogSwitchRegistry {

  private LogSwitchRegistry() {
    // can't make one!
  }

  public static final Vector registry = new Vector(100,100);
  public static final Vector printForkRegistry = new Vector(100,100);

}
