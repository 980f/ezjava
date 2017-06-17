package pers.hal42.data;

import pers.hal42.lang.ObjectX;
import pers.hal42.lang.ReflectX;
import pers.hal42.lang.StringX;
import pers.hal42.logging.ErrorLogStream;

/**
 * an interval of generic type
 *
 * Properties interface has been commented out due to its interference with genericization
 */
public class ObjectRange<T extends Comparable<T>> {
  protected T one;
  protected T two;
  private boolean sorted = false;

  private boolean singular;
  private boolean broad;
  private boolean isDirty = true; //defer analyze

  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(ObjectRange.class);


  public ObjectRange(T one, T two, boolean sorted) {
    this.one = one;
    this.two = two;
    this.sorted = sorted;
  }

  protected ObjectRange(boolean sorted) {
    isDirty = true;
    this.sorted = sorted;
  }

  public ObjectRange() {//default is to be sorted.
    this(true);
  }


  public boolean singular() {
    if (isDirty) {
      analyze();
    }
    return singular;
  }

  public boolean broad() {
    if (isDirty) {
      analyze();
    }
    return broad;
  }

  public String oneImage() {
    return StringX.TrivialDefault(one());
  }

  public final T one() {
    if (isDirty) {
      analyze();
    }
    return one;
  }

  public String twoImage() {
    return StringX.TrivialDefault(two());
  }

  public final T two() {
    if (isDirty) {
      analyze();
    }
    return two;
  }

  public ObjectRange<T> setOne(T input) {
    dbg.VERBOSE("setOne():" + input);
    one = input;
    isDirty = true;
    return this;
  }

  public ObjectRange<T> setTwo(T input) {
    dbg.VERBOSE("setTwo():" + input);
    two = input;
    isDirty = true;
    return this;
  }

  public ObjectRange<T> setBoth(T oner, T twoer) {
    return setOne(oner).setTwo(twoer);
  }


  public boolean NonTrivial() {
    if (isDirty) {
      analyze();
    }
    return singular || broad;
  }

  /** @returns -1 if item is below range (<low), +1 if above range (>=high) , else 0 (inside halfOpen interval)
   * this method does NOT check whether this range is sane. Expect NPE's etc. if it is badly initialized */
  public int compare(T item){
    int cmplow=one.compareTo(item);
    if(cmplow<0){
      return -1;
    }
    int cmphigh=two.compareTo(item);
    if(cmphigh>=0){
      return 1;
    }
    return 0;
  }

  /** @returns whether @param item is in range, if @param overlapped then range includes the higher bound.
   * overlapped probably should be a member rather than a passed parameter. */
  public boolean contains(T item, boolean overlapped) {
    return compare(item) == 0 || overlapped && two.equals(item);
  }

  protected ObjectRange swap() {
    dbg.VERBOSE("swapping");
    T exchange = one;
    one = two;
    two = exchange;
    return this;
  }

  protected ObjectRange sort() {
    if (broad) {
      int comp = two.compareTo(one);
      boolean shouldswap = (comp < 0);
      if (shouldswap) {
        swap();
      }
    }
    return this;
  }

  /**
   * must be called anytime either string has changed
   */
  protected void analyze() {

    try (ErrorLogStream pop= dbg.Push("analyze")){
      //temporarily make assignments, then make sense of them
      broad = ObjectX.NonTrivial(two);
      singular = ObjectX.NonTrivial(one);
      dbg.VERBOSE("NonTrivials: {0} {1} ", singular ,broad);
      //if two is nonTrivial
      if (broad) {
        if (singular && !broad) {
          two = one;//copy so we can be sloppy elsewhere
        }
        if (!singular || one.compareTo(two) == 0) {//and either one is trivial or operationally the same as two
          dbg.VERBOSE("is actually singular");
          one = two;
          singular = true;
          broad = false;
        } else {
          singular = false;
        }
        if (sorted) {
          sort();
        }
      }
      isDirty = false;
    } catch (Exception e) {
      e.printStackTrace();//won't occur
    }
  }

  public void copyMembers(ObjectRange<T> rhs) {
    dbg.VERBOSE("copyMembers from " + ReflectX.shortClassName(rhs) + " to this " + ReflectX.shortClassName(this));
    //can this work? if(rhs.getClass()==this.getClass())
    {
      sorted = rhs.sorted;
      setOne(rhs.one);
      setTwo(rhs.two);
    }
  }

  public String toString() {
    return (sorted ? "" : "un") + "sorted: [" + one() + ", " + two() + "]";
  }

  public static boolean NonTrivial(ObjectRange pair) {
    return pair != null && pair.NonTrivial();
  }

  /* The rules for ObjectRange:
  <li> ObjectRange expects the objects to be comparable if sorted is true.
  You will get exceptions if they are not.

  <li> Safe has NonTrivial(Object o) which does an if-else on instanceof
  those classes for which we have a Safe.NonTrivial function.
  It returns !=null for unknown classes. ObjectRange relies upon this.
  Any object.NonTrivial functions should also have a StringX.NonTrivial()
  partner made for them, which checks null and then calls the object's NonTrivial.

  <li> Finally the objects used in a range should have a meaningful toString().

  */

}
