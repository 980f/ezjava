package pers.hal42.lang;

import java.util.Iterator;

/**
 * surely this is somewhere in the standard library!
 * Iterator over an array
 */
public class ArrayIterator<T> implements Iterator<T> {
  int pointer;
  T[] things;

  public ArrayIterator(T[] things) {
    pointer = 0;
    this.things = things;
  }

  @Override
  public boolean hasNext() {
    return pointer < things.length;
  }

  @Override
  public T next() {
    return things[pointer++];
  }
}
