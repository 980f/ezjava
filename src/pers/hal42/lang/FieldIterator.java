package pers.hal42.lang;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.function.Predicate;

/**
 * Created by Andy on 6/8/2017.
 * <p>
 * To return fields of an object that are of a given type.
 * Ignores null members on recursion but not on top level scan (an optimisation you will have to live with)
 *
 * One could craft a similar class that ignores existence and can find fields deeply at the expense of not handing out objects related to the field.
 * <p>
 * It is optimized for shallow scans, but supports deep ones.
 */
public class FieldIterator<Type> implements Iterator<Type> {
  private final Class type;//must keep this ourselves as java generics are compile time.
  private final Object parent;
  private final Field[] fields;
  /** cache field test, for when we rewind frequently */
  private final boolean[] isMember;

  private int pointer;
  private final boolean deeply;
  //tree walk
  private FieldIterator<Type> nested = null;

  public FieldIterator(Class type, Object parent, boolean deeply) {
    this.type = type;
    this.parent = parent;
    this.deeply = deeply;
    fields = parent.getClass().getFields(); //this gets public fields only, //todo:1 option to forcibly access declared fields.
    isMember = new boolean[fields.length];
    for (pointer = fields.length; pointer-- > 0; ) {
      final Field field = fields[pointer];
      isMember[pointer] = belongs(field);
    }
    rewind();
  }

  /** while only used in one place this is hard enough to read to be worth its own method */
  private boolean belongs(Field f) {
    return type == null || ReflectX.isImplementorOf(f.getType(), type);
  }

  @Override
  public boolean hasNext() {
    if (nested != null) {
      if (nested.hasNext()) {
        return true;
      } else {
        nested = null;
      }
    }
    if (pointer >= 0) {//will be -1 if there are no fields and as such isMember has zero length.
      for (; pointer < fields.length; ++pointer) {
        if (isMember[pointer]) {
          return true;
        }
        if (deeply) {
          final Field field = fields[pointer];
          if (!ReflectX.isScalar(field)) {//todo:1 see if isNative also covers class not having a classLoader
            try {
              final Object child = field.get(this.parent);
              if (child != null) {
                nested = new FieldIterator<>(type, child, true);
                if (nested.hasNext()) {
                  ++pointer;//we have consumed the field
                  return true;
                } else {
                  nested = null;
                }
              }
            } catch (IllegalAccessException e) {
              continue;//try next field, this one was inaccessible.
            }
          }
        }
      }
    }
    return false;
  }

  @Override
  public Type next() {
    if (nested != null) {
      return nested.next();
    }
    if (pointer >= 0) {
      try {
        Field f = fields[pointer++];//hasNext has qualified this
        //noinspection unchecked
        return (Type) f.get(parent);
      } catch (Exception e) {//guard against user failing to call hasNext.
        return null;
      }
    } else {
      return null;
    }
  }

  public void rewind() {
    nested = null;
    pointer = 0;
  }

  /**
   * @returns some field that meets @param filter, null if none.
   * @apiNote presently ignores 'deeply' as that is very expensive
   */
  public Type find(Predicate<Type> filter) {
    for (int fi = fields.length; fi-- > 0; ) {
      if (isMember[fi]) {
        try {
          //noinspection unchecked
          Type probate = (Type) fields[fi].get(parent);
          if (filter.test(probate)) {
            return probate;
          }
          //todo:1 look deeply
        } catch (IllegalAccessException e) {
          //noinspection UnnecessaryContinue
          continue;//perhaps some other field will both match and be accessible.
        }
      }
    }
    return null;
  }

  /**
   * random access by name, even works if hasNext() returns false.
   */
  public Type find(String named) {
    for (int fi = fields.length; fi-- > 0; ) {
      if (isMember[fi]) {
        Field f = fields[fi];
        if (fields[fi].getName().equals(named)) {//not using generic search as testing name is quite a bit faster.
          try {
            //noinspection unchecked
            return (Type) f.get(parent);
          } catch (IllegalAccessException e) {
            return null; //no point in looking further, names are unique.
          }
        }
      }
    }
    return null;
  }
}
