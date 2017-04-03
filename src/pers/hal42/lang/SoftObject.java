package pers.hal42.lang;


import java.lang.ref.SoftReference;
import java.lang.ref.ReferenceQueue;

public final class SoftObject<T> extends SoftReference<T> {
  private SoftObject(T k) {
    super(k);
  }

  private static <T> SoftObject create(T k) {
    return (k == null) ? null : new SoftObject<>(k);
  }

  private SoftObject(T k, ReferenceQueue<T> q) {
    super(k, q);
  }

  public static <T> SoftObject create(T k, ReferenceQueue<T> q) {
    return (k == null) ? null : new SoftObject<>(k, q);
  }

  /* A WeakObject is equal to another WeakObject iff they both refer to objects
    that are, in turn, equal according to their own equals methods */
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if ((o instanceof SoftObject)) {
      Object u = ((SoftObject) o).get();
      T t = this.get();
      return !((t == null) || (u == null)) && (t == u || t.equals(u));
    }

    return false;

  }
}

