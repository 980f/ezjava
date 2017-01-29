package pers.hal42.util;

import java.util.Vector;
import java.util.Enumeration;
import java.util.Collections;
import pers.hal42.lang.StringX;
import pers.hal42.lang.ReflectX;
import pers.hal42.lang.TrueEnum;

/**
* comparable is just for sorting for user displays.
*/
public class LogSwitch extends LogLevelEnum implements Comparable {

  protected String guiName;
  protected static int DEFAULT_LEVEL = LogLevelEnum.VERBOSE;//set for server, which has a harder time configuring than the client

  public final static LogLevelEnum VERBOSE= new LogLevelEnum(LogLevelEnum.VERBOSE);
  public final static LogLevelEnum WARNING= new LogLevelEnum(LogLevelEnum.WARNING);
  public final static LogLevelEnum ERROR= new LogLevelEnum(LogLevelEnum.ERROR);
  public final static LogLevelEnum OFF= new LogLevelEnum(LogLevelEnum.OFF);

  private static final char [] lvlLtr = {'-','/','!'};
  public static final char letter(int msgLevel) {
    return (((msgLevel > -1) && (msgLevel < lvlLtr.length)) ? lvlLtr[msgLevel] : ' ');
  }

  private static boolean debugme=false;//have to recompile to debug
  private static void debug(String msg){
    if(debugme){
      System.out.println(msg);
    }
  }

  void register(){
    if(LogSwitchRegistry.registry == null) {
      debug("LOGSWITCH REGISTRY IS NULL!!!");
    }
    synchronized (LogSwitchRegistry.registry) {//just to get size() to relate to us adding one.
      debug("registering " + Name());
      LogSwitchRegistry.registry.add(this); // sort +++ !!!
      debug("Registry size: " + LogSwitchRegistry.registry.size());
    }
  }

  private static final Vector All() {
    return LogSwitchRegistry.registry;
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
  public static final Vector Sorted() {
    Vector copy;
    synchronized (LogSwitchRegistry.registry) {
      copy=new Vector(LogSwitchRegistry.registry.size());
      copy.addAll(LogSwitchRegistry.registry);
    }
    Collections.sort(copy);  // sort them by name
    return copy;
  }

  private static final LogSwitch find(String name) {
    synchronized (LogSwitchRegistry.registry){
      for(int i=LogSwitchRegistry.registry.size();i-->0;){
        LogSwitch test = (LogSwitch)LogSwitchRegistry.registry.elementAt(i);
        if(test.Name().equals(name)) {
          return test;
        }
      }
    }
    return null;//doesn't exist, don't make one here!
  }

  private static final LogSwitch find(Class claz,String suffix) {
    return find(shortName(claz,suffix));
  }

  private static final LogSwitch find(Class claz) {
    return find(shortName(claz));
  }

  public static boolean exists(String clasname){
    return find(clasname)!=null;
  }

  /**
   * find or create a new LogSwitch
   */
  public static final LogSwitch getFor(String guiname,int oncreate) {
    synchronized (LogSwitchRegistry.registry) {
      LogSwitch forclass=find(guiname);
      if(forclass==null){
        forclass=new LogSwitch(guiname);
        forclass.setto(oncreate);
      }
      return forclass;
    }
  }

  public static final LogSwitch getFor(String guiname) {
    return getFor(guiname,DEFAULT_LEVEL);
  }

  public static final LogSwitch getFor(Class claz,String suffix) {
    return getFor(shortName(claz,suffix));
  }

  public static final LogSwitch getFor(Class claz) {
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


  public static final EasyCursor asProperties(){
    EasyCursor blob=new EasyCursor(/* presize to di*/);
    Vector debuggers = Sorted(); // get a second list copy to prevent exceptions
    for(int i=0;i<debuggers.size();i++){
      LogSwitch bugger = (LogSwitch)debuggers.elementAt(i);
      blob.saveEnum(bugger.Name(),bugger);
    }
    return blob;
  }

  public String toSpam(){
    return Name() + ": " + Image();
  }

  public static final void apply(EasyCursor preloads){
    debug("LogSwitch.apply(EasyCursor) begins!");
    debug("Levels:"+listLevels());
    if(preloads!=null){
      debug("preloads:"+preloads.asParagraph(OS.EOL));
      String name;
      LogLevelEnum value=new LogLevelEnum();
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
    synchronized (LogSwitchRegistry.registry) {
      for(int i=LogSwitchRegistry.registry.size();i-->0;){
        ((LogSwitch)LogSwitchRegistry.registry.elementAt(i)).setto(lle);
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
    super(spam);
    if(!isLegal()){
      setto(DEFAULT_LEVEL);//truenum's leaves it more than verbose.
    }
    guiName=lookup;//net.paymate stripping now done in errorLogStream.
    register();
  }

  private  LogSwitch(String lookup) {
    this(lookup,DEFAULT_LEVEL);
  }
  /////////////////////////////////////

  public LogLevelEnum Level(){
    return this;//super;
  }

  public LogSwitch setLevel(String fromenum){
    setto(fromenum);
    return this;
  }

  public LogSwitch setLevel(int level){
    setto(level);
    // +++ can we have this send out a message when it changes, to show that it changed?
    return this;
  }

  public LogSwitch setLevel(LogLevelEnum lle){
    setto(lle.Value());
    return this;
  }

  public String Name(){
    return guiName;
  }
  ////////////////////

  /**
  * if argument is the same or more severe than internal level we shall do something
  */
  public boolean passes(int llev){
    return (llev>=value) && (value!=LogLevelEnum.OFF) && (value!=LogLevelEnum.invalid);
  }

  public boolean is(LogLevelEnum llev){
    return llev.Value()==Value();
  }

}

