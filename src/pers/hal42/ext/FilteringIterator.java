package pers.hal42.ext;

import java.util.Iterator;
import java.util.function.Predicate;

/** return a subset of an iterator, allowing nulls in and out */
public class FilteringIterator<T> implements Iterator<T> {
  public Iterator<T> wrapped;
  public Predicate<T> filter;
  public int count = 0;
  protected T lookAhead;
  /** this is used to allow nulls to pass through. */
  protected boolean finished = false;
  protected boolean found = false;

  private FilteringIterator(Iterator<T> wrapped, Predicate<T> filter) {
    this.wrapped = wrapped;
    this.filter = filter;
  }

  @Override
  public boolean hasNext() {
    if (found) {
      return true;
    }
    if (finished) {
      return false;
    }
    while (!found && wrapped.hasNext()) {
      lookAhead = wrapped.next();
      if (filter == null || filter.test(lookAhead)) {
        found = true;
        return true;
      }
    }
    finished = true;
    return false;
  }

  @Override
  public T next() {
    try {
      if (found) {
        ++count;
      }
      return found ? lookAhead : null;
    } finally {
      found = false;
    }
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("remove");
  }
}
