package pers.hal42.text;

import java.util.Iterator;

/** Iterate Strings from some other iterator */
public class StringifyingIterator implements StringIterator {

  Iterator wrapped;

  public StringifyingIterator(Iterator wrapped) {
    this.wrapped = wrapped;
  }

  public boolean hasNext() {
    return wrapped.hasNext();
  }

  /**
   * @returns null if hasNext() returned false;
   */
  public String next() {
    return String.valueOf(wrapped.next());
  }
}

