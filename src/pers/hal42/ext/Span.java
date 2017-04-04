package pers.hal42.ext;

import pers.hal42.lang.ObjectX;

import static pers.hal42.lang.ObjectX.INVALIDINDEX;

/**
 * Created by andyh on 4/3/17.
 * <p>
 * a simple range class useful for identifying substrings
 */
public class Span {
  public int lowest;
  public int highest;

  public Span(int lowest, int highest) {
    this.lowest = lowest;
    this.highest = highest;
  }

  public Span() {
    clear();
  }

  /** set the low bound, invalidate the high one */
  public void begin(int low){
    highest = ObjectX.INVALIDINDEX; //for safety
    lowest = low;
  }

  public boolean isValid() {
    return INVALIDINDEX != highest && lowest != INVALIDINDEX;//often the low is valid but the high is not
  }

  /**
   * quantity to operate upon
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


  public void leapfrog(int skip) {
    lowest = highest + skip;
    highest = INVALIDINDEX;
  }

  public void clear() {
    highest = lowest = INVALIDINDEX;
  }

  public void shift(int offset) {
    if (lowest != INVALIDINDEX) {
      lowest -= offset;
    }
    if (highest != INVALIDINDEX) {
      highest -= offset;
    }
  }

  public void take(Span other) {
    this.lowest = other.lowest;
    this.highest = other.highest;
    other.clear();
  }

  @Override
  public String toString() {
    return "Span{" +
      "lowest=" + lowest +
      ", highest=" + highest +
      '}';
  }
}
