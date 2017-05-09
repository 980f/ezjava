package pers.hal42.logging;

import pers.hal42.util.PrintFork;

import java.util.Vector;

public class LogSwitchRegistry {

  private LogSwitchRegistry() {
    //there can be only one.
  }

  //originally this tolerated freefloating LogLevelEnum but that is now a java enum and doesn't work well.
  public static final Vector<LogSwitch> registry = new Vector<>(100, 100);
  public static final Vector<PrintFork> printForkRegistry = new Vector<>(100, 100);

}
