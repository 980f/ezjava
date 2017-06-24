package pers.hal42.logging;

import pers.hal42.lang.AtExit;
import pers.hal42.lang.DateX;
import pers.hal42.lang.Safe;
import pers.hal42.lang.StringX;
import pers.hal42.stream.VirtualPrinter;
import pers.hal42.text.TextList;
import pers.hal42.util.PrintFork;
import pers.hal42.util.StringStack;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Vector;

import static pers.hal42.logging.LogLevelEnum.*;

/**
 * conditional printing of messages.
 * See PrintFork for where the messages are sent.
 * <p>
 * WARNING: turning a stream on OR off while in an enter/exit scope screws up the stack.
 * this was done to improve efficiency the rest of the time.
 */
public class ErrorLogStream implements AtExit, AutoCloseable {

  static class NullBugger extends ErrorLogStream {
    NullBugger() {
      super(LogSwitch.getFor(NullBugger.class));
    }

    public void rawMessage(int msgLevel, String message) {
      //do nothing
    }
  }

  static class StackTracer extends Exception {
    public String toString() {
      return "User requested stack trace, not a real exception";
    }
  }

  public boolean bare = false; //+_+ made a state to expedite big change.
  //public
  protected LogSwitch myLevel = null;
  /**
   * used to provide a stackable context, rather than having to fake an exception to get a stack trace.
   */
  protected StringStack context;
  /**
   * top of context stack
   */
  protected String ActiveMethod = "?"; //cached top of context stack
  public static LogFile fpf = null; //just  so that we can close the file explicitly on program exit.
  private static LogSwitch DONTaccessMEuseINSTEADglobalLevellerFUNCTION;
  // for the embedded exceptions
  private static Tracer Debug; // use Global() to get it!
  private static NullBugger bitbucket;

  protected ErrorLogStream(LogSwitch ls) {
    if (ls != null) {
      myLevel = ls;
      context = new StringStack("els." + ls.Name()); //the stack for ActiveMethod
    } else {
      myLevel = LogSwitch.getFor("INVALID");
    }
  }

  public void AtExit() {
    // stub
  }

  public boolean IsDown() { //#for atExit interface
    // stub
    return false;
  }

  public String toSpam() {
    // just deal with THIS stream ...
    return ((myLevel != null) ? (myLevel.Name() + ".level is " + myLevel.level) : "");
  }

  public ErrorLogStream setLevel(LogLevelEnum lle) {
    myLevel.setLevel(lle);
    return this;
  }

  /**
   * @return this
   */
  public ErrorLogStream setLevel(int lle) {
    myLevel.setLevel(lle);
    return this;
  }

  public String Location() {
    return ActiveMethod;
  }

  public String myName() {
    return myLevel != null ? myLevel.guiName : "nameless";
  }

  public int myLevel() {
    if (myLevel != null) {
      return myLevel.Level();
    }
    return Safe.INVALIDINDEX;
  }

  public boolean levelIs(LogLevelEnum lle) {
    return myLevel != null && myLevel.is(lle);
  }

  /**
   * for when message generation for parameter to VERBOSE() and friends is expensive
   *
   * @param msgLevel level that will be attempted.
   * @returns whether it is worth generating debug text
   */
  public boolean willOutput(int msgLevel) {
    return myLevel.passes(msgLevel) && globalLeveller().passes(msgLevel);
  }

  public boolean willOutput(LogLevelEnum msgLevel) {
    return willOutput(msgLevel.level);
  }

//  public static final boolean isDown() {
//    return (fpf != null) ? fpf.IsDown() : true;
//  }

  //////////////////////////////////////////////////////////
  public void rawMessage(int msgLevel, String message) {
    if (willOutput(msgLevel)) {
      PrintFork.Println(message, msgLevel);
    }
  }

  public void Message(int msgLevel, String message, Object... args) {
    // strange errors in here need to be debugged with System.out
    //noinspection TryWithIdenticalCatches
    try {
      // broke this down to find the exact line that causes the error ...
      StringBuilder builder = new StringBuilder(200);
      if (!bare) {
        builder.append(DateX.timeStampNowYearless());
        builder.append(LogSwitch.letter(msgLevel));
        builder.append(Thread.currentThread());
        builder.append("@").append(myLevel.Name());
        builder.append("::").append(ActiveMethod).append(":");
      }
      builder.append(MessageFormat.format(message, args));
      rawMessage(msgLevel, builder.toString());
    } catch (java.lang.NoClassDefFoundError e) {
      systemOutCaught(e);
    } catch (Exception e) {
      systemOutCaught(e);
    }
  }

  protected void Message(LogLevelEnum msgLevel, String message, Object... args) {
    Message(msgLevel.level, message, args);
  }

