package pers.hal42.lang;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;


/** view a vector as a set. */
public class VectorSetWrapper<T> implements Set<T> {
  public Vector<T> wrapped;

  public VectorSetWrapper(Vector<T> wrapped) {
    this.wrapped = wrapped;
  }

  @Override
  public int size() {
    return wrapped.size();
  }

  @Override
  public boolean isEmpty() {
    return wrapped.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return wrapped.contains(o);
  }

  @NotNull
  @Override
  public Iterator<T> iterator() {
    return wrapped.iterator();
  }

  @NotNull
  @Override
  public Object[] toArray() {
    return wrapped.toArray();
  }

  @NotNull
  @Override
  public <T1> T1[] toArray(@NotNull T1[] a) {
    return wrapped.toArray(a);
  }

  @Override
  public boolean add(T t) {
    if(wrapped.contains(t)){
      return false;
    }
    wrapped.insertElementAt(t,wrapped.size());
    return true;
  }

  @Override
  public boolean remove(Object o) {
    return wrapped.remove(o);
  }

  @Override
  public boolean containsAll(@NotNull Collection<?> c) {
    return wrapped.containsAll(c);
  }

  @Override
  public boolean addAll(@NotNull Collection<? extends T> c) {
    return wrapped.addAll(c);
  }

  @Override
  public boolean retainAll(@NotNull Collection<?> c) {
    return wrapped.retainAll(c);
  }

  @Override
  public boolean removeAll(@NotNull Collection<?> c) {
    return wrapped.removeAll(c);
  }

  @Override
  public void clear() {
    wrapped.clear();
  }
}
