package pers.hal42.ext;

import java.util.Iterator;
import java.util.function.Predicate;

public class FilteringNonNullIterator<ColumnAttributes> implements Iterator<ColumnAttributes> {
  /** number of times next() has returned something under proper use */
  public int count = 0;
  private final Predicate<ColumnAttributes> acceptor;
  private Iterator<ColumnAttributes> wrapped;
  private ColumnAttributes prefetched = null;

  public FilteringNonNullIterator(Iterator<ColumnAttributes> wrapped, Predicate<ColumnAttributes> acceptor) {
    this.wrapped = wrapped;
    this.acceptor = acceptor;
  }

  @Override
  public boolean hasNext() {
    if (prefetched == null) {
      while (wrapped.hasNext()) {
        prefetched = wrapped.next();
        if (((prefetched != null) && (acceptor == null)) || acceptor.test(prefetched)) {
          return true;
        }
      }
      prefetched = null;
    }
    return false;
  }

  @Override
  public ColumnAttributes next() {
    try {
      if (prefetched != null) {
        ++count;
      }
      return prefetched;
    } finally {
      prefetched = null;
    }
  }

  @Override
  public void remove() {
    //doesn't work due to prefetch.
    throw new UnsupportedOperationException("remove");
  }
}

