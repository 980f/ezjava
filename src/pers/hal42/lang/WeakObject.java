package pers.hal42.lang;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/** this class is probably obsolete in java 8. genericizing it was a waste of time. */
public final class WeakObject<T> extends WeakReference<T> {
  private WeakObject(T k) {
    super(k);
  }

  private WeakObject(T k, ReferenceQueue<T> q) {
    super(k, q);
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
    T t = this.get();
    Object u = ((WeakObject) o).get(); //todo:0 relax this to accept non-weak T's
    if ((t == null) || (u == null)) {
      return false;
    }
    //test for same actual object, expedite the compare
    return t == u || t.equals(u);
  }

  private static <T> WeakObject<T> create(T k) {
    return (k == null) ? null : new WeakObject<>(k);
  }

  public static <T> WeakObject<T> create(T k, ReferenceQueue<T> q) {
    return (k == null) ? null : new WeakObject<>(k, q);
  }
}

