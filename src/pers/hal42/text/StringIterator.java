package pers.hal42.text;

import java.util.Iterator;

/**
 * Created by Andy on 5/23/2017.
 * <p>
 * An iterator of strings with enough defaults to operate as a useless instance of the interface.
 */
public interface StringIterator extends Iterator<String> {
  default boolean hasNext() {
    return false;
  }

  /**
   * @returns null if hasNext() returned false;
   */
  default String next() {
    return null;
  }

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
