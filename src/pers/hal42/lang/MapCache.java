package pers.hal42.lang;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

// this class uses Integer keys and TrueEnum indexes into an array of a Map.
// it provides for synchronization for creation of the different Maps.
// might not be necessary, but, oh well.

public class MapCache {
  private Map [ ] caches = null;
  private Class classForArray = null;

  public MapCache(int fixedSize, Class classForArray) {
    caches = new Map[fixedSize];
    this.classForArray = classForArray;
  }
  public MapCache(Enum values, Class classForArray) {
    this(values.getClass().getEnumConstants().length, classForArray);
  }

  private synchronized Map getCache(Enum klass) {
    // first, get the correct cache
    Map cache = caches[klass.ordinal()];
    if(cache == null) {
      try {
        cache = (Map)classForArray.newInstance();
        caches[klass.ordinal()] = cache;
      } catch (Exception ex) {
        // ???
      }
    }
    return cache;
  }

  public synchronized void put(Integer key, Enum klass, Object o) {
    getCache(klass).put(key, o);
  }

  public synchronized Object get(Integer key, Enum klass) {
    return getCache(klass).get(key);
  }

  public synchronized Set keys(Enum klass) {
    return getCache(klass).keySet();
  }

  public int size(Enum klass) {
    return getCache(klass).size();
  }

  public int [] counts(Enum klass) {
    Enum [] kc=klass.getClass().getEnumConstants();

    int [ ] ret = new int[kc.length];
    for(int i = kc.length; i-->0;) {
      ret[i] = getCache(kc[i]).size();
    }
    return ret;
  }

  public static MapCache HashtableCache(Enum values) {
    return new HashtableCache(values);
  }

  public static MapCache SoftHashtableCache(Enum values) {
    return new SoftHashtableCache(values);
  }
}

class HashtableCache extends MapCache {
  public HashtableCache(Enum values) {
    super(Safe.enumSize(values), Hashtable.class);
  }
}

class SoftHashtableCache extends MapCache {
  public SoftHashtableCache(Enum values) {
    super(Safe.enumSize(values), SoftHashtable.class);
  }
}

