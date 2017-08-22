package pers.hal42.lang;

/** not ready to import org.apache.commons ... or deal with teh overhead of AtomicBoolean */
public class MutableBoolean {
  public static final MutableBoolean True = new MutableBoolean(true);
  public static final MutableBoolean False = new MutableBoolean(false);
  public boolean value;

  public MutableBoolean(boolean value) {
    this.value = value;
  }

  public MutableBoolean(MutableBoolean other) {
    this.value = other != null && other.value;
  }

  public void set(boolean newvalue) {
    this.value = newvalue;
  }
}
