package pers.hal42.text;

import java.util.Iterator;

/**
 * Created by Andy on 5/23/2017.
 *
 * TO allow multiple string sources to exist in a FancyArgITerator
 */
public interface StringIterator extends Iterator<String> {
  boolean hasNext();

  /**
   * @returns null if hasNext() returned false;
   */
  String next();

  default String nextElse(String defawlt) {
    return hasNext() ? next() : defawlt;
  }
}
