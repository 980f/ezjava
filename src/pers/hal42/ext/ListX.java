package pers.hal42.ext;

import java.util.Iterator;
import java.util.function.Predicate;

import static pers.hal42.lang.Index.BadIndex;

public class ListX {
  public static <T> int indexOf(Iterator<T> listerator, Predicate<T> criterion) {
    int cocounter = 0;
    while (listerator.hasNext()) {
      final T next = listerator.next();
      if (criterion.test(next)) {
        return cocounter;
      } else {
        ++cocounter;
      }
    }
    return BadIndex;
  }
}
