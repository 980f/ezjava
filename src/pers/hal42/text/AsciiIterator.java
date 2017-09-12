package pers.hal42.text;

public class AsciiIterator implements StringIterator {
  public final char base;
  protected int which;

  public AsciiIterator(char base) {
    this.base = base;
    which = 0;
  }

  /** @returns what nth next() will return without advancing. */
  public String nth(int randomWhich) {
    return Ascii.computed(base, randomWhich);
  }

  /** @returns first item regardless of present state of iteration */
  public String first() {
    return nth(0);
  }

  /** @returns what @see next() will return, without advancing. */
  public String peek() {
    return nth(which);
  }

  /** @returns whether */
  public boolean hasNext() {
    return (base + which) < 128;
  }

  /**
   * @returns null if hasNext() returned false;
   */
  public String next() {
    return nth(which++);
  }
}
