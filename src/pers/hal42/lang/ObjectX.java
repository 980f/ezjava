package pers.hal42.lang;

//import static pers.hal42.lang.Safe.INVALIDINDEX;

import static pers.hal42.lang.Index.BadIndex;

public class ObjectX {

  private ObjectX() {
    // I exist for static purposes
  }

  /** @returns whether objects are equal, with sane behavior on either being null */
  public static boolean Equals(Object one, Object other) {
    if (one == null) {
      return other == null;
    } else {
      return other != null && one.equals(other);
    }
  }

  /**
   * @return true if @param arf is an instanceof @param filter. will get fancier over time.
   * if filter is null then returns true if object exists.
   */
  public static boolean typeMatch(Object arf, Class filter) {
    return arf != null && (filter == null || arf.getClass().equals(filter));
  }

  public static Object OnTrivial(Object primary, Object backup) {
    return NonTrivial(primary) ? primary : backup;
  }

  /**
   * linear search
   */
  public static <T extends Comparable<T>> int linearSearch(T[] a, T key) {
    for (int i = a.length; i-- > 0; ) {
      int cmp = a[i].compareTo(key);
      if (cmp < 0) {
        //noinspection UnnecessaryContinue
        continue;
      } else if (cmp > 0) {
        return ~i; // put it here
      } else {
        return i; // key found
      }
    }
    return BadIndex; // insert at the end
  }

  public static int findIndex(Object[] a, Object o) {
    for (int i = a.length; i-- > 0; ) {
      if (a[i] == null) {
        if (o == null) {
          return i; // well ???
        }
      } else {
        if (a[i].equals(o)) {
          return i;
        }
      }
    }
    return BadIndex; // insert at the end
  }

  public static boolean NonTrivial(Object o) {
    if (o instanceof String) {
      return StringX.NonTrivial((String) o);
    }
    if (o instanceof StringBuffer) {
      return StringX.NonTrivial((StringBuffer) o);
    }
    // DO NOT import stuff from util here!
    // Call the appropriate class from your code and not this one!
    return o != null;
  }
}
