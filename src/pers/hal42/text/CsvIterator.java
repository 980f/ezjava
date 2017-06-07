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
  @Override
  public boolean hasNext() {
    return line!=null && span.isValid();
  }

  @Override
  public String next() {
    try {
      return span.subString(line, 0).trim();
    } finally {
      bump();
    }
  }

  private void bump() {
    span.highest=line.indexOf('=',span.lowest);
  }

  public CsvIterator(String line) {
    this.line = line;
    bump();
  }
}
