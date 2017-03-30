package pers.hal42.logging;

import pers.hal42.lang.*;
import pers.hal42.text.TextList;
import pers.hal42.transport.EasyCursor;

import java.util.Collections;
import java.util.Vector;

import static pers.hal42.logging.LogSwitchRegistry.registry;

/**
* comparable is just for sorting for user displays.
*/
public class LogSwitch implements Comparable {
  public LogLevelEnum level;
  public static LogLevelEnum legacy(int level){
    //set level to matching ordinal
    try {
      return LogLevelEnum.values()[level];
    } catch(ArrayIndexOutOfBoundsException ignored){
      return LogLevelEnum.ERROR;
    }
  }

  public void setto(LogLevelEnum level){
    this.level=level;
  }

  protected String guiName;
  protected static LogLevelEnum DEFAULT_LEVEL = LogLevelEnum.VERBOSE;//set for server, which has a harder time configuring than the client

  private static final char [] lvlLtr = {'-','/','!'};
  public static char letter(int msgLevel) {
    return (((msgLevel > -1) && (msgLevel < lvlLtr.length)) ? lvlLtr[msgLevel] : ' ');
  }

  private static boolean debugme=false;//have to recompile to debug
  private static void debug(String msg){
    if(debugme){
      System.out.println(msg);
    }
  }

  void register(){
    if(registry == null) {
      debug("LOGSWITCH REGISTRY IS NULL!!!");
    }
    synchronized (registry) {//just to get size() to relate to us adding one.
      debug("registering " + Name());
      registry.add(this); // sort +++ !!!
      debug("Registry size: " + registry.size());
    }
  }

  private static Vector All() {
    return registry;
  }

  public static String shortName(Class claz,String suffix){
    if(StringX.NonTrivial(suffix)){
      return ReflectX.shortClassName(claz)+"."+suffix;
    } else {
      return ReflectX.shortClassName(claz);
    }
  }

  public static String shortName(Class claz){
    return shortName(claz, null);
  }

  /**
   * interface for editing:
   *
   * Gives a "copy" of the list of pointers (but doesn't copy each item, still points to originals)
   * and sorts them.  Use this for external classes where possible.
   *
   * +++ Create a separate LogSwitchList class that has an itemAt() function
   * that this class and other classes can use! +++
   */
  public static Vector Sorted() {
    Vector<LogSwitch> copy;
    synchronized (registry) {
      copy=new Vector<>(registry.size());
      copy.addAll(registry);
    }
    Collections.sort(copy);  // sort them by name
    return copy;
  }

  private static  LogSwitch find(String name) {
    synchronized (registry){
      for(int i = registry.size(); i-->0;){
        LogSwitch test = (LogSwitch) registry.elementAt(i);
        if(test.Name().equals(name)) {
          return test;
        }
      }
    }
    return null;//doesn't exist, don't make one here!
  }

  private static  LogSwitch find(Class claz,String suffix) {
    return find(shortName(claz,suffix));
  }

  private static  LogSwitch find(Class claz) {
    return find(shortName(claz));
  }

  public static boolean exists(String clasname){
    return find(clasname)!=null;
  }

  /**
   * find or create a new LogSwitch
   */
  public static  LogSwitch getFor(String guiname,int oncreate) {
    synchronized (registry) {
      LogSwitch forclass=find(guiname);
      if(forclass==null){
        forclass=new LogSwitch(guiname);
        forclass.setto(legacy(oncreate));
      }
      return forclass;
    }
  }

  /** @return existing one by the @param guiname, else create a new one and set its level to 'oncreate' */
  public static  LogSwitch getFor(String guiname,LogLevelEnum oncreate) {
    for(LogSwitch item:LogSwitchRegistry.registry){
      if(item.Name().equals(guiname)){
        return item;
      }
    }
    return new LogSwitch(guiname, Safe.deNull(oncreate,DEFAULT_LEVEL));
  }

  public static LogSwitch getFor(String guiname) {
    return getFor(guiname,DEFAULT_LEVEL);
  }

