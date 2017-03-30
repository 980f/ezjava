package pers.hal42.logging;

/** WARNING: turning a stream on OR off while in an enter/exit scope screws up the stack.
 * this was done to improve efficiency the rest of the time.
 * @todo: finish applying logswitch
 */


import pers.hal42.lang.AtExit;
import pers.hal42.logging.LogSwitch;
import pers.hal42.util.StringStack;
import pers.hal42.util.Tracer;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Vector;

// for the embedded exceptions

class NullBugger extends ErrorLogStream {
  NullBugger() {
    super(LogSwitch.getFor(NullBugger.class));
  }

  public void rawMessage(int msgLevel, String message) {
  //do nothing
  }
}

class StackTracer extends Exception {
  public String toString() {
    return "User requested stack trace, not a real exception";
  }
}

public class ErrorLogStream implements AtExit {

  private static LogSwitch DONTaccessMEuseINSTEADglobalLevellerFUNCTION;

  private static LogSwitch globalLeveller() {
    if(DONTaccessMEuseINSTEADglobalLevellerFUNCTION == null) {
      DONTaccessMEuseINSTEADglobalLevellerFUNCTION = LogSwitch.getFor(ErrorLogStream.class, "GLOBALGATE");
    }
    return DONTaccessMEuseINSTEADglobalLevellerFUNCTION;
  }

  public static final void Choke(int lle) {
    globalLeveller().setto(lle);
  }

  private static Tracer Debug; // use Global() to get it!

  protected ErrorLogStream(LogSwitch ls) {
    if(ls != null) {
      myLevel = ls;
      context = new StringStack("els." + ls.Name()); //the stack for ActiveMethod
    } else {
    //we are way screwed.
    }
  }

  /**
   * @warn use getForClass whenever possible. Use this for objects whose name is not
   * reasonably derived from some class's name
   */
  public static final ErrorLogStream getForName(String name, LogLevelEnum level) {
    return getForName(name).setLevel(level);
  }

  public static final ErrorLogStream getForName(String name, int level) {
    return getForName(name).setLevel(level);
  }

  private static synchronized final ErrorLogStream getForName(String name) {
    if(StringX.NonTrivial(name)) {
      return new ErrorLogStream(LogSwitch.getFor(name));
    } else { //bit bucket for trivial name
      return Null(); // +++ put an error into the Debug logstream about this.
    }
  }

  public static final ErrorLogStream getForClass(Class myclass) {
    return getExtension(myclass, null);
  }

  public static final ErrorLogStream getForClass(Class myclass, LogLevelEnum level) {
    return getForClass(myclass).setLevel(level);
  }

  public static final ErrorLogStream getForClass(Class myclass, int level) {
    return getForClass(myclass).setLevel(level);
  }

  public static final ErrorLogStream getExtension(Class myclass, String suffix) {
    return(myclass != null) ? getForName(LogSwitch.shortName(myclass, suffix)) : Null();
  }

  public static final ErrorLogStream getExtension(Class myclass, String suffix, int level) {
    return getExtension(myclass, suffix).setLevel(level);
  }

