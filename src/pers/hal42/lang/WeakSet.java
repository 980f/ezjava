package pers.hal42.lang;

import  java.util.*; // +++ optimize
import  java.lang.ref.WeakReference;
import  java.lang.ref.ReferenceQueue;

/**
 * +++ TODO: Propagate this to other classes that can/should use it:
 * TempFile, LogFile, FileZipper, LogSwitch, SyncMonitorList, but NOT PrintForks.
 */

/**
 * A <code>Set</code> implementation with <em>weak values</em>.
 * An entry in a <code>WeakSet</code> will automatically be removed when
 * it is no longer in ordinary use.  More precisely, the presence of an
 * entry will not prevent it from being discarded by the
 * garbage collector, that is, made finalizable, finalized, and then reclaimed.
 * When an entry has been discarded it is effectively removed from the set,
 * so this class behaves somewhat differently than other <code>Set</code>
 * implementations.
 *
 * <p> A single null values is supported.  This class has
 * performance characteristics similar to those of the <code>LinkedList</code>
 * class, and has the same efficiency parameters of <em>initial capacity</em>
 * and <em>load factor</em>.
 *
 * <p> Like most collection classes, this class is not synchronized.  A
 * synchronized <code>WeakHashMap</code> may be constructed using the
 * <code>Collections.synchronizedMap</code> method.
 *
 * <p> This class is intended primarily for use with key objects whose
 * <code>equals</code> methods test for object identity using the
 * <code>==</code> operator.  Once such a key is discarded it can never be
 * recreated, so it is impossible to do a lookup of that key in a
 * <code>WeakHashMap</code> at some later time and be surprised that its entry
 * has been removed.  This class will work perfectly well with key objects
 * whose <code>equals</code> methods are not based upon object identity, such
 * as <code>String</code> instances.  With such recreatable key objects,
 * however, the automatic removal of <code>WeakHashMap</code> entries whose
 * keys have been discarded may prove to be confusing.
 *
 * <p> The behavior of the <code>WeakHashMap</code> class depends in part upon
 * the actions of the garbage collector, so several familiar (though not
 * required) <code>Map</code> invariants do not hold for this class.  Because
 * the garbage collector may discard keys at any time, a
 * <code>WeakHashMap</code> may behave as though an unknown thread is silently
 * removing entries.  In particular, even if you synchronize on a
 * <code>WeakHashMap</code> instance and invoke none of its mutator methods, it
 * is possible for the <code>size</code> method to return smaller values over
 * time, for the <code>isEmpty</code> method to return <code>false</code> and
 * then <code>true</code>, for the <code>containsKey</code> method to return
 * <code>true</code> and later <code>false</code> for a given key, for the
 * <code>get</code> method to return a value for a given key but later return
 * <code>null</code>, for the <code>put</code> method to return
 * <code>null</code> and the <code>remove</code> method to return
 * <code>false</code> for a key that previously appeared to be in the map, and
 * for successive examinations of the key set, the value set, and the entry set
 * to yield successively smaller numbers of elements.
 *
 * <p> Each key object in a <code>WeakHashMap</code> is stored indirectly as
 * the referent of a weak reference.  Therefore a key will automatically be
 * removed only after the weak references to it, both inside and outside of the
 * map, have been cleared by the garbage collector.
 *
 * <p> <strong>Implementation note:</strong> The value objects in a
 * <code>WeakHashMap</code> are held by ordinary strong references.  Thus care
 * should be taken to ensure that value objects do not strongly refer to their
 * own keys, either directly or indirectly, since that will prevent the keys
 * from being discarded.  Note that a value object may refer indirectly to its
 * key via the <code>WeakHashMap</code> itself; that is, a value object may
 * strongly refer to some other key object whose associated value object, in
 * turn, strongly refers to the key of the first value object.  This problem
 * may be fixed in a future release.
 *
 * @version	1.12, 02/02/00
 * @author	Mark Reinhold
 * @since	1.2
 * @see		java.util.HashMap
 * @see		java.lang.ref.WeakReference
 */

public class WeakSet<T> extends AbstractSet<T> {

  Set<T> hash = null;

  /* Reference queue for cleared WeakObject */
  private ReferenceQueue queue = new ReferenceQueue();

  /* Remove all invalidated entries from the map, that is, remove all entries whose keys have been discarded.
     This method should be invoked once by each public mutator in this class.
     We don't invoke this method in public accessors because that can lead to surprising ConcurrentModificationExceptions. */
  private void processQueue() {
    WeakObject<T> wk;
    while ((wk = (WeakObject)queue.poll()) != null) {
      hash.remove(wk);
    }
  }



