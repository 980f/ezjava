package pers.hal42.stream;

import java.util.Vector;

public class ObjectFifo<T> {
  protected final Vector<T> storage;

  public ObjectFifo() {
    this(100);
  }

  public ObjectFifo(int presize) {
    storage = new Vector<>(presize, presize);
  }

  public synchronized Object[] snapshot() {
    return storage.toArray();
  }

  public T next() {
    synchronized (storage) {
      int size = storage.size();
      if (size-- > 0) {
        T gotten = storage.elementAt(size);
        storage.removeElementAt(size);
        return gotten;
      } else {
        return null;
      }
    }
  }

  public int put(T obj) {
    synchronized (storage) {
      storage.insertElementAt(obj, 0);
      return storage.size();
    }
  }

  public int atFront(T obj) {
    synchronized (storage) {
      storage.add(obj);
      return storage.size();
    }
  }

  public void Clear() {
    synchronized (storage) {
      storage.setSize(0);
    }
  }

  /////////////////////
  protected boolean remove(T obj) {
    synchronized (storage) {
      return storage.remove(obj);
      //does NOT interrupt. agent does NOT need to wake up to service a removal of something that it hasn't looked at yet.
    }
  }

  /**
   * remove anything satisfying obj.equals(fifoObj)
   */
  public int removeAny(T obj) {
    synchronized (storage) {
      int count = 0;
      while (storage.remove(obj)) {
        ++count;
      }
      return count;
    }
  }

  /**
   * This exists for reporting purposes only.  DO NOT use to iterate!
   */
  public final int Size() {
    return storage.size();
  }

  public boolean isEmpty() {
    return storage.size() <= 0;
  }

}

