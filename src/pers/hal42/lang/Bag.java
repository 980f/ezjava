package pers.hal42.lang;

import java.util.Hashtable;

public class Bag extends Hashtable {
  /**
   * replace or insert object, return previous one that had the same key
   */
  public Object put(Object obj){
    return super.put(obj.toString(),obj);
  }

  public Object get(Object key){
    return super.get(key.toString());
  }

  public Bag(int initialCapacity) {
    super(initialCapacity);
  }

  public Bag() {
    super();
  }

}

