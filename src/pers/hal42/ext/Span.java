package pers.hal42.ext;

/**
 * Created by andyh on 4/3/17.
 * <p>
 * a simple range class useful for identifying substrings
 */
public class Span {
  public int lowest;
  public int highest;
  public static final int Invalid = -1;  //what indexOf returns when it doesn't find a char

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
    highest = Invalid; //for safety
    lowest = low;
  }

  /** @returns whether subString will return something other than null */
  public boolean isStarted() {
    return lowest != Invalid;
  }

  /** @returns whether subString will return an actual subString as one normally thinks of that word. */
  public boolean isValid() {
    return Invalid != highest && lowest != Invalid;//often the low is valid but the high is not
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

  /** move low @param skip past high, set high to 'invalid' */
  public void leapfrog(int skip) {
    if (highest == Invalid) {//if at or past the end
      lowest = Invalid;   //we are past the end for sure.
    } else {//begin next field
      lowest = highest + skip;
      highest = Invalid;
    }
  }

  /** set both ends of span to 'invalid' */
  public void clear() {
    highest = lowest = Invalid;
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

  /** copy values from @param other then clear() other */
  public void take(Span other) {
    this.lowest = other.lowest;
    this.highest = other.highest;
    other.clear();
  }

  /** @returns a substring from @param tocut, moves the span past its present value by @param skip. skip is usually 1 when there is a comma or the like to be skipped past. */
  public String subString(String tocut, int skip) {
    if (lowest == Invalid) {
      return "";
      //and skipping doesn't make sense
    }
    try {
      if (highest == Invalid) {//then take tail
        return tocut.substring(lowest);
      } else {
        return tocut.substring(lowest, highest);
      }
    } finally {
      leapfrog(skip);
    }
  }

  @Override
  public String toString() {
    return "Span:{" +
      "lowest:" + lowest +
      ", highest:" + highest +
      '}';
  }

  public void end(int probe) {
    highest = probe;
  }
}
