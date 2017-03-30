package pers.hal42.lang;

import org.jetbrains.annotations.NotNull;
import pers.hal42.text.TextList;

import java.io.File;


public class OS {
  /**
   * this singlet  was public because we are too lazy to wrap the TrueEnum features such as Value().
   */
  private static OsEnum os;

  public static final String EOL = System.getProperty("line.separator");

  public static String signature[];

  static {//with each OS we manipulate this list of names to recognize it
    signature = new String[OsEnum.numValues()];
    signature[OsEnum.Linux.ordinal()] = "Linux";//'none of the others' gets this
    signature[OsEnum.NT.ordinal()] = "NT"; //bogus, haven't ever tried this on a true NT system
    signature[OsEnum.Windows.ordinal()] = "Windows"; //
    signature[OsEnum.Windows2000.ordinal()] = "Windows 2000";
    signature[OsEnum.SunOS.ordinal()] = "SunOS";
  }

  /**
   * gets best guess at OS from java properties.
   */
  public static OsEnum os() {
    if (os == null) {
      //os = new OsEnum();//class contains this one instance of itself
      String oser = System.getProperty("os.name", "");// let's not do this so we don't have to refer OUT of util -> StringX.replace(Main.props("os").getString("name"), " ", "");
      //the above change hosed Andy's system's ability to use a keyspan. Thank you very much. If you can't stand the linkage then copy the code locally.
      //what alh did was to uncouple the text of the enumeration from the recognition string. we can now change the ennum text to whatever we wish.
      for (OsEnum guess : OsEnum.values()) {
        if (signature[guess.ordinal()].equals(oser)) {
          os = guess;
          return os;
        }
      }
      System.out.println("Guessing Linux, Unknown OS:" + oser);
      os = OsEnum.Linux;
    }
    return os;
  }

//  public static int OsValue() {
//    return os().ordinal();
//  }

  public static String OsName() {
    return signature[os().ordinal()];
  }
  ////////////////////////////////
  // smart questions

  public static boolean isLinux() {
    return os() == OsEnum.Linux;
  }

  public static boolean isWindows() {
    return os() == OsEnum.Windows; // really just 9X
  }

  // restore as needed:
//  public static boolean isNT() {
//    return os().is(OsEnum.NT) || os.is(OsEnum.Windows2000); // includes Win2K, in behaviour
//  }
//
  public static boolean isWin2K() {
    return os() == OsEnum.Windows2000;
  }

//  public static boolean isSolaris() {
//    return os().is(OsEnum.SunOS);
//  }
//
  public static boolean isUnish(){
    return isLinux();//|| isSolaris();
  }
//
//  public static boolean isEmbedded(){
//    return isLinux();//at present we are detecting only our own brand of linux
//  }

  ///////////////////////
  // com port naming
  public static String comPath(boolean isUsb, int portindex) {
    if (isWin2K()) {//Andy has the only extant win2k server. Keyspan starts at com3
      if (portindex < 0) {
        return "NUL"; //may not work as a comport
      }
      if (isUsb) {
        int usbBias = 3;      //really should read some property somewhere for this bias
        return "COM" + (portindex + usbBias);
      } else {
        return "COM" + portindex;
      }
    } else {
      if (portindex < 0) {
        return "/dev/null"; //may not work as a comport
      }
      if (isUsb) {
        return "/dev/ttyUSB" + portindex; //look into devfs +_+
        //we could have made the above /dev/nodes in a uniform namespace with the regular ones, sigh.
      } else {
        return "/dev/ttyS" + portindex;
      }
    }
  }

  ///////////////////////
  // extra crap

  public static final String LOGPATHKEY = "logpath";

  private static String TEMPROOT = null;
  private static final String DATALOGS = "/data/logs"; /// constant for servers
  private static final String SLASHTMP = "/tmp"; /// constant for appliances

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
    return TEMPROOT;
  }

  @NotNull
  public static File TempFile(String particular) {
    return new File(TempRoot(), particular);
  }

  /** execute df -k and return its output*/
  public static int diskfree(String moreParams, TextList msgs) {
//    String filename = "C:\\CYGWIN\\BIN\\df.exe";//+_+ move to OS specific classes
//    int timeout = 5;
//    int displayrate = 1;
//    if (StreamX.fileSize(filename) == 0) {
//      filename = "df";
//      timeout = -1;
//      displayrate = -1;
//    }
//    filename = filename + " -k " + StringX.TrivialDefault(moreParams, "");
//    int c = Executor.runProcess(filename, "", displayrate /* -1 */, timeout /* was never returning when set to -1 for my machine (not sure why) */, msgs);
//    return c;
    return 0;
  }

}

