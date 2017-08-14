package pers.hal42.text;

import java.util.Iterator;

public class NamedValueIterator implements Iterator<NamedValue> {
  StringIterator source;

  public NamedValueIterator(StringIterator source) {
    this.source = source;
  }

  @Override
  public boolean hasNext() {
    return source.hasNext();
  }

  @Override
  public NamedValue next() {
    NamedValue wrapper = new NamedValue();
    wrapper.split(source.next(), '=');
    return wrapper;
  }
}