  /* -- Constructors -- */

  /**
    * Constructs a new, empty <code>WeakHashMap</code> with the given
    * initial capacity and the given load factor.
    *
    * @param  initialCapacity  The initial capacity of the
    *                          <code>WeakHashMap</code>
    *
    * @param  loadFactor       The load factor of the <code>WeakHashMap</code>
    *
    * @throws IllegalArgumentException  If the initial capacity is less than
    *                                   zero, or if the load factor is
    *                                   nonpositive
    */
  public WeakSet(int initialCapacity, float loadFactor) {
    hash = Collections.synchronizedSet(new HashSet(initialCapacity, loadFactor));
  }

  /**
     * Constructs a new, empty <code>WeakHashMap</code> with the given
     * initial capacity and the default load factor, which is
     * <code>0.75</code>.
     *
     * @param  initialCapacity  The initial capacity of the
     *                          <code>WeakHashMap</code>
     *
     * @throws IllegalArgumentException  If the initial capacity is less than
     *                                   zero
     */
  public WeakSet(int initialCapacity) {
    hash = Collections.synchronizedSet(new HashSet(initialCapacity));
  }

    /**
     * Constructs a new, empty <code>WeakHashMap</code> with the default
     * initial capacity and the default load factor, which is
     * <code>0.75</code>.
     */
  public WeakSet() {
    hash = Collections.synchronizedSet(new HashSet());
  }

  /**
     * Constructs a new <code>WeakHashMap</code> with the same mappings as the
     * specified <tt>Map</tt>.  The <code>WeakHashMap</code> is created with an
     * initial capacity of twice the number of mappings in the specified map
     * or 11 (whichever is greater), and a default load factor, which is
     * <tt>0.75</tt>.
     *
     * @param   t the map whose mappings are to be placed in this map.
     * @since	1.3
     */
  public WeakSet(WeakSet t) {
    this(Math.max(2*t.size(), 11), 0.75f);
    addAll(t);
  }


  /* -- Simple queries -- */

  /**
     * Returns the number of key-value mappings in this map.
     * <strong>Note:</strong> <em>In contrast with most implementations of the
     * <code>Map</code> interface, the time required by this operation is
     * linear in the size of the map.</em>
     */
  public int size() {
    return hash.size();
  }

    /**
     * Returns <code>true</code> if this map contains no key-value mappings.
     */
  public boolean isEmpty() {
    return hash.isEmpty();
  }

    /* -- Lookup and modification operations -- */

    /**
     * Updates this map so that the given <code>key</code> maps to the given
     * <code>value</code>.  If the map previously contained a mapping for
     * <code>key</code> then that mapping is replaced and the previous value is
     * returned.
     *
     * @param  key    The key that is to be mapped to the given
     *                <code>value</code>
     * @param  value  The value to which the given <code>key</code> is to be
     *                mapped
     *
     * @return  The previous value to which this key was mapped, or
     *          <code>null</code> if if there was no mapping for the key
     */
  public boolean add(Object value) {
    processQueue();
    return hash.add(WeakObject.create(value, queue));
  }

    /**
     * Removes the mapping for the given <code>key</code> from this map, if
     * present.
     *
     * @param  key  The key whose mapping is to be removed
     *
     * @return  The value to which this key was mapped, or <code>null</code> if
     *          there was no mapping for the key
     */
  public boolean remove(Object value) {
    processQueue();
    boolean success = false;
    if(value != null) {
      // iterate and find the one that contains it
      for(Iterator i = hash.iterator(); i.hasNext();) {
        WeakObject wo = (WeakObject)i.next();
        if((wo != null) && value.equals(wo.get())) {
          success = hash.remove(wo);
          break;
        }
      }
    }
    return success;
  }

    /**
     * Removes all mappings from this map.
     */
  public void clear() {
    processQueue();
    hash.clear();
  }



  /* -- Views -- */
  public Iterator iterator() {

    return new Iterator() {
      Iterator hashIterator = hash.iterator();
      Object next = null;

      public boolean hasNext() {
        while (hashIterator.hasNext()) {
          WeakObject ent = (WeakObject)hashIterator.next();
          Object k = ent.get();
          if (k == null) {
            /* Weak key has been cleared by GC */
            continue;
          }
          next = k;
          return true;
        }
        return false;
      }

      public Object next() {
        if ((next == null) && !hasNext()) {
          throw new NoSuchElementException();
        }
        Object e = next;
        next = null;
        return e;
      }

      public void remove() {
        hashIterator.remove();
      }

   };
  }

}
