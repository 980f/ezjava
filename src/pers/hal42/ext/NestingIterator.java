package pers.hal42.ext;

import java.util.Iterator;
import java.util.Stack;

/** a well managed stack of iterators. @see {@link DeepIterator} for a common use case of this class. */
public class NestingIterator<T> implements Iterator<T> {
  private Stack<Iterator<T>> nesters;

  private Iterator<T> top;

  @Override
  public boolean hasNext() {
    while (top != null) {//todo:1 we might be able to use an if instead of a while due to the care taken in not pushing exhausted iterators.
      if (top.hasNext()) {
        return true;
      }
      top = null;//release the exhausted iterator as soon as posible.
      if (nesters == null || nesters.empty()) {
        return false;
      } else {
        top = nesters.pop();
      }
    }
    return false;
  }

  @Override
  public T next() {
    return top != null ? top.next() : null;
  }

  /** iteration will exhaust the @param pusher then return to using the present one. */
  public void push(Iterator<T> pusher) {
    if (pusher.hasNext()) {
      if (top != null && top.hasNext()) {//by testing hasNext we can replace instead of pushing an exhausted iterator
        if (nesters == null) {
          nesters = new Stack<>();
        }
        nesters.push(top);
      }
      top = pusher;
    }
  }
}
