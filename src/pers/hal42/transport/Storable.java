package pers.hal42.transport;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.hal42.lang.ReflectX;
import pers.hal42.lang.StringX;
import pers.hal42.text.TextList;

import java.lang.annotation.*;
import java.lang.reflect.Field;
import java.text.MessageFormat;
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
  @Target({ElementType.FIELD,ElementType.TYPE})
  public @interface Stored {
    //marker, field's own type is explored for information needed to write to it.
    /** //if nontrivial AND child is not found by the field name then it is sought by this name. */
    String legacy() default "";
  }

  /**
   * only use this for root nodes.
   */
  public Storable(String name) {
    if(name==null){
      dbg.VERBOSE("null name");
    }
    this.name = StringX.TrivialDefault(name,"");//normalize all trivialities, we don't want one space to be different name than two spaces ...
    this.parent = null;//in yur face.
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

  /**
   * we parse as strings of unknown type, then as we fish for values we setType to the expected
   */
  public boolean setType(Type newtype) {
    boolean wasUnknown = type == Type.Unclassified;
    if (type != newtype) {
      if (newtype == Textual){
        if(type == Numeric) {
          image = Double.toString(value);
        } else if(type==Boolean){
          image= java.lang.Boolean.toString(bit);
        }
      } else if (newtype == Numeric ) {
        value = StringX.parseDouble(image);
      } else if(newtype==Boolean){
        parseBool(image);//#returns whether the field appeared to be boolean.
      }
      //more such mappings might make sense
      type = newtype;
    }
    return wasUnknown;
  }

  public Type guessType() {
    if (type == Type.Unclassified) {
      try {
        value = Double.parseDouble(image);//leave this as the overly picky parser
        setType(Numeric);
      } catch (Throwable any) {
        if(parseBool(image)){
          setType(Boolean);
        } else {
          setType(Textual);
        }
      }
    }
    return type;
  }

  /** try to divine type from appearance, recursively*/
  public void guessTypes(){
    if(type == Type.Wad){
      wad.forEach(Storable::guessType);
    } else {
      guessType();
    }
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

  /**
   * @returns a child if it exists else null
   */
  public Storable existingChild(String childname) {
    if(childname==null){
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

  /**
   * set the values of fields marked 'Stored' (and set the local types).
   * if @param narrow then only fields that are direct members of the object are candidates, else base classes are included. (untested as false)
   * if @param aggressive then private fields are modified. (untested)
   *
   * @returns the number of assignments made (a diagnostic)
   */
  public int applyTo(Object obj, boolean narrow, boolean aggressive, boolean create) {
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
        Storable child = existingChild(name);
        if(child==null){
          String altname=stored.legacy();
          if(NonTrivial(altname)){//optional search for prior equivalent
            if(altname.endsWith("..")){//a minor convenience, drop if buggy.
              altname += name;
            }
            child = findChild(altname,false);
          }
        }
        if (child != null) {//then we have init data for the field
          try {
            Class fclaz = field.getType();//parent class: getDeclaringClass();
            if (aggressive) {
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
              if(nestedObject==null){
                dbg.WARNING("No object for field {0}",name);//wtf?- ah, null member
                //autocreate members if no args constructor exists, which our typical usage pattern makes common.
                if(create){
                  try {
                    nestedObject=fclaz.newInstance();
                  } catch (InstantiationException e) {
                    dbg.WARNING("No no-args constructor for field {0}",name);//todo: more context for message
                  }
                }
              }
              if(nestedObject!=null){
                child.setType(Type.Wad);
                int subchanges = child.applyTo(nestedObject, narrow, aggressive,create);
                if (subchanges >= 0) {
                  changes += subchanges;
                } else {
                  dbg.ERROR(MessageFormat.format("Not yet setting fields of type {0}", fclaz.getCanonicalName()));
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
        Storable child = create? this.child(name):this.existingChild(name);
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
              if(nestedObject==null){
                dbg.WARNING("No object for field {0}",name);
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
