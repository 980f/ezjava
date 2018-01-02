package pers.hal42.ext;

import java.util.Iterator;
import java.util.function.Function;

public class StringifyingArrayIterator<T> implements Iterator<String> {
  int pointer = 0;
  T things[];
  Function<T, String> stringer = null;

  public StringifyingArrayIterator(T[] things, Function<T, String> stringer) {
    this.things = things;
    this.stringer = stringer;
  }

  @Override
  public boolean hasNext() {
    return pointer < things.length;
  }

  @Override
  public String next() {
    T thing = things[pointer++];
    if (stringer != null) {
      return stringer.apply(thing);
    } else {
      return String.valueOf(thing);
    }
  }

  void rewind() {
    pointer = 0;
  }

  boolean unget() {
    if (pointer > 0) {
      --pointer;
      return true;
    }
    return false;
  }
}
