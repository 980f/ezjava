package pers.hal42.text;

import pers.hal42.lang.ReflectX;
import pers.hal42.lang.Safe;
import pers.hal42.lang.VectorX;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.logging.LogLevelEnum;
import pers.hal42.transport.EasyCursor;
import pers.hal42.transport.isEasy;

import java.util.Vector;

public class FormattedLines implements isEasy {
  protected char fillChar = ' '; //for converting raw strings
  Vector<FormattedLineItem> storage = new Vector<>();
  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(FormattedLines.class, LogLevelEnum.VERBOSE);
  private static final String countKey = "count";

  public FormattedLines() {
    storage = new Vector<>();
  }

  public FormattedLines(Object arf) {
    this();
    add(arf);
  }

  public FormattedLines(int initialCapacity) {
    storage = new Vector<>(initialCapacity);
  }

  public int size() {
    return storage != null ? storage.size() : 0;
  }

  public boolean NonTrivial() {
    return size() > 0;
  }

  public FormattedLines reverse() {
    VectorX.reverse(storage);
    return this;
  }

  /**
   * convert to strings now that we know the @param width of the presentation device
   *
   * @param paragraph is added to if it exists, else a new one is created.
   */
  public TextList formatted(int width, TextList paragraph) {
    if (paragraph == null) {
      paragraph = new TextList(storage.size());
    }
    for (int i = 0; i < storage.size(); i++) {//#preserve order
      paragraph.add(itemAt(i).formatted(width));
    }
    return paragraph;
  }

  /**
   * convert to strings now that we know the @param width of the presentation device
   */
  public TextList formatted(int width) {
    return formatted(width, null);
  }

  public boolean add(Object arf) {
    if (arf == null) {
      return true;//successfully added nothing
    }
    if (arf instanceof FormattedLineItem) {
      return storage.add((FormattedLineItem) arf);
    }
    if (arf instanceof String) {
      return storage.add(new FormattedLineItem((String) arf, fillChar));
    }
    if (arf instanceof TextList) {//+_+could expedite this frequently used case
      add(((TextList) arf).Vector());
    }
    if (arf instanceof Vector) {//this includes FormattedLines itself.
      //failed to call back this class// return super.addAll((Vector)arf);
      Vector victor = (Vector) arf;//cast
      int count = victor.size();
      for (int i = 0; i < count; i++) {//retain order
        if (!add(victor.elementAt(i))) {
          //do we try the rest?
          //no: quit on any error-for debug reasons
          return false;
        }
      }
      return true;
    }
    //could throw unsupportedCast, but we are too nice for that
    return false;//didan't add it!
  }

  public boolean add(String left, String right) {
    return add(FormattedLineItem.pair(left, right));
  }

  public FormattedLineItem itemAt(int index) {
    return (index < storage.size()) ? storage.elementAt(index) : null;
  }

  /* isEasy and other transport related items */
  public EasyCursor asProperties() {
    EasyCursor ezp = new EasyCursor();
    save(ezp);
    return ezp;
  }

  public void save(EasyCursor ezc) {
    ezc.setVector(storage);
  }
///////////////////////////////////////////////
// this had to go somewhere... here for when we can do two columns

  public void load(EasyCursor ezc) {
    storage = ezc.getVector(FormattedLineItem.class);
  }

  public static int Sizeof(FormattedLines probate) {
    return probate != null ? probate.size() : 0;
  }

  public static FormattedLines Empty() {
    return new FormattedLines();
  }

  /**
   * @param te value will get destroyed! it is for getting to the underlying class
   * @deprecated untested
   */
  public static <T extends Enum<T>> FormattedLines menuListing(T te, Class<? extends Enum> pool) {
    if (te == null) {
      return Empty();
    }
    int size = Safe.enumSize(pool);
    FormattedLines newone = new FormattedLines(size + 1);
    newone.add(ReflectX.justClassName(pool), te.toString());
    for (int i = 0; i < size; i++) {
      newone.add(pool.getEnumConstants()[i].toString());
    }
    return newone;
  }

}

