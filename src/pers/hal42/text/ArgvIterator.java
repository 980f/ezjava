package pers.hal42.text;

/**
 * Created by Andy on 5/23/2017.
 *
 * at the moment this is just an array iterator, but will add 'if 1st char is '@' include file' logic.
 */
public class ArgvIterator implements StringIterator {
  public int argc;
  public String[] argv;

  public ArgvIterator(String[] argv, int argc) {
    this.argc = argc;
    this.argv = argv;
  }

  public ArgvIterator(String[] argv) {
    this(argv, 0);
  }

  public boolean hasNext() {
    return argc < argv.length;
  }

  public String next() {
    try {
      return argv[argc++];
    } catch (Exception e) {
      return null;
    }
  }
}
