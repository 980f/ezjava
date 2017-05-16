package pers.hal42.lang;


import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

public class VectorX {
  private VectorX() {
    // I exist for static purposes
  }

  ///////// Collections omissions

  /**
   * @return @param v after reversing the order of its elements.
   */

  public static <T> Vector<T> reverse(Vector<T> v) {
    int size = size(v);
    if (size > 1) {
      T swap;
      int i = size;
      int other = 0;
      while (--i > other) {
        swap = v.elementAt(i);
        v.set(i, v.elementAt(other));
        v.set(other++, swap);
      }
    }
    return v;
  }

  public static int size(Vector v) {
    return v != null ? v.size() : ObjectX.INVALIDINDEX;
  }

  public static boolean NonTrivial(Vector v) {
    return v != null && v.size() > 0;
  }

  public static <T> Vector<T> fromArray(T[] oa) {
    Vector<T> v = new Vector<>(oa.length);
    v.setSize(oa.length);
    for (int i = oa.length; i-- > 0; ) {//preserve input order
      v.setElementAt(oa[i], i);
    }
    return v;
  }

  /**
   * @return @param v after inserting @param arf using @param ordering.
   * if an equivalent object is found then it is inserted FOLLOWING existing one.
   */
  public static <T> Vector orderedInsert(Vector<T> v, T arf, Comparator<T> ordering) {
    int location = Collections.binarySearch(v, arf, ordering);
    v.insertElementAt(arf, location < 0 ? ~location : location + 1);
    return v;
  }

  /**
   * @return whether @param arf was inserted into @param v using @param ordering.
   * if an equivalent object is found then new one is NOT inserted
   */
  public static boolean uniqueInsert(Vector v, Object arf, Comparator ordering) {
    int location = Collections.binarySearch(v, arf, ordering);
    if (location < 0) {
      v.insertElementAt(arf, ~location);
      return true;
    } else {
      return false;
    }
  }

  ////////
}
