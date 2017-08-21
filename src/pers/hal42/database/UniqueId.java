package pers.hal42.database;

import pers.hal42.lang.ReflectX;
import pers.hal42.lang.StringX;

/**
 * Description:  most database engines's idea of a serial number, uniqueness not enforced within this class
 */


public class UniqueId implements Comparable {

  //  private String KEY = ReflectX.justClassName(this);//can't be static! each class has to overload this
  protected static final long INVALID = ~0;//database engines's idea of an invalid serial number
  private long ivalue = INVALID;


  public UniqueId() {
    this(INVALID);
  }

  public UniqueId(long value) {
    setValue(value);
  }

  public UniqueId(String value) {
    setValue(value);
  }

  public boolean isValid() {
    return ivalue > 0; // anything less than 1 is invalid!
  }

  public String toString() {
    return String.valueOf(ivalue);
  }

  // These HAVE to be private so that people don't screw with them!  Causes problems otherwise!
  private UniqueId setValue(long value) {
    this.ivalue = value;
    if (!isValid()) {
      ivalue = INVALID;//only a single invalid valu allowed, at least until we have compare funcitons
    }
    return this;
  }

  private UniqueId setValue(String value) {
    return setValue(StringX.parseInt(value));
  }

  public long value() {
    return ivalue;
  }

  public boolean equals(UniqueId id2) {
    return compareTo(id2) == 0;
  }

  public boolean equals(Object o) {
    return o instanceof UniqueId && this.equals((UniqueId) o);
  }

  // for use as a key in a hashtable
  public int hashKey() {
    return (int) ivalue;
  }

  public int compareTo(Object obj) {//implements Comparable
    if (obj != null) {
      if (this.getClass() == obj.getClass()) { // +++ does this do the trick?
        return Long.compare(ivalue, ((UniqueId) obj).ivalue); //this.value- ((UniqueId)obj).value;
      } else {
        throw new ClassCastException("Comparing a " + ReflectX.shortClassName(this) + " to: " + obj);
      }
    } else {
      return 1; // this one is greater than null
    }
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
