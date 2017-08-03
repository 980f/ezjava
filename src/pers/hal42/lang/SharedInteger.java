package pers.hal42.lang;

/**
 * Created by Andy on 8/3/2017.
 * <p>
 * a modifiable integer that can be passed around.
 * <p>
 * There are such classes elsewhere but I can't for the life of me remember where they are.
 */
public class SharedInteger {
  int raw;

  public SharedInteger(int raw) {
    this.raw = raw;
  }

  public SharedInteger() {
    this(0);
  }

  public int next() {
    return ++raw;
  }

  public int raw() {
    return raw;
  }

  @Override
  public String toString() {//4debug
    return String.valueOf(raw);
  }
}
