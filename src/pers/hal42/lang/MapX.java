package pers.hal42.lang;

import pers.hal42.logging.ErrorLogStream;

import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Created by andyh on 7/19/17.
 *
 * generates a map which is a description of the difference of two maps.
 */
public class MapX<K, V> {

  private static ErrorLogStream dbg = ErrorLogStream.getForClass(MapX.class);

  /**
   * remove diff records where 'thatValue' is null and 'thisValue' does NOT meet criterion @param keep
   */
  public static <K, V> int DropNullIfNot(Map<K, Diff<V>> diffs, Predicate<V> keep) {
    int startingSize = diffs.size();
    Iterator<K> it = diffs.keySet().iterator();
    while (it.hasNext()) {
      K k = it.next();
      Diff<V> dif = diffs.get(k);
      if ((dif.thatValue == null) && !keep.test(dif.thisValue)) {  //todo:1 add complementary case wrt who is null
        it.remove();
      }
    }
    return startingSize - diffs.size();
  }

  @SuppressWarnings("unchecked")
  public static <K, V> Map<K, Diff<V>> Diff(Map<K, V> thiss, Map<K, V> that) {

    try {
      Map<K, Diff<V>> diffs = thiss.getClass().newInstance();
      thiss.forEach((k, v) -> {
        V probate = that.get(k);
        if (probate != null ? !probate.equals(v):v !=null) {
          diffs.put(k, new Diff(v,probate));
        }
      });
      that.forEach((k, v)->{
        if(!thiss.containsKey(k)){
          diffs.put(k, new Diff(null,v));
        }
      });
      return diffs;
    } catch (InstantiationException | IllegalAccessException e) {
      dbg.Caught(e,"Diffing maps {0}-{1}",thiss,that);
      return null;
    }

  }

  public static class Diff<V> {
    public V thisValue;
    public V thatValue;

    public Diff(V thiss, V that) {
      thisValue = thiss;
      thatValue = that;
    }

    @Override
    public String toString() {
      if (thisValue == null) {
        return "New:" + thatValue;
      } else if (thatValue == null) {
        return "Lost:" + thisValue;
      } else {
        return MessageFormat.format("Change:{0}->{1}", thisValue, thatValue);
      }
    }
  }

}
