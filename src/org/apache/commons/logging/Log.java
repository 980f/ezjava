package org.apache.commons.logging;

import pers.hal42.logging.ErrorLogStream;
import pers.hal42.logging.LogLevelEnum;

public class Log {
  ErrorLogStream dbg;

  public Log(Class aClass) {
    dbg = ErrorLogStream.getForClass(aClass);
  }

  public void warn(Throwable e, Throwable other) {
    //todo:
    dbg.Caught(e, "Paired with {0}", other);
  }

  public void warn(String s, Throwable ex) {
    if (ex != null) {
      dbg.Caught(ex, s);
    } else {
      warn(s);
    }
  }

  public void warn(String s) {
    dbg.WARNING(s);
  }
  public boolean isDebugEnabled() {
    return dbg.levelIs(LogLevelEnum.VERBOSE);
  }

  public void error(String s, Throwable ex) {
    if (ex != null) {
      dbg.Caught(ex, s);
    } else {
      error(s);
    }
  }


  public void error(String s) {
    dbg.ERROR(s);
  }

  public void error(Throwable e, Throwable other) {
    //todo:
    dbg.Caught(e, "Paired with {0}", other);
  }

  public void debug(String s, Throwable ex) {
    if (ex != null) {
      dbg.Caught(ex, s);
    } else {
      dbg.VERBOSE(s);
    }
  }

  public void info(String s) {
    dbg.VERBOSE(s);
  }
}
