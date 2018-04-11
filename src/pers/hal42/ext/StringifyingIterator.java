package pers.hal42.ext;

import java.util.Iterator;
import java.util.function.Function;

/**
 * given an iterator of the template class and a function for converting that to string, yields that function applied to those things.
 * if the function is null then String.valueOf() is the function used.
 */
public class StringifyingIterator<T> implements Iterator<String> {
  Iterator<T> things;
  Function<T, String> stringer;

  public StringifyingIterator(Iterator<T> things, Function<T, String> stringer) {
    this.things = things;
    this.stringer = stringer;
  }

  public StringifyingIterator(Iterable<T> things, Function<T, String> stringer) {
    this.things = things.iterator();
    this.stringer = stringer;
  }

  @Override
  public boolean hasNext() {
    return things.hasNext();
  }

  @Override
  public String next() {
    T thing = things.next();
    if (stringer != null) {
      return stringer.apply(thing);
    } else {
      return String.valueOf(thing);
    }
  }
}
