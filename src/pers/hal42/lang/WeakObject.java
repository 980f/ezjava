package pers.hal42.lang;

import java.lang.ref.WeakReference;
import java.lang.ref.ReferenceQueue;

public final class WeakObject extends WeakReference {
  private WeakObject(Object k) {
    super(k);
  }
  private static WeakObject create(Object k) {
    return (k == null) ? null : new WeakObject(k);
  }
  private WeakObject(Object k, ReferenceQueue q) {
    super(k, q);
  }
  public static WeakObject create(Object k, ReferenceQueue q) {
    return (k == null) ? null : new WeakObject(k, q);
  }
  /* A WeakObject is equal to another WeakObject iff they both refer to objects
  that are, in turn, equal according to their own equals methods */
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof WeakObject)) {
      return false;
    }
    Object t = this.get();
    Object u = ((WeakObject)o).get();
    if ((t == null) || (u == null)) {
      return false;
    }
    if (t == u) {
      return true;
    }
    return t.equals(u);
  }
}

