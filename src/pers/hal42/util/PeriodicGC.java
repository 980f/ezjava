package pers.hal42.util;

import pers.hal42.logging.ErrorLogStream;
import pers.hal42.timer.Alarmer;
import pers.hal42.timer.Alarmum;
import pers.hal42.timer.TimeBomb;

import static pers.hal42.logging.LogLevelEnum.*;

public class PeriodicGC implements TimeBomb {
  Alarmum myperiod;
  static ErrorLogStream dbg;

  public boolean enabled(boolean beenabled){
    try {
      return dbg.levelIs(ERROR);
    }
    finally {
      dbg.setLevel(beenabled?VERBOSE: ERROR);
    }
  }

  public void onTimeout(){
    //Main.gc(dbg);//only does it if level is verbose
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

