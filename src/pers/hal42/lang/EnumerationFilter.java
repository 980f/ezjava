package pers.hal42.lang;

import pers.hal42.logging.ErrorLogStream;
import pers.hal42.text.StringIterator;
import pers.hal42.transport.Storable;
import pers.hal42.transport.Xformer;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

@Storable.Stored(parseList = "parseList") //marks this as having a method for parsing from StringIterator
public class EnumerationFilter<E extends Enum> {

  protected Class<? extends E> claz;
  /**
   * for the convenience of manual editing setting this guy makes the others moot.
   */
  @Storable.Stored
  public boolean all = true;

  protected boolean[] present;
  protected E[] value;
  Xformer xf;
  @SuppressWarnings("unchecked")
  public EnumerationFilter(Class claz) {
    this.claz = claz;
    value = (E[]) claz.getEnumConstants();
    present = new boolean[value.length];
    xf = new Xformer(claz);
  }

  /** named for use as a Predicate<InstitutionType> */
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

  /** forget that any itmes were 'present' */
  public EnumerationFilter<E> clear() {
    all = false;
//todo:2    Arrays.setAll(present,false);
    for (int i = present.length; i-- > 0; ) {
      present[i] = false;
    }
    return this;
  }

  /** try to set a permission. @returns whether @param item was valid */
  public boolean allow(E item) {
    if (item != null) {
      present[item.ordinal()] = true;
      return true;
    }
    return false;
  }

  /** a convenience for testing */
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

  /** set permissions from single letter abbreviations */
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
      Iterator<E> inefficient = this.iterator();
      while (inefficient.hasNext()) {
        try {
          packed.append(xf.pack(inefficient.next()));
        } catch (Throwable e) {
          return null;//method improperly declared
        }
      }
      return packed.toString();
    } else {
      return null;
    }
  }

  public Iterator<E> iterator() {
    return new Iterator<E>() {
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

  public boolean hasNone() {
    for (boolean one : present) {
      if (one) {
        return false;
      }
    }
    return true;
  }
}