  public static LogSwitch getFor(Class claz,String suffix) {
    return getFor(shortName(claz,suffix));
  }

  public static LogSwitch getFor(Class claz) {
    return getFor(claz,null);
  }


  // for sorting for display purposes
  public int compareTo(Object o) {
    if((o == null) || (!(o instanceof LogSwitch))) {
      throw(new ClassCastException());//per the interface specification
    }
    LogSwitch that = (LogSwitch)o;
    return Name().compareTo(that.Name());
  }


  public static EasyCursor asProperties(){
    EasyCursor blob=new EasyCursor(/* presize to di*/);
    Vector<LogSwitch> debuggers = Sorted(); // get a second list copy to prevent exceptions
    for(LogSwitch bugger:debuggers){
      blob.saveEnum(bugger.Name(),bugger);
    }
    return blob;
  }

  public String toSpam(){
    return Name() + ": " + Image();
  }

  public static void apply(EasyCursor preloads){
    debug("LogSwitch.apply(EasyCursor) begins!");
    debug("Levels:"+listLevels());
    if(preloads!=null){
      debug("preloads:"+preloads.asParagraph(OS.EOL));
      String name;
      LogSwitch ls;
      TextList names=preloads.branchKeys();
      for(int i=names.size();i-->0;){
        name=names.itemAt(i);
        value.setto(preloads.getString(name));//#all constructors make copy of value.
        ls=find(name);
        if(ls!=null){//if it exists change its setting
          ls.setto(value.Value());
        } else {//create a new one
          ls=new LogSwitch(name,value);
        }
      }
    }
    debug("Levels:"+listLevels());
    debug("LogSwitch.apply(EasyCursor) ends!");
  }

  public static final boolean setOne(String name, LogLevelEnum lle) {
    LogSwitch ls = find(name);
    if(ls==null) {
      return false;
    } else {
      ls.setLevel(lle.Value());
      return true;
    }
  }

  // move into registry +++
  private static void SetAll(int lle){
    DEFAULT_LEVEL=lle;  //all existing, and all created hereafter!
    synchronized (registry) {
      for(int i = registry.size(); i-->0;){
        ((LogSwitch) registry.elementAt(i)).setto(lle);
      }
    }
  }

  public static final void SetAll(LogLevelEnum lle){
    SetAll(lle.Value());
  }

  public static final void SetAll(String lvl){
    SetAll(new LogLevelEnum(lvl));
  }


  public static final TextList listLevels() {
    Vector debuggers = Sorted();
    TextList responses = new TextList();
    int count = debuggers.size();
    for(int i=0;i<count;i++){
      LogSwitch ls = (LogSwitch)(debuggers.elementAt(i));
      responses.add(ls.Name() + ": " + ls.Level());
    }
    return responses;
  }

  //////////////////////////////////////////////////////
  private LogSwitch(String lookup,LogLevelEnum lle) {
    this(lookup,lle.Value());
  }


  private LogSwitch(String lookup,int spam) {
    level=legacy(spam);
//    if(!isLegal()){
//      setto(DEFAULT_LEVEL);//truenum's leaves it more than verbose.
//    }
    guiName=lookup;
    register();
  }

  private  LogSwitch(String lookup) {
    this(lookup,DEFAULT_LEVEL);
  }
  /////////////////////////////////////

  public LogLevelEnum Level(){
    return level;
  }

  public LogSwitch setLevel(String fromenum){
    level=LogLevelEnum.valueOf(fromenum);
    return this;
  }

  public LogSwitch setLevel(LogLevelEnum lle){
    level=lle;
    return this;
  }

  public String Name(){
    return guiName;
  }
  ////////////////////

  /**
  * if argument is the same or more severe than internal level we shall do something
  */
  public boolean passes(LogLevelEnum llev){
    return (llev.ordinal()>=level.ordinal()) && (level!=LogLevelEnum.OFF);
  }

  public boolean is(LogLevelEnum llev){
    return llev==level;
  }

}

