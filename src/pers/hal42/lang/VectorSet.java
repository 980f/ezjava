package pers.hal42.lang;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * java needs some handholding to realize that a Vector is also a Set.
 * The java standard library is like swiss cheese, full of little holes like this due to the fakery that is generics.
 * <p>
 * But then we can hand some functions that both leave out, like some observability on size changes. First use was a set that can be handed out for a Map implementation which tracks changes to the Key set it provides.
 */

public class VectorSet<T> extends Vector<T> implements Set<T> {
  /** called when items are added or removed */
  public interface StructureWatcher {
    /** called after removals, if @param is negative then a bunch were added starting with ~which. */
    void afterAdd(int which);

    /** called after removals, if @param is negative then a bunch were removed starting with ~which. */
    void afterRemove(int which);

    /** set() is being attempted, this can veto it */
    default boolean vetoReplace(int which) {
      return true;
    }
  }

  public List<StructureWatcher> watchers = new Vector<>();

  public VectorSet(int size) {
    super(size);
  }


  /**
   * Sets the size of this vector. If the new size is greater than the
   * current size, new {@code null} items are added to the end of
   * the vector. If the new size is less than the current size, all
   * components at index {@code newSize} and greater are discarded.
   *
   * @param newSize the new size of this vector
   * @throws ArrayIndexOutOfBoundsException if the new size is negative
   */
  @Override
  public synchronized void setSize(int newSize) {
    int oldsize = this.size();
    super.setSize(newSize);
    if (oldsize < newSize) {  //we added a bunch of nulls
      watchers.forEach(x -> x.afterAdd(~oldsize));
    }
  }

  /**
   * Deletes the component at the specified index.
   *
   * @param index the index of the object to remove
   * @throws ArrayIndexOutOfBoundsException if the index is out of range ({@code index < 0 || index >= size()})
   */
  @Override
  public synchronized void removeElementAt(int index) {
    super.removeElementAt(index);
    watchers.forEach(x -> x.afterRemove(index));
  }

  /**
   * Inserts the specified object as a component in this vector at the
   * specified {@code index}. Each component in this vector with
   * an index greater or equal to the specified {@code index} is
   * shifted upward to have an index one greater than the value it had
   * previously.
   * <p>
   * <p>The index must be a value greater than or equal to {@code 0}
   * and less than or equal to the current size of the vector. (If the
   * index is equal to the current size of the vector, the new element
   * is appended to the Vector.)
   * <p>
   * <p>This method is identical in functionality to the
   * {@link #add(int, Object) add(int, E)}
   * method (which is part of the {@link List} interface).  Note that the
   * {@code add} method reverses the order of the parameters, to more closely
   * match array usage.
   *
   * @param obj   the component to insert
   * @param index where to insert the new component
   * @throws ArrayIndexOutOfBoundsException if the index is out of range
   *                                        ({@code index < 0 || index > size()})
   */
  @Override
  public synchronized void insertElementAt(T obj, int index) {
    super.insertElementAt(obj, index);
    watchers.forEach(x -> x.afterAdd(index));
  }

  /**
   * Adds the specified component to the end of this vector,
   * increasing its size by one. The capacity of this vector is
   * increased if its size becomes greater than its capacity.
   * <p>
   * <p>This method is identical in functionality to the
   * {@link #add(Object) add(E)}
   * method (which is part of the {@link List} interface).
   *
   * @param obj the component to be added
   */
  @Override
  public synchronized void addElement(T obj) {
    super.addElement(obj);
    watchers.forEach(x -> x.afterAdd(size() - 1));   //index of last
  }

  /**
   * Removes the first (lowest-indexed) occurrence of the argument
   * from this vector. If the object is found in this vector, each
   * component in the vector with an index greater or equal to the
   * object's index is shifted downward to have an index one smaller
   * than the value it had previously.
   * <p>
   * <p>This method is identical in functionality to the
   * {@link #remove(Object)} method (which is part of the
   * {@link List} interface).
   *
   * @param obj the component to be removed
   * @return {@code true} if the argument was a component of this
   * vector; {@code false} otherwise.
   */
  @Override
  public synchronized boolean removeElement(Object obj) {
    int which = indexOf(obj);
    if (super.removeElement(obj)) {
      watchers.forEach(x -> x.afterRemove(which));
      return true;
    } else {
      return false;
    }
  }

  /**
   * Removes all components from this vector and sets its size to zero.
   * <p>
   * <p>This method is identical in functionality to the {@link #clear}
   * method (which is part of the {@link List} interface).
   */
  @Override
  public synchronized void removeAllElements() {
    super.removeAllElements();
    watchers.forEach(x -> x.afterRemove(~0));
  }

  /**
   * Replaces the element at the specified position in this Vector with the
   * specified element.
   *
   * @param index   index of the element to replace
   * @param element element to be stored at the specified position
   * @return the element previously at the specified position, but null if a watcher vetoes the setting.
   * @throws ArrayIndexOutOfBoundsException if the index is out of range
   *                                        ({@code index < 0 || index >= size()})
   * @since 1.2
   */
  @Override
  public synchronized T set(int index, T element) {
    boolean veto = watchers.stream().anyMatch(x -> x.vetoReplace(index));
    return veto ? null : super.set(index, element);
  }

  /**
   * Appends the specified element to the end of this Vector.
   *
   * @param t element to be appended to this Vector
   * @return {@code true} (as specified by {@link Collection#add})
   * @since 1.2
   */
  @Override
  public synchronized boolean add(T t) {
    super.add(t);
    watchers.forEach(x -> x.afterAdd(size()));
    return true;
  }

  /**
   * Inserts the specified element at the specified position in this Vector.
   * Shifts the element currently at that position (if any) and any
   * subsequent elements to the right (adds one to their indices).
   *
   * @param index   index at which the specified element is to be inserted
   * @param element element to be inserted
   * @throws ArrayIndexOutOfBoundsException if the index is out of range
   *                                        ({@code index < 0 || index > size()})
   * @since 1.2
   */
  @Override
  public void add(int index, T element) {
    super.add(index, element);
    watchers.forEach(x -> x.afterAdd(index));
  }

  /**
   * Removes the element at the specified position in this Vector.
   * Shifts any subsequent elements to the left (subtracts one from their
   * indices).  Returns the element that was removed from the Vector.
   *
   * @param index the index of the element to be removed
   * @return element that was removed
   * @throws ArrayIndexOutOfBoundsException if the index is out of range
   *                                        ({@code index < 0 || index >= size()})
   * @since 1.2
   */
  @Override
  public synchronized T remove(int index) {
    final T removed = super.remove(index);
    watchers.forEach(x -> x.afterRemove(index));
    return removed;
  }
}
