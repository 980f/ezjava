package pers.hal42.text;

import java.util.Stack;

/**
 * Created by Andy on 5/25/2017.
 * <p>
 * Intended for an argument list in which some members are instructions to include another list.
 * The determination that a member is an include reference is outside of this class.
 * Sensing 'include loops' is also outside the domain of this class.
 * This class discards exhausted iterators, a loop checking one has to hold on to them (or the info used to create them) to look for multi-level loops linked by last member of a list.
 */
public class FancyArgIterator implements StringIterator {

  private Stack<StringIterator> nesters;

  private StringIterator top;

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
  public String next() {
    return top != null ? top.next() : null;
  }

  /** iteration will exhaust the @param pusher then return to using the present one. */
  public FancyArgIterator push(StringIterator pusher) {
    if (pusher.hasNext()) {
      if (top != null && top.hasNext()) {//by testing hasNext we can replace instead of pushing an exhausted iterator
        if (nesters == null) {
          nesters = new Stack<>();
        }
        nesters.push(top);
      }
      top = pusher;
    }
    return this;
  }

  /** makes hasNext() return false, frees and resources */
  @Override
  public void discard() {
    if (top != null) {
      top.discard();
      if (nesters != null) {
        nesters.forEach(StringIterator::discard);
      }
      nesters = null;
    }
  }
}
