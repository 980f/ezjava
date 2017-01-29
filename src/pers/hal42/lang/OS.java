
import pers.hal42.io.IOX;
import pers.hal42.lang.StringX;
import java.io.File;
import java.util.*;


public class OS  {
  /**
     * this singlet  was public because we are too lazy to wrap the TrueEnum features such as Value().
     */
  private static OsEnum os;

  public static final String EOL=System.getProperty("line.separator");

  public static String signature[];
  static {//with each OS we manipulate this list of names to recognize it
    signature=new String[OsEnum.Prop.numValues()];
    signature[OsEnum.Linux]      ="Linux";//'none of the others' gets this
    signature[OsEnum.NT]         ="NT"; //bogus, haven't ever tried this on a true NT system
    signature[OsEnum.Windows]    ="Windows"; //
    signature[OsEnum.Windows2000]="Windows 2000";
    signature[OsEnum.SunOS]      ="SunOS";
  }

  /**
   * gets best guess at OS from java properties.
   */
  private static OsEnum os(){
    if(os==null){
      os = new OsEnum();//class contains this one instance of itself
      String oser = System.getProperty("os.name", "");// let's not do this so we don't have to refer OUT of util -> StringX.replace(Main.props("os").getString("name"), " ", "");
      //the above change hosed Andy's system's ability to use a keyspan. Thank you very much. If you can't stand the linkage then copy the code locally.
      //what alh did was to uncouple the text of the enumeration from the recognition string. we can now change the ennum text to whatever we wish.
      for(int osi=signature.length;osi-->0;){
        if (oser.indexOf(signature[osi]) >= 0){
          os.setto(osi);
          return os;
        }
      }
      System.out.println("Guessing Linux, Unknown OS:"+oser);
      os.setto(OsEnum.Linux);
    }
    return os;
  }

  public static final int OsValue(){
    return os().Value();
  }
  public static final String OsName(){
    return os().Image();//+_+ consider using signature here?
  }
  ////////////////////////////////
  // smart questions

  public static final boolean isLinux() {
    return os().is(OsEnum.Linux);
  }

  public static final boolean isWindows() {
    return os().is(OsEnum.Windows); // really just 9X
  }

  public static final boolean isNT() {
    return os().is(OsEnum.NT) || os.is(OsEnum.Windows2000); // includes Win2K, in behaviour
  }

  public static final boolean isWin2K() {
    return os().is(OsEnum.Windows2000);
  }

  public static final boolean isSolaris() {
    return os().is(OsEnum.SunOS);
  }

  public static final boolean isUnish(){
    return isLinux()|| isSolaris();
  }

  public static final boolean isEmbedded(){
    return isLinux();//at present we are detecting only our own brand of linux
  }

  ///////////////////////
  // com port naming
  public static final String comPath(boolean isUsb,int portindex){
    if(isWin2K()){//Andy has the only extant win2k server. Keyspan starts at com3
      if(portindex<0){
        return "NUL"; //may not work as a comport
      }
      if(isUsb){
        int usbBias=3;      //really should read some property somewhere for this bias
        return "COM"+(portindex+usbBias);
      } else {
        return "COM"+portindex;
      }
    } else {
      if(portindex<0){
        return "/dev/null"; //may not work as a comport
      }
      if(isUsb){
        return "/dev/ttyUSB"+portindex; //look into devfs +_+
        //we could have made the above /dev/nodes in a uniform namespace with the regular ones, sigh.
      } else {
        return "/dev/ttyS"+portindex;
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
  public static final synchronized String TempRoot(){
    // if we haven't set it yet, do now ...
    if(TEMPROOT == null) {
      String tmp = System.getProperty(LOGPATHKEY, "");
      if(StringX.NonTrivial(tmp)) {
        TEMPROOT = tmp;
      } else {
        if(!isUnish()) {
          TEMPROOT = "C:\\paymate.tmp"; // windows (yuk)
        } else {
          // if this is not linux, it is definitely a production server!
          if(!isLinux()) {
            TEMPROOT = DATALOGS;
          } else {
            //given that we only have a few servers ever why couldn't they have used the logpath system property???
            // this might be an appliance and it might be a development server.
            // development servers need to use /data/logs
            // appliances need to use /tmp
            // check to see if /data/logs exists.  If so, use it.  Otherwise, use /tmp.
            File datalogsdir = new File(DATALOGS);
            if(datalogsdir.exists()) {
              TEMPROOT = DATALOGS;
            } else {
              TEMPROOT = SLASHTMP;
            }
          }
        }
      }
    }
    return TEMPROOT;
  }

  public static final File TempFile(String particular){
    return new File(TempRoot(),particular);
  }

  public static final int diskfree(String moreParams, TextList msgs) {
    String filename = "C:\\CYGWIN\\BIN\\df.exe";//+_+ move to OS specific classes
    int timeout = 5;
    int displayrate = 1;
    if(IOX.fileSize(filename)==0) {
      filename = "df";
      timeout = -1;
      displayrate = -1;
    }
    filename = filename+" -k "+StringX.TrivialDefault(moreParams, "");
    int c = Executor.runProcess(filename, "", displayrate /* -1 */, timeout /* was never returning when set to -1 for my machine (not sure why) */, msgs);
    return c;
  }

}

