package pers.hal42.lang;

import pers.hal42.logging.ErrorLogStream;

import java.util.Map;

/**
 * Created by andyh on 7/19/17.
 *
 * generates a map which is a description of the difference of two maps.
 */
public class MapX<K, V> {

  private static ErrorLogStream dbg = ErrorLogStream.getForClass(MapX.class);

  public static class Diff<V> {
    public V thisValue;
    public V thatValue;

    public Diff(V thiss,V that){
      thisValue=thiss;
      thatValue=that;
    }

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
      that.forEach((k,v)->{
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

}
