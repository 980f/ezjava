package pers.hal42.lang;

/**
 * sanity checked int, for use as a 0-based array index.
 * unsigned reasoning is used for all index operations.
 * <p>
 * Created by andyh on 4/3/17.
 */
public class Index {
  /**
   * a magic value for 'there is no index', it is all ones.
   */
  public static final int BadIndex = ~0;//~0 is a.k.a -1, but doesn't look like you are subtracting something.
  protected int raw;

  /** force user to explicitly initialize */
  public Index(int raw) {
    this.raw = raw;
  }

  /**
   * we default indexes to ~0, and return that as 'invalid index'
   */
  boolean isValid(int index) {
    return index != BadIndex;
  }

  public boolean isOdd() {
    return isValid() && ((raw & 1) != 0);//lsb === is odd.
  }

  public int setto(int ord) {
    return raw = ord;
  }

  /** */
  public int value() {
    return raw;
  }

  public boolean isValid() {
    return isValid(raw);
  }

  public void clear() {
    raw = BadIndex;
  }

  /**
   * @return whether this is less than @param limit
   */
  public boolean in(Index limit) {
    return isValid() && limit.isValid() && raw < limit.raw;
  }

  /**
   * @return whether this contains @param other
   */
  public boolean has(Index other) {
    return isValid() && other.isValid() && raw > other.raw;
  }

  /**
   * maydo: convert negatives to canonical ~0
   */
  public int inc(int other) {
    return raw += other;
  }

  /**
   * decrement IF valid
   */
  public int dec(int other) {
    if (raw >= other) {
      return raw -= other;
    } else {
      return raw = BadIndex;
    }
  }

  /**
   * maydo: convert negatives to canonical ~0
   */
  public int postinc() {
    return raw++;
  }

  /**
   * maydo: convert negatives to canonical ~0
   */
  public int inc() {
    return ++raw;
  }

  /** @returns an element from @param array if it contains this index, else returns @param onBadIndex */
  public <AT> AT get(AT[] array, AT onBadIndex) {
    if (isValid() && array.length > raw) {
      return array[raw];
    } else {
      return onBadIndex;
    }
  }

  @Override
  public String toString() {
    return Integer.toString(raw);
  }
}
