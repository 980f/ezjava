package pers.hal42.text;

import pers.hal42.lang.StringX;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.stream.FileListIterator;

import java.io.FileNotFoundException;

/**
 * iterate over strings, if one starts with an '@' then connect to that file and read them from that file. If you make a loop then this will also loop until you run out of memory.
 * It would be easy to add a 'maximum depth' to guard against that
 */
public class IncludeFileIterator extends FancyArgIterator {
  public boolean filterPounds = true;

  public IncludeFileIterator(boolean filterPounds) {
    this.filterPounds = filterPounds;
  }

  @Override
  public String next() {
    String arg = super.next();
    while (StringX.firstCharIs(arg, '@')) {//then read some args from a file
      try {
        push(new FileListIterator(arg.substring(1)));
      } catch (FileNotFoundException e) {
        ErrorLogStream.Global().Caught(e, "Trying to include a file in IncludeFileIterator");
      }
      arg = super.next();
    }
    if (filterPounds && StringX.firstCharIs(arg, '#')) {
      if (hasNext()) {
        return next();
      } else {
        //we lied about there being another string, pass an empty one
        return "";
      }
    }
    return arg;
  }
}
