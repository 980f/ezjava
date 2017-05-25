package pers.hal42.lang;

import java.util.Comparator;
import java.util.Vector;

public class OrderedVector<T> {
  Comparator<T> ordering;

  boolean unique = false;
  private Vector<T> storage;

  private OrderedVector(Comparator<T> ordering, int prealloc, boolean unique) {
    storage = new Vector<>(prealloc);
    this.ordering = ordering != null ? ordering : new NormalCompare();//swallow exceptions on ordering
    this.unique = unique;
  }

  public static <T> OrderedVector New(Comparator<T> ordering, boolean unique, int prealloc) {
    return new OrderedVector<>(ordering, prealloc, unique);
  }

  public static <T> OrderedVector New(Comparator<T> ordering, int prealloc) {
    return new OrderedVector<>(ordering, prealloc, false);
  }

  public static <T> OrderedVector New(Comparator<T> ordering, boolean unique) {
    return new OrderedVector<>(ordering, 0, unique);
  }

  public static <T> OrderedVector New(boolean unique) {
    return new OrderedVector<T>(null, 0, unique);
  }

  public static <T> OrderedVector New() {
    return new OrderedVector<T>(null, 0, false);
  }

  //deficiency in Java, can't ast an Object[] to a T[] array even if you know that it is. perhaps someone else can figure out how to do it.
  public synchronized Object[] snapshot() {
    return storage.toArray();
  }

  //NOT sync'ed. user must be robust against spontaneous changes!
  public int length() {
    return storage.size();
  }

  public synchronized boolean insert(T arf) {
    if (unique) {
      return VectorX.uniqueInsert(storage, arf, ordering);
    } else {
      VectorX.orderedInsert(storage, arf, ordering);
      return true;
    }
  }

  public synchronized Object itemAt(int i) {
    if (i >= 0 && i < storage.size()) {
      return storage.elementAt(i);
    } else {
      return null;
    }
  }

}

