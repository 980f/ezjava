package pers.hal42.transport;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.hal42.lang.Finally;
import pers.hal42.lang.LinearMap;
import pers.hal42.lang.ReflectX;
import pers.hal42.lang.StringX;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.text.TextList;

import java.lang.annotation.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.*;

import static pers.hal42.lang.Index.BadIndex;
import static pers.hal42.lang.StringX.NonTrivial;
import static pers.hal42.transport.Storable.Origin.Defawlted;
import static pers.hal42.transport.Storable.Origin.Ether;
import static pers.hal42.transport.Storable.Type.Boolean;
import static pers.hal42.transport.Storable.Type.*;

/**
 * Created by Andy on 5/8/2017.
 * <p>
 * This can be viewed as a DOM good for JSON data. It is intended to replace usage of java properties, despite us having nice classes to make properties look like a tree.
 */
public class Storable {
  public static final ErrorLogStream dbg = ErrorLogStream.getForClass(Storable.class);

  public enum Type {
    Unclassified,
    Null,
    Boolean,
    Enummy,
    WholeNumber,
    Floating, //as in mathematics
    Textual,
    Wad
  }

  public enum Origin {
    Ether,
    Defawlted,
    Parsed,
    Assigned
  }

  /**
   * marker annotation for use by applyTo and apply()
   * if class is annotated with @Stored then process all fields, not just those annotated with Stored.
   */
  @Documented
  @Inherited
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.FIELD, ElementType.TYPE, ElementType.METHOD})
  public @interface Stored {
    /** if nontrivial AND child is not found by the field name then it is sought by this name. */
    String legacy() default "";
  }

  /**
   * marker annotation for use by applyTo and apply()
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.FIELD, ElementType.TYPE})
  public @interface Generatable {
    /** if nontrivial then it is the classname for creating objects from a node. */
    String fqcn() default "";
  }

  private final char splitchar = '/';

  /**
   * used to create objects from Storable's
   */
  @SuppressWarnings("unchecked")
  private static class Generator {
    final Class glass;// = ReflectX.classForName(gen.fcqn());
    Constructor storied = null;//
    Constructor nullary = null;  // ctor= glass.getConstructor()
    boolean viable = false;

    public Generator(Class glass) {
      this.glass = glass;
      try {
        storied = glass.getConstructor(Storable.class);
        viable = true;
      } catch (NoSuchMethodException e) {
        try {
          nullary = glass.getConstructor();
          viable = true;
        } catch (NoSuchMethodException e1) {
          dbg.ERROR("Constructing Storable.Generator for {0} ignoring {1}", glass.getName(), e1);
        }
      }
    }

    /**
     * can't try/catch inside a delegating constructor :(
     */
    private Generator(Field field) {
      this(ReflectX.classForName(field.getAnnotation(Generatable.class).fqcn()));
    }

    Object newInstance(Storable child, Rules r) {
      if (!viable) {
        return null;
      }
      try {
        if (storied != null) {
          return storied.newInstance(child);
        } else {
          Object noob = nullary.newInstance();
          child.applyTo(noob, r);
          return noob;
        }
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
        dbg.Caught(e);
        viable = false;
        return null;
      }
    }

    /**
     * to create normal fields (not collection-like)
     */
    static Generator forField(Field field) {
      try {
        return new Generator(field);
      } catch (NullPointerException e) {
        //either field isn't annotated, or the annotation is not a known class name.
        dbg.Caught(e);
        return null;
      }
    }
  }

  public static class Rules {
    public final boolean narrow;
    public final boolean aggressive;
    public final boolean create;
    /**
     * find all the fields even if they aren't public
     */
    public static final Rules Deep = new Rules(false, true, false);
    /**
     * for most cases this is adequate and fast, usual case means all fields are public and created by constructors (or native)
     */
    public static final Rules Fast = new Rules(true, false, false);
    /**
     * the Storable is the prime source of initialization, rather than a tweaker
     */
    public static final Rules Master = new Rules(false, true, true);

    public Rules(boolean narrow, boolean aggressive, boolean create) {
      this.narrow = narrow;
      this.aggressive = aggressive;
      this.create = create;
    }
  }

  /**
   * bind a node to an object
   */
  public static class Nodal<T> {
    public Storable node;
    public T item;
    public Rules r;

    /** constructing one of these fills the obj with values from the node. */
    public Nodal(Storable node, T obj) {
      this.node = node;
      this.item = obj;
      r = Rules.Master;
      node.applyTo(item, r);
    }

    public void update() {
      node.apply(item, r);
    }
  }

  /**
   * immutable key to the value, never abuse for a secondary value
   */
  public final String name;
  /**
   * if not null then value and image are related by this class's enum[]
   */
  public Class<? extends Enum> enumerizer;
  /**
   * sometimes a wad is just an array (all children are nameless)
   */
  protected int index = -1;
  @Nullable Storable parent; //can be null.
  protected Type type = Unclassified;
  protected Origin origin = Ether;
  //4 value types:
  protected String image = "";
  protected int ivalue;
  protected double dvalue;
  protected boolean bit;
  /**
   * linear storage since we rarely have more than 5 children, hashmap ain't worth the overhead.
   * for really big nodes create a nodeIndexer outside of this class and access members herein by index.
   */
  protected Vector<Storable> wad = new Vector<>();

  /** part of flattenedImage */
  protected void addToMap(Map<String, String> map, TreeName cursor) {
    try (Finally pop = cursor.push(name)) {
      for (Storable kid : wad) {
        if (Wad == kid.type) {
          kid.addToMap(map, cursor);
        } else {
          map.put(cursor.leaf(kid.name), kid.getImage());
        }
      }
    }
  }

  /** @returns new Map of images of child nodes, if @param deep then recurses into children that are wads */
  public LinearMap<String, String> flattenedImage(String slash) {
    LinearMap<String, String> map = new LinearMap<>(wad.size());
    boolean deep = slash != null;
    final TreeName cursor = deep ? new TreeName(slash) : null;
    for (Storable kid : wad) {
      if (kid.type != Wad) {
        map.put(kid.name, kid.getImage());
      } else if (deep) {
        kid.addToMap(map, cursor);
      }
    }
    return map;
  }

  /**
   * only use this for root nodes.
   */
  public Storable(String name) {
    if (name == null) {
      dbg.VERBOSE("null name");
    }
    this.name = StringX.TrivialDefault(name, "");//normalize all trivialities, we don't want one space to be different name than two spaces ...
    this.parent = null;//in yur face.
  }

  /** do NOT genericize this class just for this guy, who probably shouldn't be public. */
  public Object getEnum() {
    if (enumerizer != null) {
      try {
        return enumerizer.getEnumConstants()[ivalue];
      } catch (Exception any) {
        return null;
      }
    } else {
      return null;
    }
  }

  /**
   * set the values of fields marked 'Stored' (and set the local types).
   * if @param narrow then only fields that are direct members of the object are candidates, else base classes are included. (untested as false)
   * if @param aggressive then private fields are modified. (untested)
   * <p>
   * Note: java makes you choose between all fields at one level of the hierarchy or only public fields throughout the hierarchy. You can iterate through base classes to get all fields.
   *
   * @returns the number of assignments made (a diagnostic)
   */
  public int applyTo(Object obj, Rules r) {
    if (obj == null) {
      return -1;
    }
    int changes = 0;

    Class claz = obj.getClass();
    Stored all = ReflectX.getAnnotation(claz, Stored.class);
    final Field[] fields = r.narrow ? claz.getDeclaredFields() : claz.getFields();
    for (Field field : fields) {
      Stored stored = field.getAnnotation(Stored.class);
      if (stored != null || (all != null && !usuallySkip(field))) {
        String name = field.getName();
        Storable child = existingChild(name);
        if (child == null) {
          String altname = stored != null ? stored.legacy() : all.legacy();
          if (NonTrivial(altname)) {//optional search for prior equivalent
            if (altname.endsWith("..")) {//a minor convenience, drop if buggy.
              altname += name;
            }
            child = findChild(altname, false);
          }
        }
        if (child != null) {//then we have init data for the field
          try {
            Class fclaz = field.getType();//parent class: getDeclaringClass();
            if (r.aggressive) {
              field.setAccessible(true);
            }
            if (fclaz == String.class) {
              field.set(obj, child.getImage());//this is our main exception to recursing on non-natives.
            } else if (fclaz == boolean.class) {
              field.setBoolean(obj, child.getTruth());
            } else if (fclaz == double.class) {
              field.setDouble(obj, child.getValue());
            } else if (fclaz == int.class) {
              field.setInt(obj, (int) child.getValue());
            } else if (fclaz.isEnum()) {
              //noinspection unchecked
              child.setEnumerizer(fclaz);
              final Object childEnum = child.getEnum();
              if (childEnum != null) { //we do not override caller unless we are sure we have a proper object.
                field.set(obj, childEnum);
              }
            } else if (fclaz == Storable.class) {//member is a reference to a child herein
              field.set(obj, child);
            }
            //todo:1 add clauses for the remaining field.setXXX methods.
            else {
              //time for recursive descent
              Object nestedObject = field.get(obj);
              if (nestedObject == null) {
                dbg.WARNING("No object for field {0}", name);//wtf?- ah, null member
                //autocreate members if no args constructor exists, which our typical usage pattern makes common.
                if (r.create) {
                  if (fclaz == java.util.Properties.class) { //map stored into properties
                    nestedObject = new java.util.Properties();
                  } else {
                    Generator generator = new Generator(fclaz);
                    nestedObject = generator.newInstance(this, r);
                    //if object is compound ..
                    if (nestedObject != null && nestedObject instanceof Collection) {
                      Collection objs = (Collection) nestedObject;
                      applyTo(objs, r);
                      return changes;
                    }
                  }
                }
              }
              if (nestedObject != null) {
                child.setType(Type.Wad);//should already be true
                if (nestedObject instanceof Properties) { //must precede map and collections since it is one.
                  changes += child.applyTo((Properties) nestedObject, false);//todo:1 review choice of stringify, or find a means to configure it.
                } else if (nestedObject instanceof Map) {
                  return changes; //NYI!
                } else if (nestedObject instanceof Collection) {
                  Collection objs = (Collection) nestedObject;
                  changes += applyTo(objs, r);
                }
                //
                else { //simple treewalk
                  int subchanges = child.applyTo(nestedObject, r);
                  if (subchanges >= 0) {
                    changes += subchanges;
                  } else {
                    dbg.ERROR("Not yet setting fields of type {0}", fclaz.getCanonicalName());
                  }
                }
              }
              continue;//in order to not increment 'changes'
            }
            ++changes;
          } catch (IllegalAccessException e) {
            dbg.Caught(e, "applying Storable to {0}", claz.getCanonicalName());
          }
        }
      }
    }
    final Method method = ReflectX.methodFor(claz, Stored.class, stored -> "postload".equals(stored.legacy()));
    if (method != null) {
      try {
        method.invoke(obj);
      } catch (IllegalAccessException | InvocationTargetException e) {
        dbg.Caught(e);
      }
    }
    return changes;
  }

  public int applyTo(PropertyCursor cursor) {
    int count = 0;
    for (Storable child : wad) {
      switch (child.type) {
      case Wad:
        try (Finally pop = cursor.push(child.name)) {
          count += child.applyTo(cursor);
        }
        break;
      case Null: //don't output actual nulls, underlying hashtable will NPE.
        //todo:1 add enum to PropertyCursor to decide how to express nulls.
        break;
      case Boolean:
        cursor.putProperty(child.name, child.getTruth());
        ++count;
        break;
      case Enummy:
        cursor.putProperty(child.name, cursor.enumsAsSymbol ? child.getImage() : child.getIntValue());
        ++count;
        break;
      case WholeNumber:
        cursor.putProperty(child.name, child.getIntValue());
        ++count;
        break;
      case Floating:
        cursor.putProperty(child.name, child.getValue());
        ++count;
        break;
      case Unclassified:
      case Textual:
        cursor.putProperty(child.name, child.getImage());
        ++count;
        break;
      }
    }
    return count;
  }

  public int applyTo(Properties properties, boolean stringify) {
    PropertyCursor cursor = new PropertyCursor(properties, stringify);
    return applyTo(cursor);
  }

  /**
   * add children as properties parsing 'dot' seperated paths
   *
   * @deprecated untested
   */
  public void merge(Properties properties) {
    properties.forEach((k, v) -> {
      String name = StringX.replace(String.valueOf(k), ".", String.valueOf(splitchar));
      Storable child = findChild(name, true);
      child.setValue(String.valueOf(v));
    });
  }

  /**
   * @return which member of a wad this is
   */

  public int ordinal() {
    return index;
  }

  /**
   * set value from string. This will parse the string if the type is not textual
   */
  public void setValue(String text) {
    if (origin == Origin.Ether) {
      origin = Origin.Parsed;
    }
    if (text == null) {
      type = Null;
      image = "";
      bit = false;
      ivalue = BadIndex;
      dvalue = Double.NaN;
      return;
    }
    image = text.trim();
    switch (type) {
    case Unclassified:
      break;
    case Null:
      setType(Textual);
      break;
    case Boolean:
      parseBool(image);
      //else silently ignore garbage.
      break;
    case Floating:
      try {
        dvalue = Double.parseDouble(image);//leave this as the overly picky parser
      } catch (Throwable any) {
        dvalue = Double.NaN;
      }
      break;
    case WholeNumber:
      try {
        ivalue = Integer.parseInt(image);//leave this as the overly picky parser
      } catch (Throwable any) {
        ivalue = BadIndex;
      }
      break;
    case Enummy:
      enumOnSetImage();
    case Textual:
      break;
    case Wad:
      break;
    }
  }

  /**
   * non mutating and non null apparent image of value, for under-the-hood peeks
   */
  public String asText() {
    return StringX.unNull(image);
  }

  /**
   * identify type as textual and return it
   */
  public String getImage() {
    if (type == Type.Unclassified) {
      setType(Textual);
      image = StringX.unNull(image);
    }
    return image;
  }

  public void setValue(boolean truly) {
    setType(Boolean);
    bit = truly;
    ivalue = truly ? 1 : 0;//coa
    dvalue = ivalue;       //coa
    image = java.lang.Boolean.toString(bit);
  }

  public void setValue(Object obj) {
    if (obj instanceof String) {
      setValue((String) obj);
    } else if (obj instanceof Integer) {
      setValue(((Integer) obj).intValue());
    } else if (obj instanceof Double) {
      setValue(((Double) obj).doubleValue());
    } else if (obj instanceof Boolean) {
      setValue(((Boolean) obj).booleanValue());
    } else if (obj instanceof Map) {
      setValue((Map) obj);
    } else {
      dbg.FATAL("implement setValue {0}", obj.getClass().getName());
    }
  }

  protected boolean parseBool(String image) {
    if (image.length() == 1) {
      char single = image.charAt(0);
      switch (single) {
      case 't':
      case 'T':
      case '1':
        bit = true;
        return true;
      case 'f':
      case 'F':
      case '0':
        bit = false;
        return true;
      default:
        return false;
      }
    } else if (image.equalsIgnoreCase("true")) {
      bit = true;
      return true;
    } else if (image.equalsIgnoreCase("false")) {
      bit = false;
      return true;
    }
    return false;
  }

  public void setDefault(boolean truly) {
    switch (origin) {
    case Ether:
      origin = Defawlted;
      //#join
    case Defawlted:
      setValue(truly);
      //#join
    case Parsed:
      setType(Type.Boolean);
      break;
    case Assigned:
      //do nothing
      break;
    }
  }

  public boolean getTruth() {
    if (type == Type.Unclassified) {
      setType(Boolean);
    }
    return bit;
  }

  public double getValue() {
    if (type == Type.Unclassified) {
      setType(Floating);
    }
    return dvalue;
  }

  public void setValue(double dee) {
    if (type == Type.Unclassified) {
      setType(Floating);
    }
    dvalue = dee;
    ivalue = (int) Math.round(dvalue);
    bit = !Double.isNaN(dvalue);//coa
    if (enumerizer != null) {
      enumOnSetNumber();
    }
//    image= unchanged, can't afford to render to text on every change
  }

  public int getIntValue() {
    if (type == Type.Unclassified) {
      setType(WholeNumber);
    }
    return ivalue;
  }

  public void setValue(int whole) {
    ivalue = whole;
    if (type == Type.Unclassified) {
      setType(WholeNumber);
    }

    bit = ivalue != 0;//coa
    if (enumerizer != null) {
      enumOnSetNumber();
    }
//    image= unchanged, can't afford to render to text on every change
  }


  /** this will only work well when the Map's keys have a nice toString, and the values are object wrappers for the known types herein. */
  @SuppressWarnings("unchecked")
  public void setValue(Map mobject) {
    setType(Type.Wad);
    mobject.forEach((k, v) -> {
      child(String.valueOf(k)).setValue(v);
    });
  }

  public void setEnumerizer(Class<? extends Enum> enumer) {
    if (enumer != null && enumer != enumerizer) {//#object identity compare intended.
      this.enumerizer = enumer;
      if (type == Floating || type == WholeNumber) {
        enumOnSetNumber();
      } else {
        //what basis do we have for validating enum?
        enumOnSetImage();//set number
      }
      setType(Enummy);
    }
  }

  public void setDefault(double number) {
    switch (origin) {
    case Ether:
      origin = Defawlted;
      //#join
    case Defawlted:
      setValue(number);
      //#join
    case Parsed:
      setType(Type.Floating);
      break;
    case Assigned:
      //do nothing
      break;
    }
  }

  public void setDefault(int number) {
    switch (origin) {
    case Ether:
      origin = Defawlted;
      //#join
    case Defawlted:
      setValue(number);
      //#join
    case Parsed:
      setType(Type.WholeNumber);
      break;
    case Assigned:
      //do nothing
      break;
    }
  }

  public void setDefault(String string) {
    switch (origin) {
    case Ether:
      origin = Defawlted;
      //#join
    case Defawlted:
      setValue(string);
      //#join
    case Parsed:
      setType(Type.Textual);
      break;
    case Assigned:
      //do nothing
      break;
    }
  }

  /**
   * we parse as strings of unknown type, then as we fish for values we setType to the expected
   */
  public boolean setType(Type newtype) {
    boolean wasUnknown = type == Type.Unclassified;
    if (type != newtype) {
      switch (newtype) {
      case Textual://target
        switch (type) {
        case Floating:
          image = Double.toString(dvalue);
          break;
        case WholeNumber:
          image = Integer.toString(ivalue);
          break;
        case Boolean:
          image = java.lang.Boolean.toString(bit);
          break;
        }
        break;
      case Floating://target
        switch (type) {
        default:
          dvalue = StringX.parseDouble(image);
          break;
        case WholeNumber:
          dvalue = ivalue;
          break;
        case Boolean:
          dvalue = bit ? 1.0 : 0.0;
          break;
        }
        break;
      case WholeNumber: //target
        switch (type) {
        default:
          ivalue = StringX.parseInt(image);
          break;
        case Floating:
          ivalue = (int) Math.round(dvalue);
          break;
        case Boolean:
          ivalue = bit ? 1 : 0;
          break;
        }
        break;
      case Boolean: //target
        switch (type) {
        default:
          parseBool(image);//#returns whether the field appeared to be boolean.
          break;
        case Floating:
          bit = !Double.isNaN(dvalue);
          break;
        case WholeNumber:
          bit = ivalue != 0;
          break;
        }
        break;
      }
      type = newtype;
    }
    return wasUnknown;
  }

  private void enumOnSetImage() {
    try {
      //noinspection unchecked
      ivalue = Enum.valueOf(enumerizer, image).ordinal();
    } catch (NullPointerException | IllegalArgumentException e) {
      dbg.Caught(e); //not our job to enforce validity
    }
  }

  private void enumOnSetNumber() {
    try {
      image = enumerizer.getEnumConstants()[ivalue].toString();
    } catch (Exception e) {
      dbg.Caught(e);
      image = "#invalid";
    }
  }

  public int apply(Object obj, Rules r) {
    if (obj == null) {
      return -1;
    }
    int changes = 0;

    Class claz = obj.getClass();
    Stored all = (Stored) claz.getAnnotation(Stored.class);
    final Field[] fields = r.narrow ? claz.getDeclaredFields() : claz.getFields();
    for (Field field : fields) {
      Stored stored = field.getAnnotation(Stored.class);
      if (stored != null || (all != null && !usuallySkip(field))) {
        String name = field.getName();
        Storable child = r.create ? this.child(name) : this.existingChild(name);
        //note: do not update legacy fields, let them die a natural death.
        if (child != null) {
          try {
            Class fclaz = field.getType();//parent class: getDeclaringClass();
            if (r.aggressive) {
              field.setAccessible(true);
            }
            if (fclaz == String.class) {
              child.setValue(field.get(obj).toString());//this is our only exception to recursing on non-natives.
            } else if (fclaz == boolean.class) {
              child.setValue(field.getBoolean(obj));
            } else if (fclaz == double.class) {
              child.setValue(field.getDouble(obj));
            } else if (fclaz == int.class) {
              child.setValue(field.getInt(obj));
            } else if (fclaz.isEnum()) {
              child.setValue(field.get(obj).toString());
            }
            //todo:1 add clauses for the remaining field.getXXX methods.
            else {
              //time for recursive descent
              final Object nestedObject = field.get(obj);
              if (nestedObject == null) {
                dbg.WARNING("No object for field {0} of type {1} ", name, fclaz.getName());
              } else if (ReflectX.isImplementorOf(fclaz, Map.class)) {
                //then set children to map entries
                child.setValue((Map) nestedObject);
              } else {
                int subchanges = child.apply(nestedObject, r);
                if (subchanges >= 0) {
                  changes += subchanges;
                } else {
                  dbg.ERROR("Not yet recording fields of type {0}", fclaz.getCanonicalName());
                }
              }
              continue;//in order to not increment 'changes'
            }
            ++changes;
          } catch (IllegalAccessException e) {
            dbg.Caught(e, "applying {0} to Storable", claz.getCanonicalName());
          }
        }
      }
    }
    return changes;
  }


  /**
   * find or create child
   */
  public Storable child(String childname) {
    Storable kid = existingChild(childname);
    if (kid == null) {
      kid = makeChild(childname);
    }
    return kid;
  }

  /** make a new child, does NOT check if the name is alredy in use. */
  @NotNull
  public Storable makeChild(String childname) {
    Storable kid = new Storable(childname);
    kid.index = wad.size();
    kid.parent = this;
    wad.add(kid);
    this.type = Type.Wad;//any scalar value it had is moot once it becomes a parent.
    return kid;
  }

  /**
   * about the only time we kill nodes is when they back an array.
   * to kill a named child you have to get it then killChild(child.ordinal())
   */
  public void killChild(int which) {
    if (which >= 0 && which < wad.size()) {
      for (int shifter = which; ++shifter < wad.size(); ) {
        --wad.elementAt(shifter).index;
      }
    }
    wad.remove(which);
  }

  public Type guessType() {
    if (type == Type.Unclassified) {
      try {
        ivalue = Integer.parseInt(image);//leave this as the overly picky parser
        setType(WholeNumber);
      } catch (Throwable any) {
        ivalue = BadIndex;
        try {

          dvalue = Double.parseDouble(image);//leave this as the overly picky parser

          setType(Floating);
        } catch (Throwable anymore) {
          dvalue = Double.NaN;
          if (parseBool(image)) {
            setType(Boolean);
          } else {
            setType(Textual);
          }
        }
      }
    }
    return type;
  }

  /**
   * find or if desired make a child given a  pathname seperated by slashes. Useful for gui access to member.
   * If path is trivial then even if autocreate is true a null is returned.
   * one way to determine if the child was autocreated is to see if its type is Unclassified.
   */
  public Storable findChild(String pathname, boolean autocreate) {
    pers.hal42.text.TextList names = new TextList();
    names.simpleParse(pathname, splitchar, false);
    int depth = names.size();
    if (depth == 0) {
      return null;
    }
    Storable parent = this;
    for (int level = 0; level < depth; ++level) {
      String childname = names.itemAt(level);
      Storable kid = parent.existingChild(childname);
      if (kid == null) {
        if (autocreate) {
          do {
            parent = parent.makeChild(childname);
          } while (++level < depth);
          return parent;//created
        } else {
          return null;//not found
        }
      }
      parent = kid; //descend
    }
    return parent;//found existing
  }

  /**
   * try to filter out accidentally created nodes.
   */
  public boolean isTrivial() {
    return origin == Ether && type == Unclassified;
  }

  public int numChildren() {
    return wad.size();
  }

  /** try to divine type from appearance, recursively */
  public void guessTypes() {
    if (type == Type.Wad) {
      wad.forEach(Storable::guessType);
    } else {
      guessType();
    }
  }

  /**
   * @returns a child if it exists else null
   */
  public Storable existingChild(String childname) {
    if (childname == null) {
      return null;
    }
    for (Storable kid : wad) {
      if (kid.name.equals(childname)) {
        return kid;
      }
    }
    return null;
  }

  /**
   * this could easily get out of synch:
   * if you create from a wad, then do some insertions, then try to save back to same wad then your instances better not be the kind that individually stay in synch (but any such could also auto insert new children ...  see VectorSet )
   */
  @SuppressWarnings("unchecked")
  public int applyTo(Field field, Collection objs, Rules r) {
    int changes = 0;
    final Iterator oit = objs.iterator();
    final Iterator<Storable> kids = wad.iterator();
    //apply args
    while (oit.hasNext() && kids.hasNext()) {
      changes += kids.next().applyTo(oit.next(), r);
    }
    if (r.create && kids.hasNext()) {
      Generator generator = Generator.forField(field);
      if (generator != null) {
        try {
          while (kids.hasNext()) {
            objs.add(generator.newInstance(kids.next(), r));
          }
        } catch (Exception e) {
          dbg.Caught(e);
        }
      }
    }
    return changes;
  }

  @SuppressWarnings("unchecked")
  public int applyTo(Field field, Map map, Rules r) {
    int changes = 0;
    final Iterator<Storable> kids = wad.iterator();
    Generator generator = r.create ? Generator.forField(field) : null;
    try {
      while (kids.hasNext()) {
        Storable child = kids.next();
        Object obj = map.get(child.name);
        if (obj != null) {
          changes += child.applyTo(obj, r);
        } else if (generator != null) {
          Object noob = generator.newInstance(child, r);
          map.put(child.name, noob);
          ++changes;
        }
      }
    } catch (ClassCastException | NullPointerException any) {
      //map type not supported
    }
    return changes;
  }

  @Override
  public String toString() {
    return MessageFormat.format("{0}:{1}", name, image);
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public static boolean usuallySkip(Field field) {
    return ReflectX.ignorable(field);
  }
}
