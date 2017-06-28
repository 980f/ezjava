package pers.hal42.lang;

import org.jetbrains.annotations.NotNull;
import pers.hal42.stream.IOX;
import pers.hal42.text.TextList;
import pers.hal42.util.Executor;

import java.io.File;

public class OS {
  /**
   * this singlet  was public because we are too lazy to wrap the TrueEnum features such as Value().
   */
  private static OsEnum os;
  ////////////////////////////////
  // smart questions
  private static String TEMPROOT = null;
  public static final String EOL = System.getProperty("line.separator");
  public static final String LOGPATHKEY = "logpath";
  // can't create this dir, and logic fails to fall back on tmp private static final String DATALOGS = "/data/logs"; /// constant for servers
  private static final String DATALOGS = "/tmp";
  private static final String SLASHTMP = "/tmp"; /// constant for appliances



  // restore as needed:
//  public static boolean isNT() {
//    return os().is(OsEnum.NT) || os.is(OsEnum.Windows2000); // includes Win2K, in behaviour
//  }
//
//  public static boolean isWin2K() {
//    return os() == OsEnum.Windows2000;
//  }

  /**
   * gets best guess at OS from java properties.
   */
  public static OsEnum os() {
    if (os == null) {
      String oser = System.getProperty("os.name", "");
      for (OsEnum guess : OsEnum.values()) {
        if (oser.startsWith(guess.toString())) {
          os = guess;
          return os;
        }
      }
      System.out.println("Guessing Linux, Unknown OS:" + oser);
      os = OsEnum.Linux;
    }
    return os;
  }
//
//  public static boolean isEmbedded(){
//    return isLinux();//at present we are detecting only our own brand of linux
//  }

//  ///////////////////////
//  // com port naming
//  public static String comPath(boolean isUsb, int portindex) {
//    if (isWin2K()) {//Andy has the only extant win2k server. Keyspan starts at com3
//      if (portindex < 0) {
//        return "NUL"; //may not work as a comport
//      }
//      if (isUsb) {
//        int usbBias = 3;      //really should read some property somewhere for this bias
//        return "COM" + (portindex + usbBias);
//      } else {
//        return "COM" + portindex;
//      }
//    } else {
//      if (portindex < 0) {
//        return "/dev/null"; //may not work as a comport
//      }
//      if (isUsb) {
//        return "/dev/ttyUSB" + portindex; //look into devfs +_+
//        //we could have made the above /dev/nodes in a uniform namespace with the regular ones, sigh.
//      } else {
//        return "/dev/ttyS" + portindex;
//      }
//    }
//  }

  ///////////////////////
  // extra crap

  public static String OsName() {
    return os().toString();
  }

  public static boolean isLinux() {
    return os() == OsEnum.Linux;
  }

  public static boolean isWindows() {
    return os() == OsEnum.Windows;
  }

  //  public static boolean isSolaris() {
//    return os().is(OsEnum.SunOS);
//  }
//
  public static boolean isUnish() {
    return isLinux();//|| isSolaris();
  }

  // synchronize to prevent multiple accesses/sets
  public static synchronized String TempRoot() {
    // if we haven't set it yet, do now ...
    if (TEMPROOT == null) {
      String tmp = System.getProperty(LOGPATHKEY, "");
      if (StringX.NonTrivial(tmp)) {
        TEMPROOT = tmp;
      } else {
        // if this is not linux, it is definitely a production server!
        if (!isLinux()) {
          TEMPROOT = DATALOGS;
        } else {
          //given that we only have a few servers ever why couldn't they have used the logpath system property???
          // this might be an appliance and it might be a development server.
          // development servers need to use /data/logs
          // appliances need to use /tmp
          // check to see if /data/logs exists.  If so, use it.  Otherwise, use /tmp.
          File datalogsdir = new File(DATALOGS);
          if (datalogsdir.exists()) {
            TEMPROOT = DATALOGS;
          } else {
            TEMPROOT = SLASHTMP;
          }
        }
      }
    }
    IOX.createDir(TEMPROOT);//ensure it exists.
    return TEMPROOT;
  }

  @NotNull
  public static File TempFile(String particular) {
    return new File(TempRoot(), particular);
  }

  /**
   * execute df -k and return its output
   */
  public static int diskfree(String moreParams, TextList msgs) {
    int timeout = 5;
    int displayrate = 1;
    return Executor.runProcess("df -k " + StringX.TrivialDefault(moreParams, ""), "", displayrate, timeout, msgs);
  }

}

