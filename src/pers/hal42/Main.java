package pers.hal42;

import pers.hal42.lang.*;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.logging.LogLevelEnum;
import pers.hal42.logging.LogSwitch;
import pers.hal42.stream.IOX;
import pers.hal42.text.TextList;
import pers.hal42.thread.ThreadX;
import pers.hal42.timer.Ticks;
import pers.hal42.transport.EasyCursor;
import pers.hal42.transport.EasyProperties;
import pers.hal42.util.PrintFork;

import java.io.*;
import java.util.Vector;

import static pers.hal42.lang.SystemX.gcMessage;
import static pers.hal42.stream.IOX.Close;

/**
 * run a class that implements an application.
 * this is an old version that uses properties rather than Json files for configuration.
 * adding Json should not be a big deal, even keeping properties, just need a 'JsonCursor' similar to EasyCursor.
 */
public class Main {
  public File Home;
  public File Logcontrol;
  public File Logger;
  public File Properties;
  //add a Properties from command line args. execute inside stdStart
  protected EasyCursor stdProperties;
  protected TextList freeArgs = new TextList(); //args that are not in property syntax
  public static Main Application = null;
  private static ErrorLogStream dbg;
  private static boolean keepAlive = true;
  private static Thread toKeepAlive = null;
  public static final String logcontrolName = "logcontrol.properties";
  public static final String buflogkey = "paymate.bufLog";
  public static final String overlogkey = "paymate.overLog";
  static final Vector<AtExit> exiters = new Vector<>();

  public Main(Class mained) {
    if (dbg == null) {
      dbg = ErrorLogStream.getForClass(Main.class);
    }
    if (Application == null) { //first class in is the application...
      Application = this;  //an arbitrary rule we will learn to love.
    }
    String ourPath = StringX.TrivialDefault(System.getProperty("user.dir"), File.listRoots()[0].getAbsolutePath());
    Home = new File(ourPath);

    Properties = LocalFile(mained.getName(), "properties");
    Logcontrol = localFile(logcontrolName);
//    Logger= OS.TempFile("paymate.log");
    Logger = OS.TempFile(ReflectX.justClassName(mained) + ".log");
  }

  public String[] argv() {
    return freeArgs.toStringArray();
  }

  public EasyCursor makeProperties() {//probably not as useful as stdProperties field..
    EasyCursor ezp = new EasyCursor(System.getProperties());
    FileInputStream inStream = null;
    try {
      inStream = new FileInputStream(Properties);
      ezp.load(inStream);
    } catch (java.io.FileNotFoundException fnf) {
      dbg.ERROR(fnf.getMessage());
    } catch (Exception ex) {
      dbg.Caught(ex, "Main::getProp:properties load.");   //it is ok to not have any properties.
    } finally {
      Close(inStream);
    }
    return ezp;
  }

  public File localFile(String filename) {
    File f = new File(filename);
    if (!f.isAbsolute()) {
      f = new File(Home, filename);
    }
    return f;
  }

  public EasyCursor logStart() {
    //  PrintFork.SetAll(LogSwitch.OFF);//output NOTHING. without IP terminal assistance
    EasyCursor fordebug = null;
    FileInputStream fis = null;
    try {
      if (Logcontrol.exists()) {
        fis = new FileInputStream(Logcontrol);
        fordebug = EasyCursor.New(fis);
        LogSwitch.apply(fordebug);
      }//best if precedes many classes starting up.
    } catch (IOException ignored) {
      //yep, ignored. this is just debug control after all...
    } finally {
      Close(fis);//so that we can edit the file while program is running, for next rnu.
    }
    return fordebug;
  }

  public EasyCursor stdStart(String argv[]) {//will be prettier when we use cmdLine for arg processing
    logStart();
    stdProperties = makeProperties(); //starting with Java's properties
    //int rawargi=0;
    if (argv != null) {
      for (String anArgv : argv) { //we add in command line args of our own format
        int cut = anArgv.indexOf(':');//coz windows absorbs the more obvious '='
        if (cut >= 0) {
          stdProperties.setString(anArgv.substring(0, cut), anArgv.substring(cut + 1));
          // +_+ fix the above to toelrate whitespace between lable and value.
        } else { //add in "nth unlabeled value or valueless label"
          freeArgs.add(anArgv);
        }
      }
    }
    //    stdProperties.loadEnum("debug", LogSwitch.dump());
    boolean bufferlogger = stdProperties.getBoolean(buflogkey, false); // by default do not buffer the log
    boolean overwritelogger = stdProperties.getBoolean(overlogkey, true); // by default, overwrite the log on program restart (however, the default of buflog=false prevents this from being used; only used when buflog=true)
    // don't send the full path if the LogFile is going to be used (bufLog is true):
    ErrorLogStream.stdLogging(bufferlogger ? Logger.getName() : Logger.getAbsolutePath(), bufferlogger, overwritelogger);
    saveProps();
    return stdProperties;
  }

  public static void OnExit(AtExit cleaner) {
    exiters.add(cleaner);
  }

  public static void OnExit(AtExit cleaner, int priority) {
//priority ignored right now, later on will do insertion sort, 'til then LIFO
    exiters.add(cleaner);
  }

