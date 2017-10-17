package pers.hal42.lang;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.function.Predicate;

import static pers.hal42.lang.Index.BadIndex;

/**
 * Created by Andy on 6/8/2017.
 * <p>
 * To return non-null fields of an object that are of a given type.
 * There is a little bit of caching so members created between instantiation of the Iterator and using it might be skipped.
 * <p>
 * One could craft a similar class that ignores existence and can find fields deeply at the expense of not handing out objects related to the field.
 * <p>
 * There is a flag to optimize via a shallow scan.
 */
public class FieldIterator<Type> implements Iterator<Type> {
  private final Class type;//must keep this ourselves as java generics are compile time.
  private final Object parent;
  private final boolean deeply;

  private final Field[] fields;
  /** cache field test, for when we rewind frequently */
  private final boolean[] isMember;

  private int pointer;
  private Type prefetched = null;

  //tree walk
  private FieldIterator<Type> nested = null;

  /** @param type is used for RTTI, @param parent gives us actual content, @param deeply is whether to recursively inspect objects for the given type, usually you should say 'true', 'false' is faster but can leave things out except for very flat classes. */
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

  /** @returns a reasonable though not definitive estimate of how many iterations will occur. If @see deeply is true this can be grossly too low. */
  public int estimatedCount() {
    return fields.length;
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
    if (pointer >= 0 && prefetched == null) {//pointer will be -1 if there are no fields and as such isMember has zero length.
      for (; pointer < fields.length; ++pointer) {
        if (isMember[pointer]) {
          try {
            //noinspection unchecked
            prefetched = (Type) fields[pointer].get(parent);
            if (prefetched != null) {
              return true;
            } else {
              continue;
            }
          } catch (IllegalAccessException e) {
            //ignore inaccessible members of the type
            continue;
          }
        }
        if (deeply) {
          final Field field = fields[pointer]; //maydo: exclude static members?
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
      pointer = BadIndex;//save time when we repeatedly check after exhaustion.
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
        return prefetched;
      } catch (Exception e) {//guard against user failing to call hasNext.
        return null;  //note the enclosing class of the field must be public else we get an exception
      } finally {
        ++pointer;
        prefetched = null;
      }
    } else {
      return null;
    }
  }

  public void rewind() {
    nested = null;
    pointer = isMember.length > 0 ? 0 : BadIndex;
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
