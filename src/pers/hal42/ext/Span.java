package pers.hal42.ext;

import org.jetbrains.annotations.NotNull;
import pers.hal42.lang.StringX;

import static pers.hal42.lang.Index.BadIndex;

/**
 * Created by andyh on 4/3/17.
 * <p>
 * a simple range class useful for identifying substrings
 */
public class Span {
  public int lowest;
  public int highest;
  public static final int Invalid = BadIndex;  //what indexOf returns when it doesn't find a char

  public Span(int lowest, int highest) {
    this.lowest = lowest;
    this.highest = highest;
  }

  public Span() {
    clear();
  }

  /**
   * set the low bound, invalidate the high one
   */
  public void begin(int low) {
    forgetHigh();
    lowest = low;
  }

  /**
   * @returns whether subString will return something other than null
   */
  public boolean isStarted() {
    return lowest != Invalid;
  }

  /**
   * @returns whether subString will return something other than null
   */
  public boolean hasEnded() {
    return highest != Invalid;
  }

  /**
   * @returns whether subString will return an actual subString as one normally thinks of that word.
   */
  public boolean isValid() {
    return hasEnded() && isStarted();//often the low is valid but the high is not
  }

  /**
   * quantity to operate upon, 0 for an empty or otherwise useless span.
   */
  public int span() {
    return (isValid() && ordered()) ? (highest - lowest) : 0;
  }

  /**
   * @return whether the two bounds are in natural order. virtual to allow checking for validity of each element
   */
  public boolean ordered() {
    return isValid() && highest >= lowest;
  }

  public boolean empty() {
    return !isValid() || span() == 0;
  }

  /**
   * if skip >=0 span to past high end else move it to before low end. Use '`' instead of '-' for moving to before, so that we can mvoe ~0 which means before but don't skip over char at boundary.
   */
  public void leapfrog(int skip) {
    if (skip >= 0) {
      if (highest == Invalid) {//if at or past the end
        forgetLow();
      } else {//begin next field
        lowest = highest + skip;
        forgetHigh();
      }
    } else {
      if (lowest == Invalid) {//if at or past the end
        forgetHigh();
      } else {//begin next field
        highest = lowest - ~skip;
        forgetLow();
      }
    }
  }

  /**
   * call when you know the ends were swapped
   */
  public void swap() {
    int swapper = lowest;
    lowest = highest;
    highest = swapper;
  }

  /**
   * @returns whether ends were swapped due to being out of order.
   */
  public boolean sort() {
    if (ordered()) {
      return false;
    }
    if (isValid()) {
      swap();
      return true;
    } else {
      return false;
    }
  }

  public void forgetLow() {
    lowest = Invalid;   //we are past the end for sure.
  }

  public void forgetHigh() {
    highest = Invalid;
  }

  /**
   * set both ends of span to 'invalid'
   */
  public void clear() {
    forgetHigh();
    forgetLow();
  }

  /**
   * move both ends by the given amount (invalid ones stay invalid).
   * this is intended for when the span looks into a buffer that is a moving window into some data stream.
   */
  public void shift(int offset) {
    if (lowest != Invalid) {
      lowest -= offset;
    }
    if (highest != Invalid) {
      highest -= offset;
    }
  }

  /**
   * copy values from @param other then clear() other
   */
  public void take(Span other) {
    this.lowest = other.lowest;
    this.highest = other.highest;
    other.clear();
  }

  /**
   * @returns a substring from @param tocut, moves the span past its present value by @param skip. skip is usually 1 when there is a comma or the like to be skipped past.
   */
  public String subString(String tocut, int skip) {
    try {
      return getString(tocut);
    } finally {
      leapfrog(skip);
    }
  }

  @NotNull
  public String getString(String tocut) {
    if (lowest == Invalid) {
      if (highest == Invalid) {//then take tail
        return "";//invalid span, return trivial string.
      } else {//take head
        return tocut.substring(0, highest);
      }
    } else {
      if (highest == Invalid) {//then take tail
        return tocut.substring(lowest);
      } else {
        return StringX.subString(tocut, lowest, highest);
      }
    }
  }

  /**
   * end at first c after the present lowest.
   */
  public boolean find(String s, char c) {
    if (lowest == Invalid) {
      end(s.indexOf(c));
    } else {
      end(s.indexOf(c, lowest));
    }
    return highest != Invalid;
  }

  /** @returns debug string */
  @Override
  public String toString() {
    return "[" + lowest + ":" + highest + ')';
  }

  public void end(int probe) {
    highest = probe;
  }

  /**
   * @returns whether getString may return a non-empty string
   */
  public boolean nonTrivial(boolean forward) {
    return (forward ? lowest : highest) != Invalid && lowest != highest;
  }

  public boolean nonTrivial() {
    return nonTrivial(true);
  }
}