  private void systemOutCaught(Throwable t) {
    System.out.println("DEBUG::" + t + "\ntrace...");
    t.printStackTrace(System.out);
  }

  /**
   * three message levels are supported.
   */
  public void VERBOSE(String message) {
    Message(VERBOSE, message);
  }

  public void ERROR(String message) {
    Message(ERROR, message);
  }

  public void VERBOSE(String message, Object... clump) {
    Message(VERBOSE, message, clump);
  }

  public void WARNING(String message, Object... clump) {
    Message(WARNING, message, clump);
  }
//bareness should be stacked in parallel with name

  public void ERROR(String message, Object... clump) {
    Message(ERROR, message, clump);
  }

  /**
   * output an array as html indented list
   */
  public void logArray(int msgLevel, String tag, Object[] clump) {
    if (tag != null) { // just cleaner and easier to separate
      Message(msgLevel, "<ol name={0}>", tag);
      for (Object aClump : clump) {
        Message(msgLevel, "<li>{0}</li> ", String.valueOf(aClump));
      }
      Message(msgLevel, "</ol name={0}>", tag);
    } else {
      for (Object aClump : clump) {
        rawMessage(msgLevel, String.valueOf(aClump));
      }
    }
  }

  public void logArray(LogLevelEnum msgLevel, String tag, Object[] clump) {
    logArray(msgLevel.level, tag, clump);
  }

  public void VERBOSEv(String message, Vector clump) {
    logArray(VERBOSE, message, clump.toArray());
  }

  public void WARNINGv(String message, Vector clump) {
    logArray(WARNING, message, clump.toArray());
  }

  public void ERRORv(String message, Vector clump) {
    logArray(ERROR, message, clump.toArray());
  }

  public void close() {
    Message(TRACE, "Exits");
    ActiveMethod = context.pop();
  }

  /**
   * push teh context stack, @returns object that will pop it when 'closed'
   */
  public ErrorLogStream Push(String methodName) {
    context.push(ActiveMethod);
    ActiveMethod = methodName;
    Message(TRACE, "Entered");
    return this;
  }

  public void Caught(Throwable caught) {
    Caught(caught, "");
  }

  public void Caught(Throwable caught, String title) {
    int localLevel = ERROR.level; //FUE
    TextList tl = Caught(title, caught, new TextList());
    for (int i = 0; i < tl.size(); i++) {//#in order
      Message(localLevel, tl.itemAt(i));
    }
  }

  public final void showStack(int msgLevel) {
    if (willOutput(msgLevel)) {
      try {
        throw new StackTracer();
      } catch (StackTracer ex) {
        Caught(ex);
      }
    }
  }


  public void _objectDump(Object o, String path, TextList tl) {
    try {
      objectSubDump(o, o.getClass(), path, tl);
    } catch (Exception e2) {
      Caught(e2);
    }
  }

  /**
   * @deprecated bad logic re null tl param
   */
  void objectSubDump(Object o, Class c, String path, TextList tl) {
    // see if the object has a "toSpam()" function, if so, call it

    boolean logit = (tl == null);
    Class[] paramTypes = {Object.class, String.class, TextList.class};
    try {
      if (logit) {
        Method method = c.getMethod("toSpam");
        try {
          Object result = method.invoke(o);
          if (result instanceof TextList) {
            TextList tlResult = (TextList) result;
            for (int i = 0; i < tlResult.size(); i++) {
              tl.add(path + "." + tlResult.itemAt(i));
            }
          } else {
            String resultStr = String.valueOf(result);
            if (resultStr.indexOf('=') > -1) {
              tl.add(path + "." + resultStr);
            } else {
              tl.add(path + "=" + resultStr);
            }
          }
        } catch (Exception e) {
          Caught(e);
        }
      }
    } catch (NoSuchMethodException e) {
      WARNING("{0} does not have a method 'toSpam()'", c.getName());
      try {
        Method method = c.getMethod("objectDump", paramTypes);
        Object[] args = {c, path, tl};
        try {
          method.invoke(o, args);
        } catch (Exception e31) {
          Caught(e31);
        }
      } catch (NoSuchMethodException e45) {
        try {
          // if not, recurse through the class's supers and members
          Field[] fields1 = c.getFields();
          Field[] fields2 = c.getDeclaredFields();
          java.util.ArrayList<Field> list1 = new java.util.ArrayList<>(java.util.Arrays.asList(fields1));
          java.util.ArrayList<Field> list2 = new java.util.ArrayList<>(java.util.Arrays.asList(fields2));
          list1.addAll(list2);
          removeDuplicates(list1); // +++ need to remove duplicates
          Object[] fields = list1.toArray();
          Class[] classes = c.getClasses();
          if ((fields.length == 0) && (classes.length == 0)) {
            // this is a primitive, I think
            String msg = path + "=" + o;
            if (logit) {
              VERBOSE(msg);
            } else {
              tl.add(msg);
            }
          } else {
            dumpFields(o, c, path, tl, fields, logit);
            // then the classes
            for (Class aClass : classes) {
              objectSubDump(o, aClass, path + "[" + aClass.getName() + "]", tl);
            }
          }
        } catch (Exception e5) {
          Caught(e5);
        }
      }
    } catch (Exception e4) {
      Caught(e4);
    }
  }

