package pers.hal42.data;

import pers.hal42.lang.ObjectX;
import pers.hal42.lang.ReflectX;
import pers.hal42.lang.StringX;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.transport.EasyCursor;
import pers.hal42.transport.isEasy;


/**
 * an interval of generic type
 * todo:0 use generics! this class predates those!
 */
public class ObjectRange<T extends Comparable<T>> implements isEasy {
  protected Comparable one;
  protected Comparable two;
  private boolean singular;
  private boolean broad;
  private boolean sorted = false;
  private boolean isDirty = true; //defer analyze
  //isEasy:
  protected final static String oneKey = "one";
  protected final static String twoKey = "two";
  protected final static String sortedKey = "sorted";
  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(ObjectRange.class);

  protected ObjectRange(boolean sorted) {
    isDirty = true;
    this.sorted = sorted;
  }

  public ObjectRange() {//default is to be sorted.
    this(true);
  }

  protected ObjectRange(String one, String two, boolean sorted) {
    this(sorted);
    setBoth(one, two);
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

  public final Object one() {
    if (isDirty) {
      analyze();
    }
    return one;
  }

  public String twoImage() {
    return StringX.TrivialDefault(two());
  }

  public final Object two() {
    if (isDirty) {
      analyze();
    }
    return two;
  }

  public Comparable filter(String input) {
    return input;
  }

  public ObjectRange setOne(Comparable input) {
    dbg.VERBOSE("setOne():" + input);
    one = input;
    isDirty = true;
    return this;
  }

  public ObjectRange setTwo(Comparable input) {
    dbg.VERBOSE("setTwo():" + input);
    two = input;
    isDirty = true;
    return this;
  }

  public ObjectRange setOne(String input) {
    return setOne(filter(input));
  }

  public ObjectRange setTwo(String input) {
    return setTwo(filter(input));
  }

  public ObjectRange setBoth(Comparable oner, Comparable twoer) {
    return setOne(oner).setTwo(twoer);
  }

  public ObjectRange setBoth(String oner, String twoer) {
    return setOne(oner).setTwo(twoer);
  }

  public boolean NonTrivial() {
    if (isDirty) {
      analyze();
    }
    boolean ret = singular || broad;
    dbg.VERBOSE("" + (singular ? "singular " : "") + (broad ? "broad" : "") + (ret ? ": " + toString() : ""));
    return ret;
  }

  protected ObjectRange swap() {
    dbg.VERBOSE("swapping");
    Comparable exchange = one;
    one = two;
    two = exchange;
    return this;
  }

  protected ObjectRange sort() {
    dbg.VERBOSE("SORTING[" + (broad ? "" : "NOT") + "broad]: one[" + ReflectX.shortClassName(one) + "]=" + one + ", two[" + ReflectX.shortClassName(two) + "]=" + two);
    if (broad) {
      int comp = two.compareTo(one);
      boolean shouldswap = (comp < 0);
      dbg.VERBOSE("two.compareTo(one)=" + comp + ", so comp<0=" + shouldswap);
      dbg.VERBOSE("fyi: one.compareTo(two)=" + one.compareTo(two));
      if (shouldswap) {
        swap();
      }
    }
    dbg.VERBOSE("SORTED: one=" + one + ", two=" + two);
    return this;
  }

  /**
   * must be called anytime either string has changed
   */
  protected void analyze() {
    dbg.Enter("analyze");
    try {
      //temporarily make assignments, then make sense of them
      broad = ObjectX.NonTrivial(two);
      singular = ObjectX.NonTrivial(one);
      dbg.VERBOSE("NonTrivials: " + singular + " " + broad);
      //if two is nonTrivial
      if (broad) {
        if (!singular || one.compareTo(two) == 0) {//and either one is trivial or the same as two
          dbg.VERBOSE("is actually singular");
          one = two;
          two = null;
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
    } finally {
      dbg.Exit();
    }
  }

  public void copyMembers(ObjectRange rhs) {
    dbg.VERBOSE("copyMembers from " + ReflectX.shortClassName(rhs) + " to this " + ReflectX.shortClassName(this));
    //can this work? if(rhs.getClass()==this.getClass())
    {
      sorted = rhs.sorted;
      setOne(rhs.one);
      setTwo(rhs.two);
    }
  }

  public void save(EasyCursor ezp) {
    ezp.setString(oneKey, oneImage());
    ezp.setString(twoKey, twoImage());
    ezp.setBoolean(sortedKey, sorted);
  }

  public void load(EasyCursor ezp) {
    sorted = ezp.getBoolean(sortedKey, false); // do this one first (affects the rest) !!!
    setBoth(ezp.getString(oneKey, null), ezp.getString(twoKey, null));
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
//$Id: ObjectRange.java,v 1.17 2004/03/22 17:44:17 andyh Exp $
