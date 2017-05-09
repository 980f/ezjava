package pers.hal42.lang;

import pers.hal42.logging.ErrorLogStream;
import pers.hal42.text.TextList;
import pers.hal42.transport.EasyCursor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;


/* to get rid of debug create an exception object with all of the messages that are currently debug messages placed inside of it.
 */

public class ReflectX {

  // not a good idea to have a dbg in here --- see preLoadClass
  private ReflectX() {
    // I exist for static reasons only.
  }

  public static boolean isImplementorOf(Class claz,Class probate){
    if (claz !=null && probate != null) {
      Class supported[];
      supported=claz.getInterfaces();
      for (int i=supported.length;i-->0;){
        if (supported[i]==probate) {
          return true;
        }
      }
    }
    return false;
  }
  private static final String packageRoot="pers.hal42.";//todo: better means of default package root.
  /**
   * @return string stripped of paymates package root IFF that root is present.
   */
  public static String stripPackageRoot(String lookup) {
    return StringX.replace(lookup, packageRoot, "");
  }
  /**
   * @return the class name with out package of @param classy
   */
  public static String justClassName(Class classy){
    return classy!=null?StringX.afterLastDot(classy.getName()):"NULL";
  }
  /**
   * @return the class name with out package of @param instance
   */
  public static String justClassName(Object instance){
    return instance!=null? justClassName(instance.getClass()):"NULL";
  }
  /**
   * @return the class name without the application's root.
   */
  public static String shortClassName(Class classy){
    return classy!=null? stripPackageRoot(classy.getName()):"NULL";
  }
  /**
   * @return the class name without "packageRoot" if that is present.
   */
  public static String shortClassName(Object instance){
    return instance!=null? shortClassName(instance.getClass()):"NULL";
  }

  /**
   * @return @param child prefixed with the class name without the packageRoot if that is present.
   */
  public static String shortClassName(Object instance,String child){
    return (instance!=null? shortClassName(instance.getClass()):"NULL")+"."+child;
  }

/**
 * @return class object given classname, null on errors (doesn't throw exceptions)
 */
  public static Class classForName(String classname){
    try {
      return Class.forName(classname.trim());
    }
    catch (ClassNotFoundException cfe){
      if( ! classname.startsWith(packageRoot)){
        return classForName(packageRoot+classname);//2nd chance
      }
      return null;
    }
    catch (Exception ex) {
      return null;
    }
  }
/**
 * @return no-args constructor instance of given class.
 * will try to prefix package root to classes that aren't found.
 * On "not found" returns null,
 * todo:? change to returning the exception!
 */
  public static Object newInstance(String classname){
    try {
      return classForName(classname).newInstance();
    }
    catch (Exception ex) {
      return null;
    }
  }
/**
 * @return constructor for class @param claz with single argument @param argclaz
 */
  public static Constructor constructorFor(Class claz,Class argclaz){
    try {
      Constructor [] ctors=claz.getConstructors();
      for(int i=ctors.length;i-->0;){
        Constructor ctor=ctors[i];
        Class[] plist=ctor.getParameterTypes();
        if(plist.length==1 && plist[0]==argclaz){
          return ctor;
        }
      }
      return null;
    }
    catch (Exception ex) {
      return null;
    }
  }
  /**
   * @return new instance of @param classname, constructed with single argument @param arg1
   */
  public static Object newInstance(String classname,Object arg1){
    try {
      Object []arglist=new Object[1];
      arglist[0]=arg1;
      return constructorFor(classForName(classname),arg1.getClass()).newInstance(arglist);
    }
    catch (Exception ex){//all sorts of null pointer failures get you here.
      System.out.println("failed newInstance: "+ex.getLocalizedMessage());
      ex.printStackTrace(System.out);
      return null;
    }
  }


  /**
   * make an object given an @param ezc EasyCursor description of it.
   */
  public static Object Create(EasyCursor ezc, ErrorLogStream dbg){
    try {
      dbg=ErrorLogStream.NonNull(dbg);//ensure a bad debugger doesn't croak us.
      Class[] creatorArgs = {EasyCursor.class};
      Object [] arglist = { ezc } ;
      String classname= ezc.getString("class");
      dbg.VERBOSE(StringX.bracketed("Create:classname(",classname));
      Class  claz= classForName(classname);
      dbg.VERBOSE("Create:class:"+claz);
      Method meth= claz.getMethod("Create",creatorArgs);
      dbg.VERBOSE("Create:Method:"+meth);
      Object obj=  meth.invoke(null,arglist);
      dbg.VERBOSE("Create:Object:"+obj);
      return obj;
    } catch(Exception any){
      dbg.WARNING(any.getMessage()+" In Create(ezc), "+"properties:"+ezc.asParagraph());
      return null;
    }
  }

  public static Object Create(EasyCursor ezc){
    return  Create(ezc,ErrorLogStream.Global());
  }



  // +++ move to ObjectX?
  public static String ObjectInfo(Object obj){
    return obj==null?" null!":" type: "+shortClassName(obj);
  }

/**
 * debug was removed from the following function as it is called during the initialization
 * of the classes need by the debug stuff. Any debug will have to be raw stdout debugging.
 *
 * Try getting them from the textlist below, instead.
 */
  public static TextList preloadClassErrors;
  public static boolean preloadClass(String className, boolean loadObject) {
    try {
      Class c = classForName(className);
      if(loadObject) {
        c.newInstance(); // some drivers don't load completely until you do this
      }
      return true;
    } catch (Exception e) {
      if(preloadClassErrors==null){
        preloadClassErrors = new TextList();
      }
      preloadClassErrors.add("Exception \""+e+"\" loading class \"" + className + "\" with" + (loadObject ? "" : "out") + " loading object");
      return false;
    }
  }

  public static boolean preloadClass(String className) {
    return preloadClass(className, false);
  }

}
