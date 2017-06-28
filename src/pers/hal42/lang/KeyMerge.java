package pers.hal42.lang;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Andy on 6/27/2017.
 * <p>
 * collect keys of nontrivial data, first use is to see if we have missed a bunch of refcons vs table columns allocated for them.
 */
public class KeyMerge<T> {
  //since hashset uses a hashmap we might as well use one directly and gather width info.
  public Map<String, T> keys = new HashMap<>(2000);

  public void merge(Map<String, T> more) {
    more.forEach((k, v) -> {
      if (ObjectX.NonTrivial(v)) {
        T extant = keys.get(k);
        if (extant == null) {
          keys.put(k, v);
        } else {
          if (v instanceof String) {
            if (sizeof(v) > sizeof(extant)) {
              keys.put(k, v);
            }
          }
        }
      }
    });
  }

  protected long sizeof(T item) {
    if (item instanceof String) {
      String s = (String) item;
      return s.length();
    }
    if (item instanceof Number) {
      Number number = (Number) item;
      return number.longValue();
    }
    //todo:2 add sizers as new types use this class.
    return 0;
  }

  public int size() {
    return keys.size();
  }
}
