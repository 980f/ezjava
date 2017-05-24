package pers.hal42.text;

/**
 * Created by Andy on 5/23/2017.
 */
public interface StringIterator {
  boolean hasNext();

  /**
   * @returns null if hasNext() returned false;
   */
  String next();

  default String nextElse(String defawlt) {
    return hasNext() ? next() : defawlt;
  }
}
