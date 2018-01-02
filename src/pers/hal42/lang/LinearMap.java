package pers.hal42.lang;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * a Map for really small sets, uses linear search for key or value.
 * IE this is an array of pairs made to look like a map.
 * <p>
 * The entrySet() method is expensive, keySet() and values() are not.
 * you can get the key for a value by calling getKey(value).
 * all iterators step in the same order
 */

public class LinearMap<K, V> implements Map<K, V>, VectorSet.StructureWatcher {
  @NotNull
  @Override
  public Set<Entry<K, V>> entrySet() {
    /* must create new storage */
    VectorSet<Entry<K, V>> gross = new VectorSet<>(size());
    for (int i = size(); i-- > 0; ) {//#order matches key iterator
      gross.addElement(new SimpleEntry<>(keys.get(i), values.get(i)));
    }
    return gross;
  }

  protected VectorSet<K> keys;
  protected Vector<V> values;

  public LinearMap() {
    this(10);
  }

  public LinearMap(int initialCapacity) {
    keys = new VectorSet<>(initialCapacity);
    values = new Vector<>(initialCapacity);
    keys.watchers.add(this);
  }

  @Override
  public int size() {
    return keys.size();
  }

  @Override
  public boolean isEmpty() {
    return keys.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    //noinspection SuspiciousMethodCalls
    return keys.contains(key);
  }

  @Override
  public boolean containsValue(Object value) {
    //noinspection SuspiciousMethodCalls
    return values.contains(value);
  }

  @Override
  public V get(Object key) {
    @SuppressWarnings("unchecked")
    int which = ordinal((K) key);
    return which >= 0 ? values.get(which) : null;
  }

  @Override
  public V put(K key, V value) {
    V previous = get(key);
    int which = ordinal(key);
    if (which >= 0) {
      values.set(which, value);
    } else {
      keys.addElement(key); //creates a null entry for the value
//done by watcher: values.setSize(keys.size())
      values.set(keys.size() - 1, value);
    }
    return previous;
  }

  @Override
  public V remove(Object key) {
    @SuppressWarnings("unchecked")
    int which = ordinal((K) key);
    if (which >= 0) {
      V dud = values.get(which);
      keys.remove(which);  //which removes via afterRemove()
      return dud;
    } else {
      return null;
    }
  }

  @Override
  public void putAll(@NotNull Map<? extends K, ? extends V> m) {
    m.forEach(this::put);
  }

  @Override
  public void clear() {
    keys.clear();
    values.clear();
  }

  @NotNull
  @Override
  public Set<K> keySet() {
    return keys;
  }

  @NotNull
  @Override
  public Collection<V> values() {
    return values;
  }

  class EntryIterator implements Iterator<Entry<K, V>> {
    private int pointer = keys.size(); //#order matches set iterator

    /**
     * Returns {@code true} if the iteration has more elements.
     * (In other words, returns {@code true} if {@link #next} would
     * return an element rather than throwing an exception.)
     *
     * @return {@code true} if the iteration has more elements
     */
    @Override
    public boolean hasNext() {
      return pointer >= 0;
    }

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration
     * @throws NoSuchElementException if the iteration has no more elements
     */
    @Override
    public Entry<K, V> next() {
      if (hasNext()) {
        try {
          return new SimpleEntry<>(keys.get(pointer), values.get(pointer));
        } finally {
          ++pointer;
        }
      } else {
        throw new NoSuchElementException("past end of LinearMap Entry iteration");
      }
    }
  }

  public int ordinal(K key) {
    return keys.indexOf(key);
  }

  public int indexOf(V value) {
    return values.indexOf(value);
  }

  public K getKey(V value) {
    int which = indexOf(value);
    if (which >= 0) {
      return keys.get(which);
    } else {
      return null;
    }
  }

  /** this may be faster than calling entrySet then using its iterator, although if you are going to create multiple iterator then entrySet().iterator() is the way to go. */
  public Iterator<Entry<K, V>> entryIterator() {
    return new EntryIterator();
  }

  @Override
  public void afterAdd(int which) {
    values.setSize(keys.size());
  }

  @Override
  public void afterRemove(int which) {
    if (which >= 0) { //single add
      values.remove(which);
    } else {
      values.setSize(keys.size());
    }
  }

  /**
   * veto all attempts to replace a key.
   *
   * @param which is ignored in this instance.
   */
  @Override
  public boolean vetoReplace(int which) {
    return true;
  }
}
