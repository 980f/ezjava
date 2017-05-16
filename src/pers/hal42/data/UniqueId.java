package pers.hal42.data;


import pers.hal42.lang.ReflectX;
import pers.hal42.lang.StringX;
import pers.hal42.transport.EasyCursor;
import pers.hal42.transport.isEasy;

/**
 * most database engines's idea of a serial number, uniqueness not enforced within this class
 */
public class UniqueId implements isEasy, Comparable {

  private Integer ivalue = INVALIDINTEGER;
  private String KEY = ReflectX.justClassName(this);//can't be static! each class has to overload this
  protected static final int INVALID = -1;//database engines's idea of an invalid serial number
  //  private /*---*/ int value = INVALID;
  protected static final Integer INVALIDINTEGER = INVALID;

  public UniqueId() {
    this(INVALID);
  }

  public UniqueId(int value) {
    setValue(value);
  }

  public UniqueId(String value) {
    setValue(value);
  }

  public boolean isValid() {
    return ivalue > 0; // anything less than 1 is invalid!
  }

  public final String makeMutexName() { // handy tool for making names for mutexes.
    return KEY + "." + ivalue;
  }

  public String toString() {//+_+ rename Image()
    return ivalue.toString();
  }

  // These HAVE to be private so that people don't screw with them!  Causes problems otherwise!
  private UniqueId setValue(int value) {
    this.ivalue = value;
    if (!isValid()) {
      ivalue = INVALIDINTEGER;//only a single invalid valu allowed, at least until we have compare funcitons
    }
    return this;
  }

  private UniqueId setValue(String value) {
    return setValue(StringX.parseInt(value));
  }

  public int value() {
    return ivalue;
  }

  public boolean equals(UniqueId id2) {
    return compareTo(id2) == 0;
  }

  public boolean equals(Object o) {
    return o instanceof UniqueId && this.equals((UniqueId) o);
  }

  // for use as a key in a hashtable
  public Integer hashKey() {
    return ivalue;
  }

  public int compareTo(Object obj) {//implements Comparable
    if (obj != null) {
      if (this.getClass() == obj.getClass()) { // +++ does this do the trick?
        return this.ivalue.compareTo(((UniqueId) obj).ivalue); //this.value- ((UniqueId)obj).value;
      } else {
        throw new ClassCastException("Comparing a " + ReflectX.shortClassName(this) + " to: " + obj);
      }
    } else {
      return 1; // this one is greater than null
    }
  }

  public void save(EasyCursor ezp) {
    ezp.setInt(KEY, value());
  }

  /**
   * it is often nice to not have the field present if value is not known.
   * @return pass through of "field valid" check.
   */
  public boolean saveIfValid(EasyCursor ezp) {
    if (isValid()) {
      save(ezp);
      return true;
    } else {
      return false;
    }
  }

  public void load(EasyCursor ezp) {
    setValue(ezp.getInt(KEY));
  }

  public UniqueId Clear() {
    return setValue(INVALID);
  }

  public static boolean isValid(UniqueId id) {
    return id != null && id.isValid();
  }

  public static boolean equals(UniqueId id1, UniqueId id2) {
    if (id1 == null) {
      return id2 == null;
    } else {
      return id1.equals(id2);
    }
  }

}
