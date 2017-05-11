package pers.hal42.transport;

import org.jetbrains.annotations.NotNull;
import pers.hal42.lang.StringX;
import pers.hal42.text.TextList;

import java.lang.annotation.*;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.Vector;

import static pers.hal42.transport.JsonStorable.dbg;
import static pers.hal42.transport.Storable.Origin.Defawlted;
import static pers.hal42.transport.Storable.Origin.Ether;
import static pers.hal42.transport.Storable.Type.Boolean;
import static pers.hal42.transport.Storable.Type.*;

/**
 * Created by Andy on 5/8/2017.
 */
public class Storable {
  public final String name;

  /**
   * sometimes a wad is just an array (all children are nameless
   */
  protected int index = -1;
  Storable parent; //can be null.
  Type type = Unclassified;

  public enum Origin {
    Ether,
    Defawlted,
    Parsed,
    Assigned
  }

  Origin origin = Ether;
  String image = "";
  double value;
  boolean bit;

  /**
   * linear storage since we rarely have more than 5 children, hashmap ain't worth the overhead.
   * for really big nodes create a nodeIndexer outside of this class and access members herein by index.
   */
  Vector<Storable> wad = new Vector<>();

  /**
   * only use this for root nodes.
   */
  public Storable(String name) {
    this.name = name;
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
    return StringX.OnTrivial(image, "");
  }

  /**
   * identify type as textual and return it
   */
  public String getImage() {
    if (type == Type.Unclassified) {
      type = Textual;
      image = StringX.OnTrivial(image, "");
    }
    return image;
  }

  public void setValue(boolean truly) {
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
      type = Boolean;
    }
    return bit;
  }

  public double getValue() {
    if (type == Type.Unclassified) {
      type = Numeric;
    }
    return value;
  }

  public void setValue(double dee) {
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
      if (newtype == Textual && type == Numeric) {
        image = Double.toString(value);
      } else if (newtype == Numeric && type == Textual) {
        value = Double.parseDouble(image);//todo: use safer parser
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
        return type = Type.Numeric;
      } catch (Throwable any) {
        //ignore
      }
      parseBool(image);
    }
    return type;
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
  @Target({ElementType.FIELD})
  public @interface Stored {
    //marker, field's own type is explored for information needed to write to it.
  }

  /**
   * set the values of fields marked 'Stored' (and set the local types).
   * if @param narrow then only fields that are direct members of the object are candidates, else base classes are included. (untested as false)
   * if @param aggressive then private fields are modified. (untested)
   *
   * @returns the number of assignments made (a diagnostic)
   */
  public int applyTo(Object obj, boolean narrow, boolean aggressive) {
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
        Storable child = this.existingChild(name);
        ;
        if (child != null) {
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
            //todo: add clauses for the remaining field.setXXX methods.

            else {
              //time for recursive descent
              int subchanges = child.applyTo(field.get(obj),narrow, aggressive);
              if (subchanges >= 0) {
                changes += subchanges;
              } else {
                dbg.ERROR(MessageFormat.format("Not yet setting fields of type {0}", fclaz.getCanonicalName()));
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

  public int apply(Object obj, boolean narrow, boolean aggressive) {
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
        Storable child = this.existingChild(name);
        ;
        if (child != null) {
          try {
            Class fclaz = field.getType();//parent class: getDeclaringClass();
            if (aggressive) {
              field.setAccessible(true);
            }
            if (fclaz == String.class) {
              child.setValue( field.get(obj).toString());//this is our only exception to recursing on non-natives.
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
              int subchanges = child.apply(field.get(obj),narrow, aggressive);
              if (subchanges >= 0) {
                changes += subchanges;
              } else {
                dbg.ERROR(MessageFormat.format("Not yet recording fields of type {0}", fclaz.getCanonicalName()));
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
