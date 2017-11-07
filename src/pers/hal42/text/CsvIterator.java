package pers.hal42.text;

import pers.hal42.ext.Span;
import pers.hal42.lang.StringX;

/**
 * Created by Andy on 6/7/2017.
 * <p>
 * cute incremental csv parser.
 * Does not yet deal with quoting or escaping etc. for that you will need a serious parser.
 */
public class CsvIterator implements StringIterator {
  private final boolean inReverse;
  private final char comma;
  protected String line;
  Span span = new Span();

  public CsvIterator(boolean inReverse, char comma, String line) {
    this.inReverse = inReverse;
    this.comma = comma;
    this.line = StringX.TrivialDefault(line, "");
    span.highest = this.line.length();
    span.lowest = 0;
    bump();
  }

  public CsvIterator(String line) {
    this(false, ',', line);
  }

  @Override
  public boolean hasNext() {
    return line != null && span.nonTrivial();
  }

  @Override
  public String next() {
    try {
      return span.subString(line, inReverse ? ~1 : 1).trim();
    } finally {
      bump();
    }
  }

  public String peek() {
    return span.getString(line);
  }

  private void bump() {
    if (inReverse) {
      span.lowest = 1 + line.lastIndexOf(comma, span.highest - 1);
    } else {
      span.highest = line.indexOf(comma, span.lowest);
    }
  }

  /**
   * makes hasNext() return false, frees and resources
   */
  @Override
  public void discard() {
    span.clear();
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
