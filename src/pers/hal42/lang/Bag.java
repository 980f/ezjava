package pers.hal42.lang;

import java.util.Hashtable;

/**
 * a bag of objects accessed by their toString()
 */
public class Bag<T> extends Hashtable<String, T> {
  public Bag(int initialCapacity) {
    super(initialCapacity);
  }

  public Bag() {
    super();
  }

  /**
   * replace or insert object, return previous one that had the same key
   */
  public Object put(T obj) {
    return super.put(obj.toString(), obj);
  }

  public T get(Object key) {
    return super.get(key.toString());
  }

}

