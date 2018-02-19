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
   * poorly tested as to scope, this gives the types of a map when given a field that is or is derived from a Map.
   */
//  It will most likely hang your program if used with any other type.
  public static Type[] getInterfaceTypes(Field field, int numTypes) {
    Class<?> aClass = field.getType();
    Type[] genericInterfaces = aClass.getGenericInterfaces();
    for (Type type = field.getGenericType(); type != Object.class; type = aClass.getGenericSuperclass()) {
      if (type instanceof ParameterizedType) {
        Type[] types = ((ParameterizedType) type).getActualTypeArguments();
        if (types.length == numTypes) {//then we hopefully have the desired interface
          return types;
        }
      }
    }
    return new Type[0];
  }

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

  /**
   * @deprecated NYI
   */
  public static Field findGlobalField(String key) {
    for (int searchSplit = key.lastIndexOf('.'); searchSplit >= 0; ) {
      String classname = key.substring(0, searchSplit);
      try {
        final Class<?> parent = Class.forName(classname);
//todo: drill through remaining fields to get child.       parent.getField()
      } catch (ClassNotFoundException e) {
        //dbg.Caught(e);
        searchSplit = classname.lastIndexOf('.');
      }
    }
    return null;
  }

  /**
   * @returns whether method was invoked
   */
  public static boolean doMethodNamed(Object o, String methodName, Object... args) {
    try {
      Class[] arglist = new Class[args.length];
      for (int i = args.length; i-- > 0; ) {
        arglist[i] = args[i].getClass();
      }
      Method method = o.getClass().getMethod(methodName, arglist);
      method.invoke(o, args);
      return true;
    } catch (NoSuchMethodException | NullPointerException | InvocationTargetException | IllegalAccessException e) {
      return false;
    }
  }

  /**
   * run a method, @returns null if the method invocation faulted, which can't be distinguished from a method which might return a null.
   */
  @SuppressWarnings("unchecked")
  public static <R> R doMethodWithReturn(Object obj, Method method, Object... args) {
    if (method != null) {
      try {
        return (R) method.invoke(obj, args);
      } catch (IllegalAccessException | InvocationTargetException e) {
        return null;
      }
    }
    return null;
  }

  /**
   * @returns whether method @param parser was invoked.
   */
  public static boolean doMethod(Object obj, Method method, Object... args) {
    if (method != null) {
      try {
        method.invoke(obj, args);
        return true;
      } catch (IllegalAccessException | InvocationTargetException e) {
        return false;
      }
    }
    return false;
  }

  public static int Choices(Class<? extends Enum> eClass) {
    return eClass.getEnumConstants().length;
  }

  /**
   * if Object o has a String field named 'name' then set that field's value to the name of o in parent
   */
  public static void eponomize(Object o, Object parent) {
    Class claz = o.getClass();
    final Field[] fields = claz.getFields();
    for (Field field : fields) {
      if (field.getName().equals("name")) {  //object has a 'name' field
        Field wrapper = fieldForChild(o, parent);
        if (wrapper != null) {
          try {
            field.set(o, wrapper.getName());
          } catch (IllegalAccessException e) {
//            dbg.Caught(e);
          }
        }
        break;
      }
    }
  }

  public static Field fieldForChild(Object child, Object parent) {
    final Field[] fields = parent.getClass().getFields();
    for (final Field field : fields) {
      try {
        final Object o = field.get(parent);
        if (o == child) {
          return field;
        }
      } catch (IllegalAccessException e) {
//        dbg.Caught(e);
      }
    }
    return null;
  }

  public static String nameOfChild(Object child, Object parent) {
    Field field = fieldForChild(child, parent);
    if (field != null) {
      return field.getName();
    }
    return null;
  }

  public static boolean isImplementorOf(Field probate, Class claz) {
    return isImplementorOf(probate.getClass(), claz);
  }

  /**
   * @returns name of @param child within object @param parent, null if not an accessible child
   */
  public static String fieldName(Object parent, Object child) {
    for (Field field : parent.getClass().getDeclaredFields()) {
      try {
        Object probate = field.get(parent);
        if (probate == child) {
          field.setAccessible(true);//maybe gratuitous but is fast.
          return field.getName();
        }
      } catch (IllegalAccessException ignored) {
        //
      }
    }
    return null;
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

  /**
   * @returns in essence (negative of) whether field's class has fields.
   */
  public static boolean isScalar(Field field) {
    Class type = field.getType();
    return type != Void.class && (type.isPrimitive() || type == String.class || type.isEnum());
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
   * @returns whether @param probate can be cast to @param claz
   */
  public static boolean isImplementorOf(Class probate, Class claz) {
    if (claz != null && probate != null) {
      if (claz.equals(probate)) {
        return true; //frequent case
      }
      //noinspection unchecked
      if (claz.isAssignableFrom(probate)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @returns name of @param child within object @param parent, null if not an accessible child
   */
  public static Field findField(Object root, Object child) {
    if (child == root) {
      return null;
    }
    for (Field field : root.getClass().getDeclaredFields()) {
      try {
        Object probate = field.get(root);
        if (probate == child) {
          field.setAccessible(true);//maybe gratuitous but is fast.
          return field;
        }
      } catch (IllegalAccessException ignored) {
        //
      }
    }
    return null;
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

  /**
   * try to find a static constructor, if not then find a constructor. first one with matching args list wins. @returns new object or null if something fails
   */
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

  /**
   * @returns a method of @param claz that has an annotation of @param noted which matches the optional argument @param matches. The compiler chooses which if there are more than one.
   */
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

  /**
   * @returns name of @param child within object @param parent, null if not an accessible child
   */
  public static <T> Field[] findParentage(Object root, T child) {
    if (child == root) {
      return new Field[0];
    }
    Class childClass = child.getClass();
    FieldWalker fw = new FieldWalker(root.getClass());
    while (fw.hasNext(childClass)) {
      Field maybe = fw.next();

      try {
        Object obj = fw.getObject(root);
        if (obj == child) {
          return fw.genealogy();
        }
      } catch (IllegalAccessException ignored) {
      }
    }
    return null;
  }

  /**
   * iterate over the member classes of the given class, which is NOT the classes of the members.
   * <p>
   * I don't think this is useful.
   */
  public static class ClassWalker implements Iterator<Class> {
    final Class node;//the starting point, retained for debug
    //    final boolean local;
    protected Class[] parts;//
    protected int ci; //class index

    //todo:1 double link list for efficiency, or otherwise maintain pointer to most deeply nested instance. Present implementation chases the list with every iteration step.
    protected ClassWalker nested;

    public ClassWalker(Class node) {
      this.node = node;
//      this.local = local;
      parts = node.getDeclaredClasses();
      ci = parts.length;
      nest();
    }

    public void nest() {
      nested = ci > 0 ? new ClassWalker(parts[--ci]) : null;//recurses
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

  /**
   * marker for a method that can parse an enum. Used by parseEnum for when the default enum parser faults.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD})
  public @interface Parser {

  }

  /**
   * infinite descent if class has an outer class that has other inner classes.
   * walk all the Scalar fields of a class hierarchy.
   * This looks through definitions, an extended class's fields will NOT be seen.
   * To get all actual fields requires objects.
   * Scalar fields are members that are single valued as determined by @see ReflectX.isScalar()
   */
  public static class FieldWalker implements Iterator<Field> {
    final Class ref;//the starting point, retained for debug
    protected Field[] fields;
    protected int fi;
    protected FieldWalker nested = null;
    protected boolean aggressive = true;
    protected Field lookAhead = null;

    /**
     * @param ref is root class for the walk
     */
    public FieldWalker(Class ref) {
      this.ref = ref;
      fields = ref.getDeclaredFields();
      fi = fields.length;
    }

    public boolean hasNext(Class childClass) {
      if (nested != null) {
        if (nested.hasNext(childClass)) {
          return true;
        } else {
          nested = null;
        }
      }
      while (lookAhead == null && fi-- > 0) {//while we haven't found one and there are still some to look at
        lookAhead = fields[fi];
        if (isImplementorOf(childClass, lookAhead.getType())) {
          return true;
        }
        if (!isScalar(lookAhead) && isNormal(lookAhead)) {//then we have a struct
          nested = new FieldWalker(lookAhead.getType());
          if (nested.hasNext(childClass)) {//almost always true.
            return true;
          }
          nested = null;
        }
        lookAhead = null;
      }
      return false;
    }

    public boolean hasNext() {
      if (nested != null) {
        if (nested.hasNext()) {
          return true;
        } else {
          nested = null;
        }
      }
      while (lookAhead == null && fi-- > 0) {//while we haven't found one and there are still some to look at
        lookAhead = fields[fi];
        if (isScalar(lookAhead)) {
          return true;
        }
        if (isNormal(lookAhead)) {
          nested = new FieldWalker(lookAhead.getType());
          if (nested.hasNext()) {//almost always true.
            return true;
          }
          nested = null;
        }
        lookAhead = null;
      }
      return false;
    }

    /**
     * @returns either a field if there is still one or null. Will return a null 'prematurely' if hasNext is not called as it should be, you can't just iterate until you get a null.
     */
    public Field next() {
      if (nested != null) {
        return nested.next();
      }
      try {
        if (aggressive) {
          lookAhead.setAccessible(true);
        }
        return lookAhead;//should not have been called.
      } finally {
        lookAhead = null;
      }
    }

    /**
     * @returns whether the field is something that a human would see as normal
     */
    public boolean isNormal(Field f) {
      if (f.isSynthetic()) {
        return false;
      }
      final Class<?> type = f.getType();
      if (type.isArray()) {
        return false;
      }
      if (type.isAnnotation()) {
        return false;
      }
      if (type.isAnonymousClass()) {
        return false;
      }
      if (isImplementorOf(type, Iterable.class)) {
        return false;
      }
      return true;
    }

    /**
     * call this after calling next but before calling hasNext to get the parentage of what next() returned, that item will be at the end of the returned array.
     */
    public Field[] genealogy() {
      int level = depth();
      Field[] parentage = new Field[level];
      FieldWalker fw = this;
      while (level-- > 0) {
        parentage[level] = fw.fields[fw.fi];
        fw = fw.nested;
      }
      return parentage;
    }

    public int depth() {
      int count = 1;//count ourself
      for (FieldWalker fw = this.nested; fw != null; fw = fw.nested) {
        ++count;
      }
      return count;
    }

    public Object getObject(Object root) throws IllegalAccessException {
      root = fields[fi].get(root);
      if (nested != null) {
        return nested.getObject(root);
      }
      return root;
    }
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