  private void removeDuplicates(java.util.ArrayList list) {
    for (int i = list.size(); i-- > 1; ) {
      for (int j = i; j-- > 0; ) {
        if (list.get(i).equals(list.get(j))) {
          Global().VERBOSE("removed[" + i + "]: " + list.get(i));
          list.remove(i);
          break;
        }
      }
    }
  }

  void dumpFields(Object o, Class c, String path, TextList tl, /*Field[]*/ Object[] fields, boolean logit) {
    if (tl == null) {
      logit = true;
    }
    // first, do the fields
    for (Object field : fields) {
      Object fo = null;
      Field f = (Field) field;
      String newPath = path + "." + f.getName();
      try {
        fo = f.get(o);
      } catch (IllegalAccessException e2) {
        String msg = newPath + "= <INACCESSIBLE!>";
//        if(logit) {
        VERBOSE(msg);
//        } else {
//          tl.add(msg);
//        }
      } catch (IllegalArgumentException e3) {
        String msg = newPath + "= <NOT INSTANCE OF CLASS:" + c.getName() + "?>";
        if (logit) {
          VERBOSE(msg);
        } else {
          tl.add(msg);
        }
      }
      if (fo != null) {
        if ((fo instanceof Boolean) || (fo instanceof Character) || (fo instanceof Number) || //handles byte, double, float, integer, long, short
          (fo instanceof String) || (fo instanceof StringBuffer)) {
          String msg = newPath + "=" + fo;
          if (logit) {
            VERBOSE(msg);
          } else {
            tl.add(msg);
          }
        } else {
          objectSubDump(fo, fo.getClass(), newPath, tl);
        }
      }
    }
  }

  public void Exit() {
    close();
  }

  public void dump(int level, String path, String content) {
    if (willOutput(level)) {
      try {
        Files.write(Paths.get("bigquery.sql"), content.getBytes());
      } catch (IOException e) {
        ERROR("Couldn't save debug copy of content, oh well.");
      }
    }
  }

  //this had to be implmented with a lazy init due to classloader loops.
  private static LogSwitch globalLeveller() {
    if (DONTaccessMEuseINSTEADglobalLevellerFUNCTION == null) {
      DONTaccessMEuseINSTEADglobalLevellerFUNCTION = LogSwitch.getFor(ErrorLogStream.class, "GLOBALGATE");
    }
    return DONTaccessMEuseINSTEADglobalLevellerFUNCTION;
  }

  public static void Choke(LogLevelEnum lle) {
    globalLeveller().setLevel(lle);
  }

  /**
   * @apiNote use getForClass whenever possible. Use this for objects whose name is not
   * reasonably derived from some class's name
   */
  public static ErrorLogStream getForName(String name, LogLevelEnum level) {
    return getForName(name).setLevel(level);
  }

  public static ErrorLogStream getForName(String name, int level) {
    return getForName(name).setLevel(level);
  }

  private static synchronized ErrorLogStream getForName(String name) {
    if (StringX.NonTrivial(name)) {
      return new ErrorLogStream(LogSwitch.getFor(name));
    } else { //bit bucket for trivial name
      return Null(); // +++ put an error into the Debug logstream about this.
    }
  }

  public static ErrorLogStream getForClass(Class myclass) {
    return getExtension(myclass, null);
  }

  public static ErrorLogStream getForClass(Class myclass, LogLevelEnum level) {
    return getForClass(myclass).setLevel(level);
  }

  public static ErrorLogStream getForClass(Class myclass, int level) {
    return getForClass(myclass).setLevel(level);
  }

  public static ErrorLogStream getExtension(Class myclass, String suffix) {
    return (myclass != null) ? getForName(LogSwitch.shortName(myclass, suffix)) : Null();
  }

  public static ErrorLogStream getExtension(Class myclass, String suffix, int level) {
    return getExtension(myclass, suffix).setLevel(level);
  }

  public static ErrorLogStream Null() {
    if (null == bitbucket) {
      bitbucket = new NullBugger();
    }
    return bitbucket;
  }

  /**
   * @return a debugger that will work. EIther the given one or one that consumes all messages.
   */
  public static ErrorLogStream NonNull(ErrorLogStream dbg) {
    return dbg != null ? dbg : ErrorLogStream.Null();
  }

