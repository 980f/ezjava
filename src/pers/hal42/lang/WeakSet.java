package pers.hal42.lang;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.*;

/**
 * +++ TODO: Propagate this to other classes that can/should use it:
 * TempFile, LogFile, FileZipper, LogSwitch, SyncMonitorList, but NOT PrintForks.
 * <p>
 * The comments here were badly copied from an actual WeakMap implementation.
 *
 * @author Mark Reinhold
 * @version 1.12, 02/02/00
 * @see java.util.HashMap
 * @see java.lang.ref.WeakReference
 * @since 1.2
 */

public class WeakSet<T> extends AbstractSet<T> {

  Map<Integer, WeakReference<T>> hash = null;

  public WeakSet(int initialCapacity, float loadFactor) {
    hash = Collections.synchronizedMap(new HashMap<Integer, WeakReference<T>>(initialCapacity, loadFactor));
  }

  public WeakSet(int initialCapacity) {
    this(initialCapacity, 0.75F);
  }

  public WeakSet() {
    this(100);
  }

  private void purge() {
    Set<Map.Entry<Integer, WeakReference<T>>> set = hash.entrySet();
    set.removeIf(item -> item.getValue().get() == null);
  }

  public int size() {
    return hash.size();
  }

  public boolean isEmpty() {
    return hash.isEmpty();
  }

  public boolean add(T value) {
    purge();
    return hash.put(value.hashCode(), new WeakReference<>(value)) != null;
  }


  /**
   * @return The value to which this key was mapped, or <code>null</code> if there was no mapping for the key
   */

  public boolean remove(Object value) {
    try {
      return value != null && hash.remove(value.hashCode()) != null;
    } finally {
      //purge after since the removable above is likely to be the last reference to the object.
      purge();
    }
  }

  public void clear() {
    hash.clear();
  }


  @NotNull
  public Iterator<T> iterator() {

    return new Iterator<T>() {
      Iterator<Map.Entry<Integer, WeakReference<T>>> mapIterator = hash.entrySet().iterator();
      //to ensure a non-null return we make a strong reference from hasNext until next.
      T next = null;

      public boolean hasNext() {
        while (mapIterator.hasNext()) {
          Map.Entry<Integer, WeakReference<T>> nref = mapIterator.next();
          next = nref.getValue().get();
          if (next != null) {
            return true;
          }
        }
        return false;
      }

      public T next() {
        if ((next == null) && !hasNext()) {
          throw new NoSuchElementException();
        }
        try {
          return next;
        } finally {
          next = null;
        }
      }

      public void remove() {
        mapIterator.remove();
      }

    };
  }

}
