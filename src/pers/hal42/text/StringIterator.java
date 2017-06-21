package pers.hal42.text;

import java.util.Iterator;

/**
 * Created by Andy on 5/23/2017.
 * <p>
 * TO allow multiple string sources to exist in a FancyArgITerator
 */
public interface StringIterator extends Iterator<String> {
  boolean hasNext();

  /**
   * @returns null if hasNext() returned false;
   */
  String next();

  /** for those who like to run off the end of iteration */
  default String nextElse(String defawlt) {
    return hasNext() ? next() : defawlt;
  }

  /** makes hasNext() return false, frees and resources */
  default void discard() {
    //universal but expensive way to do it
    while (hasNext()) {
      next();
    }
  }
}
