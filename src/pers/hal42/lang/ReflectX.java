package pers.hal42.lang;

import pers.hal42.logging.ErrorLogStream;
import pers.hal42.text.TextList;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.Iterator;
import java.util.function.Predicate;

/**
 * Utilities to ease use of reflection.
 * <p>
 * todo:2 get rid of debug class use by create an exception object with all of the messages that are currently debug messages placed inside of it.
 */

public class ReflectX {

  /**
   * marker annotation for use by applyTo and apply()
   * if class is annotated with @Stored then process all fields except those marked with this.
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.FIELD, ElementType.TYPE})
  public @interface Ignore {
    /**
     * if nontrivial AND child is not found by the field name then it is sought by this name.
     */
    String legacy() default "";
  }

  public static int Choices(Class<? extends Enum> eClass) {
    return eClass.getEnumConstants().length;
  }

  /** if Object o has a String field named 'name' then set that field's value to the name of o in parent*/
  public static void eponomize(Object o, Object parent) {
    Class claz=o.getClass();
    final Field[] fields = claz.getFields();
    for (Field field : fields) {
      if (field.getName().equals("name")) {  //object has a 'name' field
        Field wrapper=fieldForChild(o,parent);
        if(wrapper!=null){
          try {
            field.set(o,wrapper.getName());
          } catch (IllegalAccessException e) {
//            dbg.Caught(e);
          }
        }
        break;
      }
    }
  }

  public static Field fieldForChild(Object child, Object parent){
    final Field[] fields = parent.getClass().getFields();
    for (final Field field : fields) {
      try {
        final Object o = field.get(parent);
        if(o ==child){
          return field;
        }
      } catch (IllegalAccessException e) {
//        dbg.Caught(e);
      }
    }
    return null;
  }

  public static String nameOfChild(Object child, Object parent){
    Field field=fieldForChild(child,parent);
    if(field!=null){
      return field.getName();
    }
    return null;
  }

  /**
   * iterate deeply through a class hierarchy.
   * Annoyingly java makes us choose between getting public hierarchy or only local for all member classes.
   * One could mix those up and get all members of self and public hierarchy
   */
  public static class ClassWalker implements Iterator<Class> {
    final Class node;//the starting point, retained for debug
    final boolean local;
    protected Class[] parts;//
    protected int ci; //class index

    //todo:1 double link list for efficiency, or otherwise maintain pointer to most deeply nested instance. Present implementation chases the list with every iteration step.
    protected ClassWalker nested;

    public ClassWalker(Class node, boolean local) {
      this.node = node;
      this.local = local;
      parts = local ? node.getDeclaredClasses() : node.getClasses();
      ci = parts.length;
      nest();
    }

    public void nest() {
      nested = ci > 0 ? new ClassWalker(parts[--ci], local) : null;//recurses
    }

    @Override
    public boolean hasNext() {
      if (nested != null) {
        if (nested.hasNext()) {
          return true;
        } else {
          nest();//has a --ci or nested <- null;
          return hasNext();
        }
      } else {
        return ci >= 0;
      }
    }

    @Override
    public Class next() {
      if (nested != null) {
        return nested.next();
      } else {
        --ci;
        return node;
      }
    }
  }

  public static class FieldWalker implements Iterator<Field> {
    final Class ref;//the starting point, retained for debug
    final boolean local;
    ClassWalker cw;
    Class current;
    Field[] fields;
    int fi;

    public FieldWalker(Class ref, boolean local) {
      this.ref = ref;
      this.local = local;
      cw = new ClassWalker(ref, local);
      nextClass();
    }

    public void nextClass() {
      if (cw.hasNext()) {
        current = cw.next();
        fields = local ? current.getDeclaredFields() : current.getFields();
        fi = fields.length;
      } else {
        fields = null;
        fi = 0;
      }
    }

    public boolean hasNext() {
      if (fi > 0) {
        return true;
      } else {
        nextClass();
        return fi > 0;
      }
    }

    /**
     * @returns either a field if there is still one or null.
     */
    public Field next() {
      if (fi > 0) {
        return fields[--fi];
      } else {
        return null;//should not have been called.
      }
    }
  }


  public static TextList preloadClassErrors;
  private static final String packageRoot = "pers.hal42.";//todo:1 better means of default package root.

  // not a good idea to have a dbg in here --- see preLoadClass
  private ReflectX() {
    // I exist for static reasons only.
  }


  @SuppressWarnings("unchecked")
  public static <T> T CopyConstruct(T prefill) {
    Constructor<T> maker = constructorFor((Class<? extends T>) prefill.getClass(), prefill.getClass());
    try {
      return maker != null ? maker.newInstance(prefill) : null;
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      return null;
    }
  }

  /**
   * @returns @param ordinal th enum constant, or null if ordinal out of range or there are no such constants
   */
  public static Object enumByOrdinal(Class fclaz, int ordinal) {
    Object[] set = fclaz.getEnumConstants();
    if (ordinal < 0 || ordinal >= set.length) {
      return null;
    } else {
      return set[ordinal];
    }
  }

  /** marker for a method that can parse an enum. Used by parseEnum for when the default enum parser faults. */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD})
  public @interface Parser {

  }

  @SuppressWarnings("unchecked")
  public static <T extends Enum> T parseEnum(Class<T> Tclaz, String image) {
    try {
      return (T) Enum.valueOf(Tclaz, image); //best case is perfect match
    } catch (IllegalArgumentException e) {
      final Method parser = methodFor(Tclaz, Parser.class, null);
      if (parser != null) {
        parser.setAccessible(true);
        try {
          return (T) parser.invoke(null, new Object[]{image});
        } catch (IllegalAccessException | InvocationTargetException e1) {
          return null;
        }
      }
      return null;
    } catch (NullPointerException e) {
      return null;
    }
  }

  /**
   * @return factory method for class @param claz with @param args. No arg may be null!
   */
  public static Method factoryFor(Class claz, Object... args) {
    Method[] candidates = claz.getMethods();
    for (Method m : candidates) {
      int mods = m.getModifiers();
      if (Modifier.isStatic(mods)) {
        Class[] plist = m.getParameterTypes();
        if (isArglist(plist, args)) {
          return m;
        }
      }
    }
    return null;
  }

  public static boolean isArglist(Class[] plist, Object[] args) {
    if (plist.length == args.length) {
      for (int i = plist.length; i-- > 0; ) {
        final Class<?> probate = args[i].getClass();
        if (isImplementorOf(probate, plist[i])) {
          return true;
        }
      }
    }
    return false;
  }


  /**
   * @return constructor for class @param claz with single argument @param argclaz
   */
  public static Method factoryFor(Class claz, Class argclaz) {
    Method[] candidates = claz.getMethods();
    for (Method m : candidates) {
      int mods = m.getModifiers();
      if (Modifier.isStatic(mods)) {
        Class[] plist = m.getParameterTypes();
        if (plist.length == 1 && plist[0] == argclaz) {
          return m;
        }
      }
    }
    return null;
  }


  public static Object enumObject(Class fclaz, String string) {
    if (string == null) {
      return null;
    }
    Method factory = factoryFor(fclaz, String.class);
    if (factory != null) {
      try {
        return factory.invoke(string);
      } catch (IllegalAccessException | InvocationTargetException e) {
        ErrorLogStream.Global().Caught(e, "ReflectX.enumObject on {0}", fclaz);
        return null;
      }
    }
    //look for constructor, not quite limiting ourselves to enums if we do.
    return null;
  }

  /**
   * @returns whether @param claz implements @param probate
   */
  public static boolean isImplementorOf(Class probate, Class claz) {
    if (claz != null && probate != null) {
      if (claz.equals(probate)) {
        return true; //frequent case
      }
      //if probate is an extension of claz
      final Class[] dclasses = probate.getDeclaredClasses();
      for (Class it :dclasses) {
        if (it.equals(claz)) {
          return true;
        }
      }
      //if probate implements an interface
      final Class[] ifaces = probate.getInterfaces();
      for (Class it : ifaces) {
        if (it.equals(claz)) {
          return true;
        }
      }
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  public static <T> T getAnnotation(Class ref, Class annotation) {
    return (T) ref.getAnnotation(annotation);
  }

  public static boolean ignorable(Field field) {
    boolean skipper = field.isAnnotationPresent(Ignore.class);
    int modifiers = field.getModifiers();
    skipper |= Modifier.isFinal(modifiers);
    skipper |= Modifier.isTransient(modifiers);
    return skipper;
  }

  /** @returns in essence whether field's class has fields. The internals of this method are occasionally reordered for performance of particular users. */
  public static boolean isScalar(Field field) {
    Class type=field.getType();
    if(int.class==type){
      return true;
    }
    if(boolean.class==type){
      return true;
    }
    if(double.class==type){
      return true;
    }
    if(long.class==type){
      return true;
    }
    if (String.class == type) {
      return true;
    }
    if (float.class == type) {
      return true;
    }
    if (short.class == type) {
      return true;
    }
    if(char.class==type){
      return true;
    }
    //noinspection RedundantIfStatement
    if(byte.class==type){
      return true;
    }
    return false;
  }

  /**
   * @return string stripped of paymates package root IFF that root is present.
   */
  public static String stripPackageRoot(String lookup) {
    return StringX.replace(lookup, packageRoot, "");
  }

  /**
   * @return the class name with out package of @param classy
   */
  public static String justClassName(Class classy) {
    if (classy != null) {
      String fullname = classy.getName();
      Package packer = classy.getPackage();
      String packname = packer.getName();
      if (fullname.startsWith(packname)) {
        //below creates CamelCase name
        return StringX.removeAll("$*?:;", fullname.substring(packname.length() + 1));
      } else {
        return fullname;//let caller figure out what the problem is
      }
    } else {
      return "NULL";
    }
  }

  /**
   * @return the class name with out package of @param instance
   */
  public static String justClassName(Object instance) {
    return instance != null ? justClassName(instance.getClass()) : "NULL";
  }

  /**
   * @return the class name without the application's root.
   * @deprecated at 1.5 use Class.getSimpleName()
   */
  public static String shortClassName(Class classy) {
    return classy != null ? stripPackageRoot(classy.getName()) : "NULL";
  }

  /**
   * @return the class name without "packageRoot" if that is present.
   */
  public static String shortClassName(Object instance) {
    return instance != null ? shortClassName(instance.getClass()) : "NULL";
  }

  /**
   * @return @param child prefixed with the class name without the packageRoot if that is present.
   */
  public static String shortClassName(Object instance, String child) {
    return (instance != null ? shortClassName(instance.getClass()) : "NULL") + "." + child;
  }

  /**
   * @return class object given classname, null on errors (doesn't throw exceptions)
   */
  public static Class classForName(String classname) {
    try {
      return Class.forName(classname.trim());
    } catch (ClassNotFoundException cfe) {
      if (!classname.startsWith(packageRoot)) {
        return classForName(packageRoot + classname);//2nd chance
      }
      return null;
    }
  }

  /**
   * @return no-args constructor instance of given class.
   * will try to prefix package root to classes that aren't found.
   * On "not found" returns null,
   */
  public static Object newInstance(String classname) {
    try {
      //noinspection ConstantConditions
      return classForName(classname).newInstance();
    } catch (IllegalAccessException | InstantiationException e) {
      return null;
    }
  }

  /**
   * @return constructor for class @param claz with single argument @param argclaz
   */
  @SuppressWarnings("unchecked")
  public static <T> Constructor<T> constructorFor(Class<? extends T> claz, Object... args) {
    try {
      Constructor[] ctors = claz.getConstructors();
      for (int i = ctors.length; i-- > 0; ) {
        Constructor ctor = ctors[i];
        Class[] plist = ctor.getParameterTypes();
        if (isArglist(plist, args)) {
          return ctor;
        }
      }
      return null;
    } catch (Exception ex) {
      return null;
    }
  }

  /**
   * @return constructor for class @param claz with single argument @param argclaz
   */
  @SuppressWarnings("unchecked")
  public static <T> Constructor<T> constructorFor(Class<? extends T> claz, Class argclaz) {
    try {
      Constructor[] ctors = claz.getConstructors();
      for (int i = ctors.length; i-- > 0; ) {
        Constructor ctor = ctors[i];
        Class[] plist = ctor.getParameterTypes();
        if (plist.length == 1 && plist[0] == argclaz) {
          return ctor;
        }
      }
      return null;
    } catch (Exception ex) {
      return null;
    }
  }

  /**
   * @return new instance of @param classname, constructed with single argument @param arg1
   */
  public static Object newInstance(String classname, Object arg1) {
    try {
      Object[] arglist = new Object[1];
      arglist[0] = arg1;
      //noinspection ConstantConditions
      return constructorFor(classForName(classname), arg1.getClass()).newInstance(arglist);
    } catch (Exception ex) {//all sorts of null pointer failures get you here.
      System.out.println("failed newInstance: " + ex.getLocalizedMessage());
      ex.printStackTrace(System.out);
      return null;
    }
  }

  /** try to find a static constructor, if not then find a constructor. first one with matching args list wins. @returns new object or null if something fails */
  @SuppressWarnings("unchecked")
  public static <T> T newInstance(Class tclaz, Object... args) {
    final Method cfactory = ReflectX.factoryFor(tclaz, args);
    if (cfactory != null) {
      try {
        return (T) cfactory.invoke(args);
      } catch (IllegalAccessException | InvocationTargetException e) {
        return null;
      }
    }
    final Constructor maker = constructorFor(tclaz, args);
    if (maker != null) {
      try {
        return (T) maker.newInstance(args);
      } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
        return null;
      }
    }
    return null;//no available factory
  }

  /**
   * @returns name of @param child within object @param parent, null if not an accessible child
   */
  public static String fieldName(Object parent, Object child) {
    for (Field field : parent.getClass().getDeclaredFields()) {
      try {
        Object probate = field.get(parent);
        if (probate == child) {
          return field.getName();
        }
      } catch (IllegalAccessException ignored) {
        //
      }
    }
    return null;
  }

  /** @returns a method of @param claz that has an annotation of @param noted which matches the optional argument @param matches. The compiler chooses which if there are more than one. */
  public static <A extends Annotation> Method methodFor(Class claz, Class<A> noted, Predicate<A> matches) {
    for (Method method : claz.getDeclaredMethods()) {
      A annotation = method.getAnnotation(noted);
      if (annotation != null && (matches == null || matches.test(annotation))) {
        method.setAccessible(true);
        return method;
      }
    }
//    for (Method method : claz.getMethods()) {
//      A annotation = method.getAnnotation(noted);
//      if (annotation != null && (matches == null || matches.test(annotation))) {
//         return method;
//      }
//    }

    return null;
  }
//
//  /**
//   * make an object given an @param ezc EasyCursor description of it.
//   */
//  public static Object Create(EasyCursor ezc, ErrorLogStream dbg) {
//    try {
//      dbg = ErrorLogStream.NonNull(dbg);//ensure a bad debugger doesn't croak us.
//      Class[] creatorArgs = {EasyCursor.class};
//      Object[] arglist = {ezc};
//      String classname = ezc.getString("class");
//      dbg.VERBOSE(StringX.bracketed("Create:classname(", classname));
//      Class claz = classForName(classname);
//      if (claz != null) {
//        dbg.VERBOSE("Create:class:" + claz);
//        //noinspection unchecked
//        Method meth = claz.getMethod("Create", creatorArgs);
//        dbg.VERBOSE("Create:Method:" + meth);
//        Object obj = meth.invoke(null, arglist);
//        dbg.VERBOSE("Create:Object:" + obj);
//        return obj;
//      }
//      return null;
//    } catch (Exception any) {
//      dbg.WARNING(" Ezc.Create() got {0}, properties: {1}", any.getMessage(), ezc.asParagraph());
//      return null;
//    }
//  }
//
//  public static Object Create(EasyCursor ezc) {
//    return Create(ezc, ErrorLogStream.Global());
//  }

  //todo:2 move to ObjectX?
  public static String ObjectInfo(Object obj) {
    return obj == null ? " null!" : " type: " + shortClassName(obj);
  }

  public static Object loadClass(String className) {
    try {
      Class c = Class.forName(className);
      return c.newInstance(); // some drivers don't load completely until you do this
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * debug was removed from the following function as it is called during the initialization
   * of the classes need by the debug stuff. Any debug will have to be raw stdout debugging.
   * <p>
   * Try getting them from the preloadClassErrors ..
   */
  public static boolean preloadClass(String className, boolean loadObject) {
    try {
      Class c = classForName(className);
      if (loadObject && c != null) {
        return c.newInstance() != null; // some drivers don't load completely until you do this
      }
      return c != null;
    } catch (Exception e) {
      if (preloadClassErrors == null) {
        preloadClassErrors = new TextList();
      }
      preloadClassErrors.add("Exception \"" + e + "\" loading class \"" + className + "\" with" + (loadObject ? "" : "out") + " loading object");
      return false;
    }
  }

  public static boolean preloadClass(String className) {
    return preloadClass(className, false);
  }
}
