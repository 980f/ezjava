package pers.hal42.transport;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.hal42.lang.ReflectX;
import pers.hal42.lang.StringX;
import pers.hal42.text.TextList;

import java.lang.annotation.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import static pers.hal42.lang.StringX.NonTrivial;
import static pers.hal42.transport.JsonStorable.dbg;
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
  public final String name;

  /**
   * sometimes a wad is just an array (all children are nameless)
   */
  protected int index = -1;
  @Nullable
  Storable parent; //can be null.
  Type type = Unclassified;
  Origin origin = Ether;
  String image = "";
  double value;
  boolean bit;
  /**
   * linear storage since we rarely have more than 5 children, hashmap ain't worth the overhead.
   * for really big nodes create a nodeIndexer outside of this class and access members herein by index.
   */
  Vector<Storable> wad = new Vector<>();

  public enum Origin {
    Ether,
    Defawlted,
    Parsed,
    Assigned
  }

  public enum Type {
    Unclassified,
    Null,
    Boolean,
    Numeric,
    Textual,
    Wad
  }

  /**
   * marker annotation for use by applyTo and apply()
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.FIELD, ElementType.TYPE})
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
    String fcqn() default "";
  }

  /** used to create objects from Storable's */
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
          dbg.Caught(e1);
        }
      }
    }

    /** can't try/catch inside a delegating constructor :( */
    private Generator(Field field) {
      this(ReflectX.classForName(field.getAnnotation(Generatable.class).fcqn()));
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

    /** to create normal fields (not collection-like) */
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

  /**
   * @return which member of a wad this is
   */
  public int ordinal() {
    return index;
  }

  public static class Rules {
    /** find all the fields even if they aren't public */
    public static final Rules Deep = new Rules(false, true, false);
    /** for most cases this is adequate and fast, usual case means all fields are public and reated by constructors (or native) */
    public static final Rules Fast = new Rules(true, false, false);
    /** the Storable is the prime source of initialization, rather than a tweaker */
    public static final Rules Master = new Rules(false, true, true);
    public final boolean narrow;
    public final boolean aggressive;
    public final boolean create;

    public Rules(boolean narrow, boolean aggressive, boolean create) {
      this.narrow = narrow;
      this.aggressive = aggressive;
      this.create = create;
    }
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
    value = truly ? 1.0 : 0.0;
    image = java.lang.Boolean.toString(bit);
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
      value = Double.NaN;
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
    case Numeric:
      try {
        value = Double.parseDouble(image);//leave this as the overly picky parser
      } catch (Throwable any) {
        value = Double.NaN;
      }
      break;
    case Textual:
      break;
    case Wad:
      break;
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
      setType(Numeric);
    }
    return value;
  }

  public void setValue(double dee) {
    if (type == Type.Unclassified) {
      setType(Numeric);
    }
    value = dee;
    bit = !Double.isNaN(value);
//    image= unchanged, can't afford to render to text on every change
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
      setType(Type.Numeric);
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
      if (newtype == Textual) {
        if (type == Numeric) {
          image = Double.toString(value);
        } else if (type == Boolean) {
          image = java.lang.Boolean.toString(bit);
        }
      } else if (newtype == Numeric) {
        value = StringX.parseDouble(image);
      } else if (newtype == Boolean) {
        parseBool(image);//#returns whether the field appeared to be boolean.
      }
      //more such mappings might make sense
      type = newtype;
    }
    return wasUnknown;
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
        value = Double.parseDouble(image);//leave this as the overly picky parser
        setType(Numeric);
      } catch (Throwable any) {
        if (parseBool(image)) {
          setType(Boolean);
        } else {
          setType(Textual);
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
    names.simpleParse(pathname, '/', false);
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
   * I think this was left over from a parser that didn't handle trailing commas.
   */
  public boolean isTrivial() {
    return false;
//    return origin== Ether && type==Unclassified;
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

  /**
   * set the values of fields marked 'Stored' (and set the local types).
   * if @param narrow then only fields that are direct members of the object are candidates, else base classes are included. (untested as false)
   * if @param aggressive then private fields are modified. (untested)
   *
   * @returns the number of assignments made (a diagnostic)
   */
  public int applyTo(Object obj, Rules r) {
    if (obj == null) {
      return -1;
    }
    int changes = 0;

    Class claz = obj.getClass();
    final Field[] fields = r.narrow ? claz.getDeclaredFields() : claz.getFields();
    for (Field field : fields) {
      Stored stored = field.getAnnotation(Stored.class);
      if (stored != null) {
        String name = field.getName();
        Storable child = existingChild(name);
        if (child == null) {
          String altname = stored.legacy();
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
              field.set(obj, child.getImage());//this is our only exception to recursing on non-natives.
            } else if (fclaz == boolean.class) {
              field.setBoolean(obj, child.getTruth());
            } else if (fclaz == double.class) {
              field.setDouble(obj, child.getValue());
            } else if (fclaz == int.class) {
              field.setInt(obj, (int) child.getValue());
            }
            //todo:1 add clauses for the remaining field.setXXX methods.
            else {
              //time for recursive descent
              Object nestedObject = field.get(obj);
              if (nestedObject == null) {
                dbg.WARNING("No object for field {0}", name);//wtf?- ah, null member
                //autocreate members if no args constructor exists, which our typical usage pattern makes common.
                if (r.create) {
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
              if (nestedObject != null) {
                child.setType(Type.Wad);//should already be true
                if (nestedObject instanceof Map) {
                  return changes;
                } else if (nestedObject instanceof Collection) {
                  Collection objs = (Collection) nestedObject;
                  changes += applyTo(objs, r);
                } else { //simple treewalk
                  int subchanges = child.applyTo(nestedObject, r);
                  if (subchanges >= 0) {
                    changes += subchanges;
                  } else {
                    dbg.ERROR(MessageFormat.format("Not yet setting fields of type {0}", fclaz.getCanonicalName()));
                  }
                }
              }
              continue;//in order to not increment 'changes'
            }
            ++changes;
          } catch (IllegalAccessException e) {
            dbg.Caught(e, MessageFormat.format("applying Storable to {0}", claz.getCanonicalName()));
          }
        }
      }
    }
    return changes;
  }

  public int apply(Object obj, boolean narrow, boolean aggressive, boolean create) {
    if (obj == null) {
      return -1;
    }
    int changes = 0;

    Class claz = obj.getClass();
    final Field[] fields = narrow ? claz.getDeclaredFields() : claz.getFields();
    for (Field field : fields) {
      Stored stored = field.getAnnotation(Stored.class);
      if (stored != null) {
        String name = field.getName();
        Storable child = create ? this.child(name) : this.existingChild(name);
        //note: do not update legacy fields, let them die a natural death.
        if (child != null) {
          try {
            Class fclaz = field.getType();//parent class: getDeclaringClass();
            if (aggressive) {
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
            }
            //todo: add clauses for the remaining field.getXXX methods.
            else {
              //time for recursive descent
              final Object nestedObject = field.get(obj);
              if (nestedObject == null) {
                dbg.WARNING("No object for field {0}", name);
              } else {
                int subchanges = child.apply(nestedObject, narrow, aggressive, create);
                if (subchanges >= 0) {
                  changes += subchanges;
                } else {
                  dbg.ERROR(MessageFormat.format("Not yet recording fields of type {0}", fclaz.getCanonicalName()));
                }
              }
              continue;//in order to not increment 'changes'
            }
            ++changes;
          } catch (IllegalAccessException e) {
            dbg.Caught(e, MessageFormat.format("applying {0} to Storable", claz.getCanonicalName()));
          }
        }
      }
    }
    return changes;
  }
}
