package pers.hal42.text;

import java.util.Iterator;

/**
 * Iterate Strings from some other iterator using String.valueOf() to conver to text, except that null objects are mapped to the configured 'forNull' member.
 * The 'forNull' functionality is handy for yielding a blank instead of the text "(null)" for null objects.
 *
 * @see pers.hal42.ext.StringifyingIterator for a fancier template checked + functional variation of this.
 */
public class StringifyingIterator implements StringIterator {
  /** what to convert null into. */
  public String forNull = "";

  public Iterator wrapped;

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

    final Object item = wrapped.next();
    if (item == null) {
      return forNull;
    } else {
      return String.valueOf(item);
    }
  }
}