  public static void endLogging() {
    if (fpf != null) {
      fpf.AtExit();
    }
  }

  public static void atExit() {
    endLogging();
  }

  public static boolean isGlobalVerbose() {
    return globalLeveller().is(VERBOSE);
  }

  public static ErrorLogStream Global() {
    // +_+ synchronize creation, if not may get more than one, which isn't a big deal.
    if (Debug == null) {
      Debug = new Tracer(LogSwitch.getFor(ErrorLogStream.class, "Debug")); //used by printfork management.
    }
    return Debug;
  }

  public static TextList Caught(String title, Throwable caught, TextList tl) {
    tl.add("<Caught> " + StringX.TrivialDefault(title, ""));
    resolveExceptions(caught, tl);
    tl.add("</Caught>");
    return tl;
  }

  //uses refelction to seek first of any members of class exception
  protected static Throwable NextException(Throwable t1) {
    Throwable t2 = null;
    // look for an exception in the exception:
    // getOrigException()
    try {
      Class c = t1.getClass();
      String possibilities[] = {"getOrigException", "getNextException"};
      boolean isSql = false;
      // see if it is a JPosException or SQLException
      for (int i = possibilities.length; i-- > 0 && (t2 == null); ) {
        Method method = c.getMethod(possibilities[i]);
        if (method != null) {
          t2 = (Exception) method.invoke(t1);
        }
      }
    } catch (Exception e) {
    /* abandon all hope ye who enter here */
    }
    return t2;
  }

  protected static String extendedInfo(Throwable t) {
    if ((t != null) && (t instanceof SQLException)) {
      SQLException e = (SQLException) t;
      StringBuilder ret = new StringBuilder();
      ret.append("\n  SQLState:").append(e.getSQLState()).
        append("\n  SQLMessage:").append(e.getMessage()).
        append("  SQLVendor:").append(String.valueOf(e.getErrorCode()));
      return ret.toString();
    }
    return null;
  }

  /**
   * New Exception resolver stuff
   */
  public static TextList resolveExceptions(Throwable t) {
    return resolveExceptions(t, new TextList());
  }

  public static TextList resolveExceptions(Throwable t, TextList tl) {
    tl.add("Error: " + t); // the trace doesn't always give enough detail!
    VirtualPrinter buffer = new VirtualPrinter();
    if (t != null) {
      t.printStackTrace(buffer); //all that ar elistneing to errors...
    }
    tl.add(buffer.backTrace());
    String ei = extendedInfo(t);
    if (ei != null) {
      tl.add(ei);
    }
    // here, see if the exception CONTAINS an exception, and if so, do it too
    Throwable t2 = NextException(t);
    if (t2 != null) {
      Caught("", t2, tl);
    }
    return tl;
  }

  public static TextList whereAmI() {
    TextList tl = new TextList();
    Throwable t = new Throwable();
    try {
      throw t;
    } catch (Throwable t2) {
      resolveExceptions(t2, tl);
    }
    return tl;
  }

  /**
   * @param background if true pushes out log information on a background thread, good for debug but bad for performance.
   */
  public static PrintFork stdLogging(String logName, boolean background, boolean overwrite) {
    PrintFork pf = null;
    PrintFork.SetAll(LogLevelEnum.VERBOSE);
    pf = PrintFork.New("System.out", System.out, LogLevelEnum.WARNING.level, true);//always have a console logger.
    if (StringX.NonTrivial(logName)) {
      if (background) {
        fpf = new LogFile(logName, overwrite);
        pf = fpf.getPrintFork(logName, LogLevelEnum.VERBOSE, true);  //ancient lore had these not register themselves.
      } else {
        try {
          if (!logName.endsWith(LogFile.DotLog)) {
            logName = logName + LogFile.DotLog;
          }
          pf = PrintFork.New("System.out", System.out, LogLevelEnum.VERBOSE.level, true);
          pf = PrintFork.New(logName, new PrintStream(new FileOutputStream(logName)));//simple logging, no roller etc.:
        } catch (Exception filennotfound) {
          //??? who cares
        }
      }
    }
    Global().ERROR("stdLogging Started:" + logName + (background ? " buffered" : " straight") + (overwrite ? " overwrite" : " append"));
    return pf;
  }

  public static void stdLogging(String logName, boolean background) {
    stdLogging(logName, background, false); //defaulted for server.
  }

  public static void stdLogging(String logName) {
    stdLogging(logName, false); //defaulted for best debug behavior.
  }

  // dumpage
  // path = the object's variable name
  public static void objectDump(Object o, String path) {
    objectDump(o, path, null);
  }

  public static void objectDump(Object o, String path, TextList tl) {
    Global()._objectDump(o, path, tl);
  }

}
