package pers.hal42.transport;

import pers.hal42.ext.DepthTracker;
import pers.hal42.ext.PushedParser;
import pers.hal42.ext.Span;

import static pers.hal42.transport.PushedJSONParser.JsonAction.*;

/**
 * Created by andyh on 4/3/17.
 */
public class PushedJSONParser extends PushedParser {

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


  public enum JsonAction {
    Illegal,    //not a valid char given state, user must decide how to recover.
    Continue,   //continue scanning
    BeginWad, //open brace encountered
    EndItem,  //comma between siblings
    EndWad,   //closing wad
    Done
  }

  ;

/**
 * tracks incoming byte sequence, records interesting points, reports interesting events
 */


//extended return value
  /**
   * whether a name was seen, bounds recorded in 'name'.
   */
  public boolean haveName;
  /**
   * 'location' recorded at start and end of name token
   */
  public Span name;
  public boolean quotedName;
  /**
   * our first user doesn't care about the difference between [ and {, but someone else may so:
   */
  public boolean orderedWad;

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
        switch(d.last) {
          case '=':
            d.last=':';
            //join
          case ':':
            recordName();
            return Continue; //null name is not the same as no name
          case '[': //array
            orderedWad=true;
            return BeginWad;
          case '{': //normal
            orderedWad=false;
            return BeginWad;
          case ']':
            orderedWad=true;
            return EndWad;
          case '}': //normal
            orderedWad=false;
            return EndWad;
          case ';':
            d.last=',';
            //join
          case ',': //sometimes is an extraneous comma, we choose to ignore those.
            return EndItem;//missing value, possible missing whole child.
          case 0: /*abnormal*/
            return EndItem;//ok for single value files, not so much for normal ones.
          default:
            return Illegal;
        } // switch
    }
    return Illegal;//catch regressions
  }

  /**
   * to be called by agent that called next() after it has handled an item, to make sure we don't duplicate items if code is buggy.
   */
  public void itemCompleted() {
    //essential override.
  }

  /**
   * @param fully is whether to prepare for a new stream, versus just prepare for next item.
   */
  public void reset(boolean fully) {
    super.reset(fully);
  }

  /**
   * subtract the @param offset from all values derived from location, including location.
   * this is useful when a buffer is reused, such as in reading a file a line at a time.
   */
  public void shift(int offset) {
    super.shift(offset);
  }

  protected void recordName() {
    //override
  }

  private void endToken(int mark) {
  }
}
