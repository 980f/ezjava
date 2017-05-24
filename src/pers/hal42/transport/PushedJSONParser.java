package pers.hal42.transport;

import pers.hal42.ext.DepthTracker;
import pers.hal42.ext.PushedParser;
import pers.hal42.ext.Span;
import pers.hal42.logging.ErrorLogStream;

import static pers.hal42.transport.PushedJSONParser.JsonAction.*;

/**
 * Created by andyh on 4/3/17.
 * tracks incoming byte sequence, records interesting points, reports interesting events
 */
public class PushedJSONParser extends PushedParser {
  static final ErrorLogStream dbg = ErrorLogStream.getForClass(PushedJSONParser.class);
  /**
   * whether a name was seen, bounds recorded in 'name'.
   */
  public boolean haveName;
  /**
   * 'cursor' recorded at start and end of name token
   */
  public final Span name = new Span();
  /**
   * whether the name was quoted.
   */
  public boolean quotedName;
  /**
   * our first user doesn't care about the difference between [ and {, but someone else may so:
   */
  public boolean orderedWad;
  public final JsonStats stats = new JsonStats();

  /**
   * @param equalsAndSemi determines dialect of JSON allowed.
   */
  public PushedJSONParser(boolean equalsAndSemi) {
    String seps = "{}[]:,";
    if (equalsAndSemi) {
      seps += "=;";
    }
    lookFor(seps);
  }

  /**
   * records locations for text extents, passes major events back to caller
   */
  public JsonAction nextitem(char pushed) {

    switch (next(pushed)) {
    case BeginValue:
    case EndValue:
    case Continue:
      return Continue;

    case Illegal:
      return Illegal;
    case Done:
      return Done;

    case EndValueAndItem:
    case EndItem:
      switch (d.last) {
      case '='://treat equals like colon// todo: an enable for this feature
        d.last = ':';
        //join
      case ':':
        if (super.value.isValid()) {
          haveName = true;
          quotedName = super.wasQuoted;
          name.take(super.value);
          recordName(); //early access to name, you don't have to implement this function for the parser to do its job.
          return Continue; //null name is not the same as no name
        } else {
          return Illegal; //stray colon.
        }
      case '[': //array
        orderedWad = true;
        return BeginWad;
      case '{': //normal
        orderedWad = false;
        return BeginWad;
      case ']':
        orderedWad = true;
        return EndWad;
      case '}': //normal
        orderedWad = false;
        return EndWad;
      case ';': //treat semis like commas, //todo: an enable for this
        d.last = ',';
        //join
      case ',': //sometimes is an extraneous comma, we choose to ignore those.
        return EndItem;//missing value, possible missing whole child.
      case 0: /*abnormal*/
        return EndItem;//ok for single value files, not so much for normal ones.
      default:
        return Illegal;//inspect d.last to guess why
      } // switch
    }
    return Illegal;//catch regressions
  }

  /**
   * to be called by agent that called next() after it has handled an item, to make sure we don't duplicate items if code is buggy.
   */
  public void itemCompleted() {
    //essential override.
    haveName = false;
    quotedName = false;
//constructor escaped and 'name' was null
    if (name != null) {
      name.clear();
    } //else name will construct to a null one.
    super.wasQuoted = false;
    super.value.clear();
  }

  /**
   * @param fully is whether to prepare for a new stream, versus just prepare for next item.
   */
  public void reset(boolean fully) {
    super.reset(fully);
  }

  /**
   * subtract the @param offset from all values derived from cursor, including cursor.
   * this is useful when a buffer is reused, such as in reading a file a line at a time.
   */
  public void shift(int offset) {
    super.shift(offset);
  }

  /**
   * this is a stub, you don't need to super it.
   */
  protected void recordName() {
    dbg.VERBOSE("found name between {0} and {1}", name.lowest, name.highest);
  }

//  private void endToken(int mark) {
//  }

  public enum JsonAction {
    Illegal,    //not a valid char given state, user must decide how to recover.
    Continue,   //continue scanning
    BeginWad, //open brace encountered
    EndItem,  //comma between siblings
    EndWad,   //closing wad
    Done
  }

  /**
   * attributes of the file, only interesting for debug
   */
  static public class JsonStats {
    /**
     * number of values
     */
    int totalNodes = 0;
    /**
     * number of terminal values
     */
    int totalScalar = 0;

    DepthTracker depthTracker = new DepthTracker();

    void reset() {
      totalNodes = 0;
      totalScalar = 0;
      depthTracker.reset();
    }

    void onNode(boolean scalar) {
      ++totalNodes;
      if (scalar) {
        ++totalScalar;
      }
    }
  }
}
