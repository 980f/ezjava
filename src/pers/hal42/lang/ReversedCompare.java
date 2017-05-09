package pers.hal42.lang;

import java.util.*;

public class ReversedCompare extends NormalCompare {
/**
 * calls compare() with same order given in case of a not-quite compliant comparison.
 * uses negation to reverse the sense of the compare.
 */
  public int compare(Object o1, Object o2){
    return -super.compare(o1,o2);
  }
/**
 * compare COMPARATORS
 */
  public boolean equals(Object obj){
    return obj instanceof ReversedCompare;
  }

  private static ReversedCompare rshared=new ReversedCompare();

  public static Comparator New() {
    return rshared;
  }

}
