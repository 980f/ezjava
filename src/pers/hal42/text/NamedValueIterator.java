package pers.hal42.text;

import pers.hal42.lang.StringX;

import java.util.Iterator;

/** extract steam of name:value pairs, skipping blanks and items starting with '#' */
public class NamedValueIterator implements Iterator<CommandArgument> {
  protected StringIterator source;
  protected char cutter;
  protected transient String lookahead;
  CommandArgument shared;
  private final boolean sharedReturn;

  public NamedValueIterator(StringIterator source, char cutter, boolean sharedReturn) {
    this.source = source;
    this.cutter = cutter;
    this.sharedReturn = sharedReturn;
    if (sharedReturn) {
      shared = new CommandArgument();
    }
    preview();
  }

  public NamedValueIterator(StringIterator source) {
    this(source, '=', false);
  }

  private void preview() {
    while (source.hasNext()) {
      lookahead = source.next();
      if (StringX.NonTrivial(lookahead)) {
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
  public CommandArgument next() {
    try {
      CommandArgument wrapper = sharedReturn ? shared : new CommandArgument();
      wrapper.split(lookahead, cutter);
      return wrapper;
    } finally {
      preview();
    }
  }
}
