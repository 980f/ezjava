package pers.hal42.lang;

import java.lang.ref.WeakReference;
import java.lang.ref.ReferenceQueue;

public final class WeakObject<T> extends WeakReference<T> {
  private WeakObject(T k) {
    super(k);
  }
  private static <T> WeakObject<T> create(T k) {
    return (k == null) ? null : new WeakObject<>(k);
  }

  private WeakObject(T k, ReferenceQueue q) {
    super(k, q);
  }

  public static <T> WeakObject<T> create(T k, ReferenceQueue q) {
    return (k == null) ? null : new WeakObject<>(k, q);
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
    Object u = ((WeakObject)o).get(); //todo:0 relax this to accept non-weak T's
    if ((t == null) || (u == null)) {
      return false;
    }
    if (t == u) {
      return true;
    }
    return t.equals(u);
  }
}

