package pers.hal42.lang;

public class CountedBoolean {
  protected int count = 0;

  public CountedBoolean() {
    Reset();
  }

  public CountedBoolean(boolean enableit) {
    this();
    setto(enableit);
  }

  public boolean booleanValue() {//make us look like Boolean
    return count > 0;
  }

  public boolean set() {
    ++count;
    return booleanValue();
  }

  public boolean clr() {
    synchronized (this) {
      if (booleanValue()) {
        --count;
      }
    }
    return booleanValue();
  }

  public boolean setto(boolean enableit) {
    return enableit ? set() : clr();
  }

  public void Reset() {
    count = 0;
  }

}
