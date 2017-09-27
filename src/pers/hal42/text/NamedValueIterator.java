package pers.hal42.text;

import pers.hal42.lang.StringX;

import java.util.Iterator;

/** extract steam of name:value pairs, skipping blanks and items starting with '#' */
public class NamedValueIterator implements Iterator<NamedValue> {
  protected StringIterator source;

  public NamedValueIterator(StringIterator source) {
    this.source = source;
    preview();
  }

  protected String lookahead;

  private void preview() {
    while (source.hasNext()) {
      lookahead = source.next();
      if (StringX.NonTrivial(lookahead) && !lookahead.startsWith("#")) { //vacuous or commented out item
        return;
      }
    }
    lookahead = null;
  }

  @Override
  public boolean hasNext() {
    return lookahead != null;
  }

  @Override
  public NamedValue next() {
    try {
      NamedValue wrapper = new NamedValue();
      wrapper.split(lookahead, '=');
      return wrapper;
    } finally {
      preview();
    }
  }
}
