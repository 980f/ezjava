package pers.hal42.ext;

/**
 * Created by andyh on 4/3/17.
 * <p>
 * an attempt to mimic c++ 'decrement on going out of sccope
 */
public class CountedLock implements AutoCloseable {
  public java.util.concurrent.atomic.AtomicInteger counter;

  @Override
  public void close() {
    counter.decrementAndGet();
  }

  public void open() {
    counter.incrementAndGet();
  }

  int asInt() {
    return counter.get();
  }

  public void setto(int i) {
    counter.set(i);
  }
}
