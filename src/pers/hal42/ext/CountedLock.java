package pers.hal42.ext;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by andyh on 4/3/17.
 * <p>
 * an attempt to mimic c++ 'decrement on going out of scope via "try with resources"
 * usage: try (Countedlock thistoo= thiscounter.open())
 */
public class CountedLock implements AutoCloseable {
  public java.util.concurrent.atomic.AtomicInteger counter = new AtomicInteger(0);

  @Override
  public void close() {
    counter.decrementAndGet();
  }

  /**
   * use this in a try-with-resources try clause, you may assign it to an AutoCloseable if you don't need the value in the try body.
   */
  public CountedLock open() {
    counter.incrementAndGet();
    return this;
  }

  public int asInt() {
    return counter.get();
  }

  public void setto(int i) {
    counter.set(i);
  }
}
