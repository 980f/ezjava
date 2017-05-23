package pers.hal42.stream;

import java.util.Vector;

public class ObjectFifo<T> {
  protected final Vector<T> fifo;

  public ObjectFifo() {
    this(100);
  }

  public ObjectFifo(int presize) {
    fifo = new Vector<>(presize, presize);
  }

  public synchronized Object[] snapshot() {
    return fifo.toArray();
  }

  public T next() {
    synchronized (fifo) {
      int size = fifo.size();
      if (size-- > 0) {
        T gotten = fifo.elementAt(size);
        fifo.removeElementAt(size);
        return gotten;
      } else {
        return null;
      }
    }
  }

  public int put(T obj) {
    synchronized (fifo) {
      fifo.insertElementAt(obj, 0);
      return fifo.size();
    }
  }

  public int atFront(T obj) {
    synchronized (fifo) {
      fifo.add(obj);
      return fifo.size();
    }
  }

  public void Clear() {
    synchronized (fifo) {
      fifo.setSize(0);
    }
  }

  /////////////////////
  protected boolean remove(T obj) {
    synchronized (fifo) {
      return fifo.remove(obj);
      //does NOT interrupt. agent does NOT need to wake up to service a removal of something that it hasn't looked at yet.
    }
  }

  /**
   * remove anything satisfying obj.equals(fifoObj)
   */
  public int removeAny(T obj) {
    synchronized (fifo) {
      int count = 0;
      while (fifo.remove(obj)) {
        ++count;
      }
      return count;
    }
  }

  /**
   * This exists for reporting purposes only.  DO NOT use to iterate!
   */
  public final int Size() {
    return fifo.size();
  }

  public boolean isEmpty() {
    return fifo.size() <= 0;
  }

}

