package pers.hal42.lang;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.function.Predicate;

/**
 * Created by Andy on 6/8/2017.
 * <p>
 * To return fields of an object that are of a given type.
 */
public class FieldIterator<Type> implements Iterator<Type> {
  private final Class type;//must keep this ourselves as java generics are compile time.
  private final Object parent;
  private final Field[] fields;
  /** cache fied test, for when we rewind frequently */
  private final boolean[] isMember;
  private int pointer;

  public FieldIterator(Class type, Object parent) {
    this.type = type;
    this.parent = parent;
    fields = parent.getClass().getFields();
    isMember = new boolean[fields.length];
    for (pointer = fields.length; pointer-- > 0; ) {
      isMember[pointer] = belongs(fields[pointer]);
    }
  }

  /** while only used in one place this is hard enough to read to be worth its own method*/
  private boolean belongs(Field f) {
    return type == null || ReflectX.isImplementorOf(f.getType(), type);
  }

  @Override
  public boolean hasNext() {
    for (; pointer < fields.length; ++pointer) {
      if (isMember[pointer]) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Type next() {
    try {
      Field f = fields[pointer++];//hasNext has qualified this
      //noinspection unchecked
      return (Type) f.get(parent);
    } catch (Exception e) {//guard against user failing to call hasNext.
      return null;
    }
  }

  public void rewind(){
    pointer=0;
  }

  /**
   * @returns some field that meets @param filter, null if none.
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
