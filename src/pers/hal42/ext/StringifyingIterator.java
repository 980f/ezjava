package pers.hal42.ext;

import java.util.Iterator;
import java.util.function.Function;

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
