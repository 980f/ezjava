package pers.hal42.text;

import pers.hal42.ext.Span;

/**
 * Created by Andy on 6/7/2017.
 *
 * cute incremental csv parser
 */
public class CsvIterator implements StringIterator {

  protected String line;
  Span span=new Span();

  public CsvIterator(String line) {
    this.line = line;
    if (line.length() > 0) {
      span.lowest = 0;
      bump();
    }
  }

  @Override
  public boolean hasNext() {
    return line != null && span.isStarted();
  }

  @Override
  public String next() {
    try {
      return span.subString(line, 1).trim();
    } finally {
      bump();
    }
  }

  private void bump() {
    span.highest = line.indexOf(',', span.lowest);
  }

  /** makes hasNext() return false, frees and resources */
  @Override
  public void discard() {
    span.clear();
  }
}