  private static void atExit() {
    for (int i = exiters.size(); i-- > 0; ) { //#ORDER MATTERS we reverse the order in which they were registered.
      exiters.elementAt(i).AtExit();
    }
  }

  public static EasyCursor props() {
    if (Application != null && Application.stdProperties != null) {
      return new EasyCursor(Application.stdProperties);
    } else {
      return new EasyCursor(System.getProperties());
    }
  }

  public static EasyCursor Properties(Class claz) {
    return EasyCursor.FromDisk(LocalFile(claz.getName(), "properties"));
  }

  public static String[] Argv() {
    if (Application != null) {
      return Application.freeArgs.toStringArray();
    } else {
      return new String[0];
    }
  }

  public static EasyCursor props(String firstpush) {
    EasyCursor ezc = props();
    ezc.push(firstpush);
    return ezc;
  }

  public static File LocalFile(String filename) {
    return ObjectX.NonTrivial(Application) ? Application.localFile(filename) : null;
  }

  public static File LocalFile(String fileroot, String extension) {
    return ObjectX.NonTrivial(Application) ? Application.localFile(fileroot + '.' + extension) : null;
  }

  /**
   * save the logging controls to @param aName
   */
  public static void saveLogging(String aName) {
    if (!StringX.NonTrivial(aName)) {
      aName = logcontrolName;
    }
    try {
      File logcontrol = new File(aName);
//      if (!logcontrol.exists()) {
//        if (!logcontrol.isAbsolute()) {
//          logcontrol = OS.TempFile(aName);
//        }
//      }
      OutputStream saver = new FileOutputStream(logcontrol);
      EasyProperties allLogswitches = LogSwitch.asProperties();
      allLogswitches.storeSorted(saver, "debug stream controls");
      saver.close();
    } catch (IOException ignored) {
      // this function is for debug only
    }
  }


  /** @param aName is root name for logging control file. Null or blank gets default name. */
  public static EasyCursor loadLogging(String aName) {
    if (!StringX.NonTrivial(aName)) {
      aName = logcontrolName;
    }
    EasyCursor fordebug = null;
    FileInputStream fis = null;
    try {
      if (IOX.FileExists(aName)) {
        fis = new FileInputStream(aName);
        fordebug = EasyCursor.New(fis);
        LogSwitch.apply(fordebug);
      }//best if precedes many classes starting up.
    } catch (IOException ignored) {
      //yep, ignored. this is just debug control after all...
    } finally {
      Close(fis);//so that we can edit the file while program is running, for next run.
    }
    return fordebug;
  }

  public static void stdExit(int exitvalue) {
    saveLogging("logcontrol.improperties");//there is an IPTerminal command to do a save.
    saveProps();
    ErrorLogStream.endLogging();
    atExit();//kill everyone else that is listed to be killed
//    end();//kill ourselves
    System.exit(exitvalue);
  }

  public static void saveProps() {
    try {
      EasyCursor combo = new EasyCursor(); //do NOT use defaulting!
      //done the following way gets ALL properties, even the system stuff
      combo.addMore(props());
      combo.addMore(LogSwitch.asProperties());
      combo.storeSorted(new FileOutputStream(OS.TempFile("combined.properties")), "COMBINED Main Properties");
    } catch (Exception ingored) {
      //ignore errors as this method exists for debug only.
    }
  }

  public static void keepAlive() {
    toKeepAlive = Thread.currentThread();
    while (keepAlive) {
      try {
        long stayAliveTime = Ticks.forMinutes(30);
        dbg.WARNING("Staying Alive:{0}", stayAliveTime);
        ThreadX.sleepFor(stayAliveTime); // whatever
      } catch (Exception e) {
        dbg.Caught(e);
      } //but don't catch throwables.
    }
  }

  public static void end() {
    keepAlive = false;
    if ((toKeepAlive != null) && toKeepAlive.isAlive()) {
      try {
        toKeepAlive.interrupt();
      } catch (Exception e) {
        dbg.Caught(e, "while ending:");
      }
    }
  }

  public static void gc(ErrorLogStream doit) {
    if (doit.willOutput(LogLevelEnum.VERBOSE)) {
//      gcMessage(doit.myName());
      PrintFork.Println(gcMessage(doit.myName()), LogLevelEnum.ERROR.level);
//      PrintFork.Println("StringStack:{" + StringStack.dumpStackList().asParagraph(OS.EOL) + "}", LogLevelEnum.ERROR.level);
    }
  }

  public static void gc(LogSwitch doit) {
    if (doit.is(LogLevelEnum.VERBOSE)) {
      System.err.println(gcMessage(doit.Name()));//# keep as system.err
    }
  }

  /**
   * cli==command line interface
   */
  public static Main cli(Class mained, String[] args) {
    if (mained == null) {
      mained = Main.class;
    }
    Main newone = new Main(mained);
    newone.stdStart(args);
    return newone;
  }

  /**
   * cli==command line interface
   */
  public static Main cli(Object mained, String[] args) {
    return cli(mained != null ? mained.getClass() : null, args);
  }
}
