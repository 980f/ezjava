package pers.hal42.text;

import pers.hal42.lang.StringX;
import pers.hal42.math.MathX;

import java.util.Enumeration;
import java.util.NoSuchElementException;

public class TextListIterator implements Enumeration {
  public String onError = "";//#do NOT put 'null' here, too many places expect non-null.
  private TextList tl;
  private int startIndex; //for rollbacks
  private int index; //next field to return
  private boolean ended = true;

  ////////////////////
  private TextListIterator(TextList tl, int startIndex, String onError) {
    this.tl = tl;
    this.onError = onError;
    setIndex(this.startIndex = startIndex);
  }

  public boolean eof() {
    return ended;
  }

  private String onerr() {
    ended = true;
    return onError;
  }

  public String lookAhead() {
    try {
      return ended ? onerr() : tl.itemAt(index);
    } catch (Exception ex) {
      return onerr();
    }
  }

  public String next() {
    try {
      return lookAhead();
    } finally {
      ++index;
    }
  }

  /**
   * beware: the default return is a function of onError string
   */
  public int nextInt() {
    return StringX.parseInt(next());
  }

  /**
   * @return number of fields remaining, possibly 0.
   * if struct is borked or been iterated off of the end then return is MathX.INVALIDINTEGER
   */
  public int remaining() {
    try {
      return tl.size() - index;
    } catch (Exception ex) {
      return MathX.INVALIDINTEGER;
    }
  }

  public boolean stillHas(int needed) {
    return remaining() >= needed;
  }

  private void setIndex(int newindex) {
    index = newindex;
    ended = index < 0 || tl == null || index >= tl.size();
  }

  public String rewind(int relative) {
    setIndex(index - relative);
    return lookAhead();
  }

  public String setto(int absolute) {
    setIndex(startIndex + absolute);
    return lookAhead();
  }

  /**
   * rewind to start of allocation,
   *
   * @return a peek at that field.
   */
  public String rewind() {//
    return setto(0);
  }

  public TextList TextList() {
    return tl;
  }

  public TextList restOfList() {
    try {
      return tl.tail(index);
    } finally {
      setIndex(tl.size());  //all done.
    }
  }

  public String tail(String comma) {
    return restOfList().asParagraph(comma);
  }

  public String tail(String open, String close) {
    return restOfList().asParagraph(open, close);
  }

  public String tail() {
    return restOfList().asParagraph(" ");//#don't trust textlist's default
  }

  /**
   * Tests if this enumeration contains more elements.
   *
   * @return <code>true</code> if and only if this enumeration object
   * contains at least one more element to provide;
   * <code>false</code> otherwise.
   */
  public boolean hasMoreElements() {
    return stillHas(1);
  }

  /**
   * Returns the next element of this enumeration if this enumeration
   * object has at least one more element to provide.
   *
   * @return the next element of this enumeration.
   * @throws NoSuchElementException if no more elements exist.
   */
  public Object nextElement() {
    if (!stillHas(1)) {
      throw new java.util.NoSuchElementException("TextListIterator");
    }
    return this.next();
  }

  /**
   * @return new made from @param tl starting at @param startIndex.
   * N.B.: a full rewind will gets you to starting point of textlist, not to where list was one
   * this object was created.
   */
  public static TextListIterator New(TextList tl, int startIndex, String onError) {
    return new TextListIterator(tl, startIndex, onError);
  }

  /**
   * @return new made from existing list, starting at @param startIndex.
   * N.B.: a full rewind will gets you to starting point of textlist, not to where list was one
   * this object was created.
   */

  public static TextListIterator New(TextList tl, int startIndex) {
    return new TextListIterator(tl, startIndex, "");
  }

  public static TextListIterator New(TextList tl, String onError) {
    return new TextListIterator(tl, 0, onError);
  }
/////////////////////
// Enumeration Interface {

  public static TextListIterator New(TextList tl) {
    return new TextListIterator(tl, 0, "");
  }

  /**
   * @return new made from command line args.
   */
  public static TextListIterator New(String[] args) {
    return New(TextList.CreateFrom(args));
  }
//}
/////

}

