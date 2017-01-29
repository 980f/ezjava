package pers.hal42.util.timer;

import pers.hal42.util.*;
import pers.hal42.*;

public class PeriodicGC implements TimeBomb{
  Alarmum myperiod;
  static ErrorLogStream dbg;

  public boolean enabled(boolean beenabled){
    try {
      return dbg.levelIs(LogSwitch.ERROR);
    }
    finally {
      dbg.setLevel(beenabled?LogSwitch.VERBOSE:LogSwitch.ERROR);
    }
  }

  public void onTimeout(){
    Main.gc(dbg);//only does it if level is verbose
    Alarmer.reset(myperiod.ticks,myperiod);
  }

  public static PeriodicGC Every(long ticks){
    return new PeriodicGC((int)ticks);
  }

  private PeriodicGC(int periodinticks) {
    if(dbg==null) dbg=ErrorLogStream.getForClass(PeriodicGC.class);
    myperiod=Alarmer.New(periodinticks,this);
  }

}

