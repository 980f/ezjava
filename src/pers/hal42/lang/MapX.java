package pers.hal42.lang;

import pers.hal42.logging.ErrorLogStream;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Created by andyh on 7/19/17.
 * <p>
 * generates a map which is a description of the difference of two maps.
 */
public class MapX<K, V> {

  private static ErrorLogStream dbg = ErrorLogStream.getForClass(MapX.class);

  /**
   * @returns a list of the keys common to both maps.
   * todo:1 this must already exist in Collections or something standard.
   */
  public static <K, V> List<K> Intersection(Map<K, V> one, Map<K, V> other) {
    List<K> joint = new ArrayList<>(Math.min(one.size(), other.size()));
    for (K k : one.keySet()) {
      if (other.containsKey(k)) {
        joint.add(k);
      }
    }
    return joint;
  }

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
      Map<K, Diff<V>> diffs = thiss.getClass().newInstance();//#hack: same type of underlying map, java generics oddity.
      thiss.forEach((k, v) -> {
        V probate = that.get(k);
        if ((probate != null) ? !probate.equals(v) : (v != null)) {
          diffs.put(k, new Diff(v, probate));
        }
      });
      that.forEach((k, probate) -> {
        if (!thiss.containsKey(k)) {
          diffs.put(k, new Diff(null, probate));
        }
      });
      return diffs;
    } catch (InstantiationException | IllegalAccessException e) {
      dbg.Caught(e, "Diffing maps {0}-{1}", thiss, that);
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
