package pers.hal42.text;

import pers.hal42.ext.Span;
import pers.hal42.lang.StringX;

/**
 * Created by Andy on 6/7/2017.
 * <p>
 * cute incremental csv parser.
 * Does not yet deal with quoting or escaping etc. for that you will need a serious parser.
 */
public class SimpleCsvIterator implements StringIterator {
  protected String line;
  final Span span = new Span();
  private final boolean inReverse;
  protected final char comma;

  public SimpleCsvIterator(boolean inReverse, char comma, String line) {
    this.inReverse = inReverse;
    this.comma = comma;
    reload(line);
  }

  /**
   * reuse object for new data but with same settings for parsing mode
   */
  public void reload(String line){
    this.line = StringX.TrivialDefault(line, "");
    span.highest = this.line.length();
    span.lowest = 0;
    bump();
  }

  public SimpleCsvIterator(String line) {
    this(false, ',', line);
  }

  @Override
  public boolean hasNext() {
    return line != null && inReverse ? span.hasEnded() : span.isStarted();//half open is good enough, it is the final element.
  }

  @Override
  public String next() {
    try {
      //the 2nd arg below alters the span to start after where it presently ends.
      return span.subString(line, inReverse ? ~1 : 1).trim();
    } finally {
      bump();
    }
  }

  /**
   * makes hasNext() return false, frees and resources
   */
  @Override
  public void discard() {
    span.clear();
  }

  public String peek() {
    return span.getString(line);
  }

  private void bump() {
    //this depends upon the leapfrog argument in the next():
    if (inReverse) {
      span.lowest = 1 + line.lastIndexOf(comma, span.highest - 1);
    } else {
      span.highest = line.indexOf(comma, span.lowest);
    }
  }

  //pending item through end of string
  public String tail() {
    return line.substring(span.lowest);
  }

  //start of string to pending item - separator
  public String head() {
    return StringX.subString(line, 0, span.lowest);
  }
}
