package pers.hal42.logging;

import pers.hal42.lang.OS;
import pers.hal42.lang.ReflectX;
import pers.hal42.lang.StringX;
import pers.hal42.text.TextList;
import pers.hal42.transport.EasyCursor;

import java.util.Collections;
import java.util.Vector;

import static pers.hal42.logging.LogLevelEnum.VERBOSE;
import static pers.hal42.logging.LogSwitchRegistry.registry;

/**
 * LogSwitches are contols for emitting messages through the two layes of the logging,
 * ErrorLogStreams gate what they pass on to PrintForks, which gate what they pass along to the outside world.
 * <p>
 * comparable is just for sorting for user displays.
 */
public class LogSwitch implements Comparable {
  /**
   * a nickname to manipulate a logging control
   */
  protected String guiName;
  /**
   * using arbitrary level value, for which LogLevelEnum holds typical ones
   */
  protected int level;
  private static final char[] lvlLtr = {'-', '/', '!'};
  protected static int DEFAULT_LEVEL = VERBOSE.level;//set for server, which has a harder time configuring than the client
  @SuppressWarnings("FieldCanBeLocal")
  private static boolean debugme = false;//have to recompile to debug

  //////////////////////////////////////////////////////
  private LogSwitch(String lookup, LogLevelEnum lle) {
    this(lookup, lle.level);
  }

  private LogSwitch(String lookup, int spam) {
    level = spam;
    guiName = lookup;
    register();
  }

  private LogSwitch(String lookup) {
    this(lookup, DEFAULT_LEVEL);
  }

  public void setto(LogLevelEnum level) {
    this.level = level.level;
  }

  public void setto(int level) {
    this.level = level;
  }

  void register() {
    if (null != registry) {
      synchronized (registry) {//just to get size() to relate to us adding one.
        debug("registering " + Name());
        registry.add(this); // sort +++ !!!
        debug("Registry size: " + registry.size());
      }
    } else {
      debug("LOGSWITCH REGISTRY IS NULL!!!");
    }

  }

  /**
   * Returns a string representation of the object. In general, the
   * {@code toString} method returns a string that
   * "textually represents" this object. The result should
   * be a concise but informative representation that is easy for a
   * person to read.
   * It is recommended that all subclasses override this method.
   * <p>
   * The {@code toString} method for class {@code Object}
   * returns a string consisting of the name of the class of which the
   * object is an instance, the at-sign character `{@code @}', and
   * the unsigned hexadecimal representation of the hash code of the
   * object. In other words, this method returns a string equal to the
   * value of:
   * <blockquote>
   * <pre>
   * getClass().getName() + '@' + Integer.toHexString(hashCode())
   * </pre></blockquote>
   *
   * @return a string representation of the object.
   */
  @Override
  public String toString() {
    return LogLevelEnum.asString(level);
  }

  // for sorting for display purposes
  public int compareTo(Object o) {
    if ((o == null) || (!(o instanceof LogSwitch))) {
      throw (new ClassCastException());//per the interface specification
    }
    LogSwitch that = (LogSwitch) o;
    return Name().compareTo(that.Name());
  }

  /**
   * todo:2 use text for known levels. Add method on LogLevelEnum for that.
   */
  public String toSpam() {
    return Name() + ": " + Level();
  }

  public int Level() {
    return level;
  }

  public LogSwitch setLevel(int level) {
    this.level = level;
    return this;
  }

  public LogSwitch setLevel(LogLevelEnum lle) {
    return setLevel(lle.level);
  }

  public String Name() {
    return guiName;
  }

  /**
   * if argument is the same or more severe than internal level we shall do something
   */
  public boolean passes(LogLevelEnum llev) {
    return (llev.level >= level) && (level != 0);
  }

  public boolean passes(int level) {
    return (level >= this.level) && (this.level != 0);
  }

  public boolean is(LogLevelEnum llev) {
    return llev.level == level;
  }

  public static char letter(int msgLevel) {
    return (((msgLevel > -1) && (msgLevel < lvlLtr.length)) ? lvlLtr[msgLevel] : ' ');
  }

  private static void debug(String msg) {
    if (debugme) {
      System.out.println(msg);
    }
  }

  private static Vector<LogSwitch> All() {
    return registry;
  }

  public static String shortName(Class claz, String suffix) {
    if (StringX.NonTrivial(suffix)) {
      return ReflectX.shortClassName(claz) + "." + suffix;
    } else {
      return ReflectX.shortClassName(claz);
    }
  }

  public static String shortName(Class claz) {
    return shortName(claz, null);
  }

