package pers.hal42.lang;

/**
 * Created by Andy on 8/3/2017.
 * <p>
 * a fully modifiable integer that can be passed around.
 * <p>
 * There are such classes elsewhere but I can't for the life of me remember where they are.
 * Often Index will do, it prevents wrapping which is usually a good thing.
 */
public class SharedInteger {
  public int raw;

  public SharedInteger(int raw) {
    this.raw = raw;
  }

  public SharedInteger() {
    this(0);
  }

  public int next() {
    return ++raw;
  }

  public int postinc() {
    try {
      return raw;
    } finally {
      ++raw;
    }
  }

  @Override
  public String toString() {//4debug
    return String.valueOf(raw);
  }
}