  private static NullBugger bitbucket;
  public static ErrorLogStream Null() {
    if(null == bitbucket) {
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

  public void AtExit() {
  // stub
  }

  public static final void endLogging() {
    if(fpf != null) {
      fpf.AtExit();
    }
  }

  public static final void atExit() {
    endLogging();
  }

  public boolean IsDown() { //#for atExit interface
    // stub
    return false;
  }

//  public static final boolean isDown() {
//    return (fpf != null) ? fpf.IsDown() : true;
//  }

  public String toSpam() {
    // just deal with THIS stream ...
    return( (myLevel != null) ? (myLevel.Name() + ".level is " + myLevel.Image()) : "");
  }

//for legacy code
  public final static int VERBOSE = LogLevelEnum.VERBOSE;
  public final static int WARNING = LogLevelEnum.WARNING;
  public final static int ERROR = LogLevelEnum.ERROR;
  public final static int OFF = LogLevelEnum.OFF;

  //public
  protected LogSwitch myLevel = null;
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

  protected StringStack context;
  protected String ActiveMethod = "?"; //cached top of context stack

  public String Location() {
    return ActiveMethod;
  }

  public boolean bare = false; //+_+ made a state to expedite big change.
//bareness should be stacked in parallel with name

  public static final boolean isGlobalVerbose() {
    return globalLeveller().is(VERBOSE);
  }

  public String myName() {
    return myLevel != null ? myLevel.guiName : "nameless";
  }

  public int myLevel() {
    if(myLevel != null) {
      return myLevel.Value();
    }
    return TrueEnum.Invalid();
  }

  public boolean levelIs(LogLevelEnum lle) {
    if(myLevel != null) {
      return myLevel.is(lle);
    }
    return false;
  }

  /**
   * for when message generation for parameter to VERBOSE() and friends is expensive
   * @param msgLevel level that will be attempted.
   * @return whether it is worth generating debug text
   */
  public boolean willOutput(int msgLevel) {
    return myLevel.passes(msgLevel) && globalLeveller().passes(msgLevel);
  }

//////////////////////////////////////////////////////////
  public void rawMessage(int msgLevel, String message) {
    if(willOutput(msgLevel)) {
      PrintFork.Println(message, msgLevel);
    }
  }

  public static ErrorLogStream Global() {
    // +_+ synchronize creation, if not may get more than one, which isn't a big deal.
    if(Debug == null) {
      Debug = new Tracer(LogSwitch.getFor(ErrorLogStream.class, "Debug")); //used by printfork management.
    }
    return Debug;
  }

  // !!! run javamake to test this! (works today, but any changes will EASILY break this!) MMM20001229
  protected void Message(int msgLevel, String message) {
    // strange errors in here need to be debugged with System.out
    try {
      // broke this down to find the exact line that causes the error ...
      String prefix;
      if(!bare) {
        prefix = DateX.timeStampNowYearless();
        prefix += LogSwitch.letter(msgLevel);
        prefix += Thread.currentThread();
        prefix += "@" + myLevel.Name();
        prefix += "::" + ActiveMethod + ":";
      } else {
        prefix = "";
      }
      rawMessage(msgLevel, prefix + message);
    } catch(java.lang.NoClassDefFoundError e) {
      systemOutCaught(e);
    } catch(Exception e) {
      systemOutCaught(e);
    }
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

  public void WARNING(String message) {
    Message(WARNING, message);
  }

  public void ERROR(String message) {
    Message(ERROR, message);
  }

  public void VERBOSE(String message, Object[] clump) {
    logArray(VERBOSE, message, clump);
  }

  public void WARNING(String message, Object[] clump) {
    logArray(WARNING, message, clump);
  }

  public void ERROR(String message, Object[] clump) {
    logArray(ERROR, message, clump);
  }

  public void logArray(int msgLevel, String tag, Object[] clump) {
    if(tag != null) { // just cleaner and easier to separate
      rawMessage(msgLevel, "<ol name=" + tag + ">");
      for(int i = 0; i < clump.length; i++) { //in ascending order for clarity
        rawMessage(msgLevel, "<li> " + String.valueOf(clump[i]));
      }
      rawMessage(msgLevel, "</ol name=" + tag + ">");
    } else {
      for(int i = 0; i < clump.length; i++) { //in ascending order for clarity
        rawMessage(msgLevel, String.valueOf(clump[i]));
      }
    }
  }

  public void VERBOSE(String message, Vector clump) {
    logArray(VERBOSE, message, clump.toArray());
  }

  public void WARNING(String message, Vector clump) {
    logArray(WARNING, message, clump.toArray());
  }

  public void ERROR(String message, Vector clump) {
    logArray(ERROR, message, clump.toArray());
  }

  public void Enter(String methodName) {
    context.push(ActiveMethod);
    ActiveMethod = methodName;
    VERBOSE("Entered");
  }

  public void Caught(Throwable caught) {
    Caught("", caught);
  }

  public void Caught(String title, Throwable caught) {
    int localLevel = ERROR; //FUE
    TextList tl = Caught(title, caught, new TextList());
    for(int i = 0; i < tl.size(); i++) {
      Message(localLevel, tl.itemAt(i));
    }
  }

  public static final TextList Caught(String title, Throwable caught, TextList tl) {
    int localLevel = ERROR; //FUE
    tl.add("<Caught> " + StringX.TrivialDefault(title, ""));
    resolveExceptions(caught, tl);
    tl.add("</Caught>");
    return tl;
  }

  //uses refelction to seek first of any members of class exception
  protected static final Throwable NextException(Throwable t1) {
    Throwable t2 = null;
    // look for an exception in the exception:
    // getOrigException()
    try {
      Class c = t1.getClass();
      int SQLItem = 1;
      String possibilities[] = {"getOrigException", "getNextException"};
      boolean isSql = false;
      // see if it is a JPosException or SQLException
      for(int i = possibilities.length; i-- > 0 && (t2 == null); ) {
        Method method = c.getMethod(possibilities[i], null);
        if(method != null) {
          t2 = (Exception) method.invoke(t1, null);
        }
      }
    } catch(Exception e) {
    /* abandon all hope ye who enter here */
    }
    return t2;
  }

  protected static final String extendedInfo(Throwable t) {
    String ret = null;
    if( (t != null) && (t instanceof SQLException)) {
      SQLException e = (SQLException) t;
      ret += "\n  SQLState:" + e.getSQLState();
      ret += "\n  SQLMessage:" + e.getMessage();
      ret += "  SQLVendor:" + String.valueOf(e.getErrorCode());
    }
    return ret;
  }

  /**
   * New Exception resolver stuff
   */
  public static final TextList resolveExceptions(Throwable t) {
    return resolveExceptions(t, new TextList());
  }

  public static final TextList resolveExceptions(Throwable t, TextList tl) {
    tl.add("Error: " + t); // the trace doesn't always give enough detail!
    VirtualPrinter buffer = new VirtualPrinter();
    if(t != null) {
      t.printStackTrace(buffer); //all that ar elistneing to errors...
    }
    tl.add(buffer.backTrace());
    String ei = extendedInfo(t);
    if(ei != null) {
      tl.add(ei);
    }
    // here, see if the exception CONTAINS an exception, and if so, do it too
    Throwable t2 = NextException(t);
    if(t2 != null) {
      Caught("", t2, tl);
    }
    return tl;
  }

  public static final TextList whereAmI() {
    TextList tl = new TextList();
    Throwable t = new Throwable();
    try {
      throw t;
    } catch(Throwable t2) {
      resolveExceptions(t2, tl);
    }
    return tl;
  }

  public final void showStack(int msgLevel) {
    if(willOutput(msgLevel)) {
      try {
        throw new StackTracer();
      } catch(StackTracer ex) {
        Caught(ex);
      }
    }
  }

  public void Exit() {
    VERBOSE("Exits");
    ActiveMethod = context.pop();
  }

  public static LogFile fpf = null; //just  so that we can close the file explicitly on program exit.

  public static final void stdLogging(String logName, boolean background, boolean overwrite) {
    PrintFork pf = null;

    if(StringX.NonTrivial(logName)) {
      if(background) {
        fpf = new LogFile(logName, overwrite);
        pf = fpf.getPrintFork(true); // this creates it
      } else {
        try {
          PrintFork.New("System.out", System.out);
          pf = PrintFork.New(logName, new PrintStream(new FileOutputStream(logName)));
        } catch(Exception filennotfound) {
        //??? who cares
        }
      }
    }
    Global().ERROR("stdLogging Started:" + logName + (background ? " buffered" : " straight") + (overwrite ? " overwrite" : " append"));
  }

  public static final void stdLogging(String logName, boolean background) {
    stdLogging(logName, background, false); //defaulted for server.
  }

  public static final void stdLogging(String logName) {
    stdLogging(logName, true); //defaulted for server.
  }

// dumpage
  // path = the object's variable name
  public static final void objectDump(Object o, String path) {
    objectDump(o, path, null);
  }

  public static final void objectDump(Object o, String path, TextList tl) {
    Global()._objectDump(o, path, tl);
  }

  public void _objectDump(Object o, String path, TextList tl) {
    try {
      objectSubDump(o, o.getClass(), path, tl);
    } catch(Exception e2) {
      Caught(e2);
    }
  }

  void objectSubDump(Object o, Class c, String path, TextList tl) {
    // see if the object has a "toSpam()" function, if so, call it
    Method method = null;
    boolean logit = (tl == null);
    Class[] paramTypes = {Object.class, String.class, TextList.class};
    try {
      method = c.getMethod("toSpam", null);
      try {
        Object result = method.invoke(o, null);
        if(result instanceof TextList) {
          TextList tlResult = (TextList) result;
          for(int i = 0; i < tlResult.size(); i++) {
            tl.add(path + "." + tlResult.itemAt(i));
          }
        } else {
          String resultStr = String.valueOf(result);
          if(resultStr.indexOf('=') > -1) {
            tl.add(path + "." + resultStr);
          } else {
            tl.add(path + "=" + resultStr);
          }
        }
      } catch(Exception e) {
        Caught(e);
      }
    } catch(NoSuchMethodException e) {
      WARNING(c.getName() + " does not have a method 'toSpam'");
      try {
        method = c.getMethod("objectDump", paramTypes);
        Object[] args = {c, path, tl};
        try {
          method.invoke(o, args);
        } catch(Exception e31) {
          Caught(e31);
        }
      } catch(NoSuchMethodException e45) {
        try {
          // if not, recurse through the class's supers and members
          Field[] fields1 = c.getFields();
          Field[] fields2 = c.getDeclaredFields();
          java.util.ArrayList list1 = new java.util.ArrayList(java.util.Arrays.asList(fields1));
          java.util.ArrayList list2 = new java.util.ArrayList(java.util.Arrays.asList(fields2));
          list1.addAll(list2);
          removeDuplicates(list1); // +++ need to remove duplicates
          Object[] fields = list1.toArray();
          Class[] classes = c.getClasses();
          if( (fields.length == 0) && (classes.length == 0)) {
            // this is a primitive, I think
            String msg = path + "=" + o;
            if(logit) {
              VERBOSE(msg);
            } else {
              tl.add(msg);
            }
          } else {
            dumpFields(o, c, path, tl, fields, logit);
            // then the classes
            for(int i = 0; i < classes.length; i++) {
//              objectSubDump(o, classes[i], path+"["+classes[i].getName()+"]",tl);
            }
          }
        } catch(Exception e5) {
          Caught(e5);
        }
      }
    } catch(Exception e4) {
      Caught(e4);
    }
  }

  private void removeDuplicates(java.util.ArrayList list) {
    for(int i = list.size(); i-- > 1; ) {
      for(int j = i; j-- > 0; ) {
        if(list.get(i).equals(list.get(j))) {
          Global().VERBOSE("removed[" + i + "]: " + list.get(i));
          list.remove(i);
          break;
        }
      }
    }
  }

  void dumpFields(Object o, Class c, String path, TextList tl, /*Field[]*/ Object[] fields, boolean logit) {
    if(tl == null) {
      logit = true;
    }
    // first, do the fields
    for(int i = 0; i < fields.length; i++) {
      Object fo = null;
      Field f = (Field) fields[i];
      String newPath = path + "." + f.getName();
      try {
        fo = f.get(o);
      } catch(IllegalAccessException e2) {
        String msg = newPath + "= <INACCESSIBLE!>";
//        if(logit) {
        VERBOSE(msg);
//        } else {
//          tl.add(msg);
//        }
      } catch(IllegalArgumentException e3) {
        String msg = newPath + "= <NOT INSTANCE OF CLASS:" + c.getName() + "?>";
        if(logit) {
          VERBOSE(msg);
        } else {
          tl.add(msg);
        }
      }
      if(fo != null) {
        if( (fo instanceof Boolean) || (fo instanceof Character) ||
            (fo instanceof Number) || //handles byte, double, float, integer, long, short
            (fo instanceof String) || (fo instanceof StringBuffer)) {
          String msg = newPath + "=" + fo;
          if(logit) {
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

}

