package pers.hal42.text;

public class AsciiIterator implements StringIterator {
  public final char base;
  protected int which;

  public AsciiIterator(char base) {
    this.base = base;
    which = 0;
  }

  public boolean hasNext() {
    return which < 128;
  }

  /**
   * @returns null if hasNext() returned false;
   */
  public String next() {
    return Ascii.computed(base, which++);
  }

  /** @returns what nth next() will return without advancing. */
  public String nth(int randomWhich) {
    return Ascii.computed(base, randomWhich);
  }

  /** @returns what @see next() will return, without advancing. */
  public String nth() {
    return Ascii.computed(base, which);
  }

  public String first() {
    return Ascii.computed(base, 0);
  }
}
