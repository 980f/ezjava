package pers.hal42.text;

import java.util.Stack;

/**
 * Created by Andy on 5/25/2017.
 *
 * Intended for an argument list in which some members are instructions to include aother list.
 * The determination that a member is an include reference is outside of this class.
 * Sensing 'include loops' is also outside the domain of this class.
 * This class discards exhausted iterators, a loop checking one has to hold on to them (or the info used to create them) to look for multi-level loops linked by last member of a list.
 */
public class FancyArgIterator implements StringIterator {

  private Stack<StringIterator> nesters;

  private StringIterator top;

  @Override
  public boolean hasNext() {
    while (top != null) {//todo:1 we might be able to use an if instead of a while due to the care taken in not pushing exhasuted iterators.
      if (top.hasNext()) {
        return true;
      }
      top=null;//release the exhausted iterator as soon as posible.
      if (nesters != null && !nesters.empty()) {
        top = nesters.pop();
      } else {
        return false;
      }
    }
    return false;
  }

  @Override
  public String next() {
    return top != null ? top.next() : null;
  }

  public void push(StringIterator pusher) {
    if (pusher.hasNext()) {
      if (top != null && top.hasNext()) {//by testing hasNext we can replace  instead of pushing an exhausted iterator
        if (nesters == null) {
          nesters = new Stack<>();
        }
        nesters.push(top);
      }
      top = pusher;
    }
  }

}
