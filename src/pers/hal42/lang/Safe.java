package pers.hal42.lang;


import pers.hal42.logging.ErrorLogStream;
import pers.hal42.timer.LocalTimeFormat;
import pers.hal42.util.Executor;

import java.io.FileOutputStream;
import java.util.Date;

public class Safe {

  /**
   * this is in Safe because it uses some packages that the more obvious pakages for it otherwise aren't needed
   */
  static final LocalTimeFormat LinuxDateCommand = LocalTimeFormat.Utc("MMddHHmmyyyy.ss");

  /**
   * if 1st object is null return second else return it. Kotlin has an operator for this.
   */
  public static <T> T deNull(T nullish, T def) {
    return (nullish != null) ? nullish : def;
  }

  public static int enumSize(Class<? extends Enum> eclass) {
    return eclass.getEnumConstants().length;
  }

  public static int enumSize(Enum eg) {
    return enumSize(eg.getClass());
  }

  public static void setSystemClock(long millsfromepoch) { //exec something to set the system clock
    String forlinuxdateprogram = LinuxDateCommand.format(new Date(millsfromepoch));   //todo:1 move this line into DateX
    ErrorLogStream.Global().ERROR("setting time to:" + forlinuxdateprogram);
    String progname = pers.hal42.lang.OS.isUnish() ? "setClock " : "setClock.bat ";
    // Executor.runProcess("date -s -u "+busybox.format(now),"fixing clock",0,0,null,false);
    Executor.ezExec(progname + forlinuxdateprogram, 0);
  }

  public static FileOutputStream fileOutputStream(String filename) {
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(filename);
    } catch (Exception e) {
      // +++ bitch
    }
    return fos;
  }


}

