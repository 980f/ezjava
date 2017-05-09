package pers.hal42.lang;

import java.util.*;

public class NormalCompare implements Comparator {
/**
 * calls compareTo with same order given in case of a not-quite compliant comparison.
 * if object not comparable uses hashcode().
 * a non null object is greater than a null object.
 */

  public int compare(Object o1, Object o2){
    try {
      return ((Comparable)o1).compareTo(o2);
    }
    catch(ClassCastException cce){
      return o1.hashCode()-o2.hashCode();
    }
    catch (NullPointerException npe) {
      return o1!=null ? 1 : ( (o2!=null) ? -1 : 0 ) ;
    }

  }
/**
 * compare COMPARATORS
 */
  public boolean equals(Object obj){
    return obj instanceof NormalCompare;
  }

  private static NormalCompare shared=new NormalCompare();

  public static Comparator New() {
    return shared;
  }

}

