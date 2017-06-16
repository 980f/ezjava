package pers.hal42.text;

import pers.hal42.logging.ErrorLogStream;
import pers.hal42.stream.FileListIterator;

import java.io.FileNotFoundException;

/**
 * iterate over strings, if one starts with an '@' then connect to that file and read them from that file. If you make a loop then this will also loop until you run out of memory.
 * It would be easy to add a 'maximum depth' to guard against that
 */
public class IncludeFileIterator extends FancyArgIterator {
  @Override
  public String next() {
    String arg = super.next();
    while (arg.charAt(0) == '@') {//then read some args from a file
      try {
        push(new FileListIterator(arg.substring(1)));
        arg = super.next();
      } catch (FileNotFoundException e) {
        ErrorLogStream.Global().Caught(e, "Trying to include a file in IncludeFileIterator");
      }
    }
    return arg;
  }
}
