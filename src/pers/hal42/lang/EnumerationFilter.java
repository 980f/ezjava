package pers.hal42.lang;

import pers.hal42.logging.ErrorLogStream;
import pers.hal42.text.StringIterator;
import pers.hal42.transport.Storable;
import pers.hal42.transport.Xformer;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.function.Consumer;

@Storable.Stored(parseList = "parseList") //marks this as having a method for parsing from StringIterator
public class EnumerationFilter<E extends Enum> {
  public final Class<? extends E> claz;
  /**
   * for the convenience of manual editing setting this guy makes the others moot.
   */
  @Storable.Stored
  public boolean all = true;
  /** cache of enum constants */
  public final E[] value;
  /** cached method lookups for string<->array of booleans transformations */
  public final Xformer xf;
  /** a boolean for each enum value */
  protected final boolean[] present;

  public EnumerationFilter(Class<? extends E> claz) {
    this.claz = claz;
    value = claz.getEnumConstants();
    present = new boolean[value.length];
    xf = new Xformer(claz);
  }

  /** named for use as a Predicate<E> */
  public boolean test(E it) {
    return all || (it != null && present[it.ordinal()]);
  }

  /** @returns whether any of the items are 'present' */
  @SafeVarargs
  public final boolean any(E... items) {
    if (all) {
      return true;
    }
    for (E item : items) {
      if (test(item)) {
        return true;
      }
    }
    return false;
  }

  /** @returns whether ALL of the items are 'present' */
  @SafeVarargs
  public final boolean all(E... items) {
    if (all) {
      return true;
    }
    for (E item : items) {
      if (!test(item)) {
        return false;
      }
    }
    all = true;//
    return true;
  }

  /** @returns whether no items are permitted/present */
  public boolean hasNone() {
    if (all) {
      return false;
    }
    for (boolean one : present) {
      if (one) {
        return false;
      }
    }
    return true;
  }

  /** forget that any items were 'permitted/present' */
  public EnumerationFilter<E> clear() {
    all = false;
//todo:2    Arrays.setAll(present,false);
    for (int i = present.length; i-- > 0; ) {
      present[i] = false;
    }
    return this;
  }

  /** try to set a permission. @returns whether @param item was valid (not null) */
  public boolean allow(E item) {
    if (item != null) {
      present[item.ordinal()] = true;
      return true;
    }
    return false;
  }

  /** a convenience for testing: only permit the given @param item */
  public void doOnly(E item) {
    clear();
    allow(item);
  }

  /** set permissions according to a list of fullsized tags */
  public EnumerationFilter<E> parseList(StringIterator list) {
    clear();//all usages at one point did this so I brought it into this method.
    while (list.hasNext()) {
      allow(ReflectX.parseEnum(claz, list.next()));
    }
    return this;
  }

  /** set permissions from single letter abbreviations packed into a string */
  public boolean parse(String packed) {
    if (packed != null) {
      if (xf.parser != null) {
        for (char ch : packed.toCharArray()) {
          try {
            //noinspection unchecked
            allow((E) xf.parser.invoke(null, ch));
          } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
            //ignored input
            ErrorLogStream.Global().Caught(e, "invoking an enum parser");
          }
        }
        return true;
      } else {
        //class not suitable for automatic packing.
        return false;
      }
    } else {
      return true;//we parse 'nothing' just fine, even if we can't parse at all ;)
    }
  }

  @Override
  public String toString() {
    if (xf.packer != null) {
      return packed();
    } else {
      return super.toString();
    }
  }

  public String packed() {
    if (xf.packer != null) {
      StringBuilder packed = new StringBuilder(present.length);
      forEach(item -> packed.append(xf.pack(item)));//throws stuff when class involved is not compliant
      return packed.toString();
    } else {
      return null;
    }
  }

  public Iterator<E> iterator() {
    return new Iterator<E>() {//yet another lookahead iterator.
      protected int pointer = 0;

      @Override
      public boolean hasNext() {
        for (; pointer < present.length; ++pointer) {
          if (present[pointer]) {
            return true;
          }
        }
        return false;
      }

      @SuppressWarnings("unchecked")
      @Override
      public E next() {
        if (pointer < present.length) {
          return value[pointer++];
        }
        return null;
      }
    };
  }

  public void forEach(Consumer<? super E> action) {
    iterator().forEachRemaining(action);
  }

}
