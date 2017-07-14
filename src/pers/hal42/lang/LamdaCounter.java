package pers.hal42.lang;

/**
 * Created by Andy on 7/14/2017.
 * <p>
 * a loop counter usable inside a lambda
 */
public class LamdaCounter {
  public int count;

  public LamdaCounter(int start) {
    count = start;
  }

  public int incr() {
    return count++;
  }
}