  /**
   * interface for editing:
   * <p>
   * Gives a "copy" of the list of pointers (but doesn't copy each item, still points to originals)
   * and sorts them.  Use this for external classes where possible.
   * <p>
   * +++ Create a separate LogSwitchList class that has an itemAt() function
   * that this class and other classes can use! +++
   */
  public static Vector<LogSwitch> Sorted() {
    Vector<LogSwitch> copy;
    synchronized (registry) {
      copy = new Vector<>(registry.size());
      copy.addAll(registry);
    }
    Collections.sort(copy);  // sort them by name
    return copy;
  }

  private static LogSwitch find(String name) {
    synchronized (registry) {
      for (LogSwitch test : registry) {
        if (test.Name().equals(name)) {
          return test;
        }
      }
    }
    return null;//doesn't exist, don't make one here!
  }

  private static LogSwitch find(Class claz, String suffix) {
    return find(shortName(claz, suffix));
  }

  private static LogSwitch find(Class claz) {
    return find(shortName(claz));
  }

  public static boolean exists(String clasname) {
    return find(clasname) != null;
  }

  /**
   * find or create a new LogSwitch
   */
  public static LogSwitch getFor(String guiname, int oncreate) {
    synchronized (registry) {
      LogSwitch forclass = find(guiname);
      if (forclass == null) {
        forclass = new LogSwitch(guiname);
        forclass.setto(oncreate);
      }
      return forclass;
    }
  }

  /**
   * @return existing one by the @param guiname, else create a new one and set its level to 'oncreate'
   */
  public static LogSwitch getFor(String guiname, LogLevelEnum oncreate) {
    for (LogSwitch item : LogSwitchRegistry.registry) {
      if (item.Name().equals(guiname)) {
        return item;
      }
    }
    return new LogSwitch(guiname, oncreate);
  }

  public static LogSwitch getFor(String guiname) {
    return getFor(guiname, DEFAULT_LEVEL);
  }

  public static LogSwitch getFor(Class claz, String suffix) {
    return getFor(shortName(claz, suffix));
  }

  public static LogSwitch getFor(Class claz) {
    return getFor(claz, null);
  }
  /////////////////////////////////////

  public static EasyCursor asProperties() {
    EasyCursor blob = new EasyCursor(/* presize to di*/);
    Vector<LogSwitch> debuggers = Sorted(); // get a second list copy to prevent exceptions
    for (LogSwitch bugger : debuggers) {
      try {
        LogLevelEnum bugnum = LogLevelEnum.values()[bugger.Level()];
        blob.saveEnum(bugger.Name(), bugnum);
      } catch (ArrayIndexOutOfBoundsException ignored) {
        blob.setInt(bugger.Name(), bugger.Level());
      }
    }
    return blob;
  }

  public static void apply(EasyCursor preloads) {
    debug("LogSwitch.apply(EasyCursor) begins!");
    debug("Levels:" + listLevels());
    if (preloads != null) {
      debug("preloads:" + preloads.asParagraph(OS.EOL));
      String name;
      LogSwitch ls;
      TextList keys = preloads.branchKeys();
      for (String key : keys.storage) {
        ls = find(key);
        String value = preloads.getString(key);//#all constructors make copy of value.

        int level = LogLevelEnum.levelFor(value);
        if (ls != null) {//if it exists change its setting
          ls.setto(level);
        } else {//create a new one
          //noinspection UnusedAssignment
          ls = new LogSwitch(key, level);
        }
      }
    }
    debug("Levels:" + listLevels());
    debug("LogSwitch.apply(EasyCursor) ends!");
  }

  public static boolean setOne(String name, LogLevelEnum lle) {
    LogSwitch ls = find(name);
    if (ls == null) {
      return false;
    } else {
      ls.setLevel(lle.level);
      return true;
    }
  }

  // move into registry +++
  private static void SetAll(int lle) {
    DEFAULT_LEVEL = lle;  //all existing, and all created hereafter!
    synchronized (registry) {
      for (int i = registry.size(); i-- > 0; ) {
        registry.elementAt(i).setto(lle);
      }
    }
  }
  ////////////////////

  public static void SetAll(LogLevelEnum lle) {
    SetAll(lle != null ? lle.level : DEFAULT_LEVEL);
  }

  public static void SetAll(String lvl) {
    SetAll(LogLevelEnum.valueOf(lvl)); //migth pass a null to
  }

  public static TextList listLevels() {
    Vector debuggers = Sorted();
    TextList responses = new TextList();
    int count = debuggers.size();
    for (int i = 0; i < count; i++) {
      LogSwitch ls = (LogSwitch) (debuggers.elementAt(i));
      responses.add(ls.Name() + ": " + ls.Level());
    }
    return responses;
  }

}

