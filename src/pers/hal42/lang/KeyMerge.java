package pers.hal42.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Andy on 6/27/2017.
 * <p>
 * collect keys of nontrivial data, first use is to see if we have missed a bunch of refcons vs table columns allocated for them.
 */
public class KeyMerge<T> {
  List<String> keys = new ArrayList<>(2000);

  public void merge(Map<String, T> more) {
    more.forEach((k, v) -> {
      if (ObjectX.NonTrivial(v)) {
        if (!keys.contains(k)) {
          keys.add(k);
        }
      }
    });
  }
}
