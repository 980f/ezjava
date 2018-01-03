package pers.hal42.stream;

import pers.hal42.lang.ArrayIterator;
import pers.hal42.lang.StringX;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by andyh on 3/29/17.
 * <p>
 * Named for first use this is being upgraded to other list formats. Forinstance it can do filenames and property paths.
 */
public class Csv {
  public Bracket bracket = new Bracket();
  public boolean quoteAll = true;
  /**
   * whether to quote the framing characters, usuallky true-false only if you guarantee they won't be present in the content
   */
  public boolean quoteFraming = true;

  /**
   * the values to be comma separated.
   * use an ordered collection which gets frequent appends and the occasional prepend, rarely fiddled with in the middle.
   */
  public void print(Iterator<Object> fields, PrintStream os) {
    if (bracket.before) {
      os.print(bracket.sep);
    }
    if (!fields.hasNext()) {
      return;
    }
    boolean emitSeparator = false;
    while (fields.hasNext()) {
      Object item = fields.next();
      if (emitSeparator) {  //precedes datum
        os.print(bracket.sep);
      }
      emitSeparator = true;
      String image = item.toString();
      boolean quoteThisone = quoteAll || (quoteFraming && bracket.needsQuoting(image));
      if (quoteThisone) {
        os.print('"');
      }
      os.print(image);
      if (quoteThisone) {
        os.print('"');
      }
    }
    ;
    if (bracket.after) {
      os.print(bracket.sep);
    }
    os.print('\n');//system independent eol.
  }

  public void print(PrintStream os, Object... things) {
    print(new ArrayIterator<>(things), os);
  }

  public void printMap(Map<?, ?> map, PrintStream ps) {
    map.forEach((k, v) -> print(ps, k, v));
  }

  /** variations such as braces or parens and tabs vs comma */
  public class Bracket {
    public char sep = ',';
    public char open = '(';
    public char close = ')';

    //todo:2 add bracket char different from sep, auto select matching one
    public boolean before = false;
    public boolean after = false;

    public boolean needsQuoting(String image) {
      return StringX.hasFlag(image, sep) || before && StringX.hasFlag(image, open) || after && StringX.hasFlag(image, close);
    }
  }
}
