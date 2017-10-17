package pers.hal42.lang;

import pers.hal42.text.StringIterator;

import java.lang.annotation.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class EnumerationFilter<E extends Enum> {
  /**
   * marker annotation for use by applyTo and apply()
   * if class is annotated with @Stored then process all fields, not just those annotated with Stored.
   */
  @Documented
  @Inherited
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD})
  /** marks a static method that either takes a char and returns an Enum or vice versa*/
  public @interface Xform {
    //public static Enum whatever(char ch)
    boolean parser();

    //public static char whatever(Enum item)
    boolean packer();
  }

  protected Class<? extends E> claz;
  protected boolean[] present;
  protected E[] value;
  private Method packer;
  private Method parser;

  @SuppressWarnings("unchecked")
  public EnumerationFilter(Class claz) {
    this.claz = claz;
    value = (E[]) claz.getEnumConstants();
    present = new boolean[value.length];
    packer = ReflectX.methodFor(claz, Xform.class, Xform::packer);
    parser = ReflectX.methodFor(claz, Xform.class, Xform::parser);
  }

  /** named for use as a Predicate<InstitutionType> */
  public boolean test(E it) {
    return it != null && present[it.ordinal()];
  }

  public Iterator<E> iterator() {
    return new Iterator<>();
  }

  public EnumerationFilter<E> clear() {
//todo:2    Arrays.setAll(present,false);
    for (int i = present.length; i-- > 0; ) {
      present[i] = false;
    }
    return this;
  }

  public EnumerationFilter<E> parseList(StringIterator list) {
    while (list.hasNext()) {
      String next = list.next();
      Enum item = ReflectX.parseEnum(claz, next);
      if (item != null) {
        present[item.ordinal()] = true;
      }
    }
    return this;
  }

  public void parse(String packed) {
    if (packed != null) {
      final byte[] bytes = packed.getBytes();

      if (parser != null) {
        for (byte ch : bytes) {
          try {
            @SuppressWarnings("unchecked")
            E it = (E) parser.invoke(null, ch);
            if (it != null) {
              present[it.ordinal()] = true;
            }
          } catch (IllegalAccessException | InvocationTargetException e) {
            //ignored input
          }
        }
      } else {
        //class not suitable for automatic packing.
      }
    }
  }

  @Override
  public String toString() {
    if (packer != null) {
      return packed();
    } else {
      return super.toString();
    }
  }

  public String packed() {

    if (packer != null) {
      StringBuilder packed = new StringBuilder(present.length);
      Iterator<E> inefficient = this.iterator();
      while (inefficient.hasNext()) {
        try {
          packed.append(packer.invoke(null, inefficient.next()));
        } catch (IllegalAccessException | InvocationTargetException e) {
          return null;//method improperly declared
        }
      }
      return packed.toString();
    } else {
      return null;
    }
  }

  public class Iterator<E> implements java.util.Iterator<E> {
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
        return (E) value[pointer++];
      }
      return null;
    }
  }
}
