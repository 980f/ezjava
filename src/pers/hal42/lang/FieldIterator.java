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
  private int pointer = 0;

  public FieldIterator(Class type, Object parent) {
    this.type = type;
    this.parent = parent;
    fields = parent.getClass().getFields();
  }

  private boolean belongs(Field f) {
    return type == null || ReflectX.isImplementorOf(f.getType(), type);
  }

  @Override
  public boolean hasNext() {
    while (pointer < fields.length) {
      Field f = fields[pointer];
      if (belongs(f)) {
        return true;
      }
      ++pointer;
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

  /**
   * random access by name, even works if hasNext() returns false.
   */
  public Type find(Predicate<Type> filter) {
    for (Field f : fields) {
      if (belongs(f)) {
        try {
          //noinspection unchecked
          Type probate = (Type) f.get(parent);
          if (filter.test(probate)) {
            return probate;
          }
        } catch (IllegalAccessException e) {
          return null;
        }
      }
    }
    return null;
  }

  /**
   * random access by name, even works if hasNext() returns false.
   */
  public Type find(String named) {
    for (Field f : fields) {
      if (f.getName().equals(named)) {//not using generic search as testing name is quite a bit faster.
        if (belongs(f)) {
          try {
            //noinspection unchecked
            return (Type) f.get(parent);
          } catch (IllegalAccessException e) {
            return null;
          }
        }
      }
    }
    return null;
  }
}
