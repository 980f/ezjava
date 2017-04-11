package pers.hal42.ext;

import pers.hal42.lang.UTF8;
import pers.hal42.text.Ascii;

import static pers.hal42.ext.PushedParser.Action.*;
import static pers.hal42.ext.PushedParser.Phase.*;

/**
 * Created by andyh on 4/3/17.
 */
public class PushedParser {

  String seperators;

  public static class Diag {
    public int location = 0;//number of chars total
    /**
     * number of newelines that have been seen
     */
    public int row = 0;//number of lines, for error messages
    /**
     * number of bytes inspected since last newline was seen
     */
    public int column = 0;//where in row
    //last char examined, retained for error messages
    public char last = 0;

    public void reset() {
      location = 0;
      row = 0;
      column = 0;
      last = 0;
    }

  }

  public Diag d=new Diag();

  protected boolean inQuote;
  /**
   * used to skip over utf extended chars
   */
  protected CountDown utfFollowers=new CountDown(0);

  /**
   * lexer states and events
   * <table border="1"> <tbody>
   * <tr> <td>from\to</td> <td>Before</td>            <td>Inside</td>       <td>After</td>      </tr>
   * <tr> <td>Before</td>  <td>empty item</td>        <td>begin token</td>  <td>--</td>         </tr>
   * <tr> <td>Inside</td>  <td>end item and token</td><td>(no event)</td>   <td>end token</td>  </tr>
   * <tr> <td>After</td>   <td>end item</td>          <td>--</td>           <td>(no event)</td> </tr>
   * </tbody>     </table>
   */
  public enum Phase {
    Before, //nothing known, expect name or value
    Inside, //inside quotes
    After, //inside unquoted text: we're tolerant so this can be a name or a symbolic value
  }

  Phase phase;

  /**
   * 'location' recorded at start and end of token
   */
  Span value=new Span();
  /**
   * whether value was found with quotes around it
   */
  boolean wasQuoted;

  public enum Action {
    //these tend to pass through all layers, to where the character source is:
    Continue,   //continue scanning
    Illegal,    //not a valid char given state, user must decide how to recover.
    Done,     //pass through EOF
    //the next two tend to be lumped together:
    EndItem,   //well separated seperator
    EndValueAndItem,  //seperator ended item.

    //no-one seems to care about these events, we'll keep them in case value must be extracted immediatley after the terminating character
    BeginValue, //record location, it is first char of something.
    EndValue,  //just end the token

  }

  void itemCompleted() {
    value.clear();
    phase = Before;
    inQuote = false;//usually already is, except on reset
  }

  public void reset(boolean fully) {
    itemCompleted();

    if (fully) {
      d.location = 0;
      d.row = 0;
      d.column = 0;
      d.last = 0;
    }
  }

  public void shift(int offset) {
    d.column = d.location - offset;//only correct for normal use cases
    value.shift(offset);
    d.location -= offset;
  }

  public PushedParser() {
    reset(true);
  }

  public void lookFor(String seps) {
    seperators = seps;
  }

  Action endValue(boolean andItem) {
    wasQuoted = inQuote;
    inQuote = false;
    phase = andItem ? Before : After;
    value.highest = d.location;
    return andItem ? EndValueAndItem : EndValue;
  }

  void beginValue() {
    phase = Inside;
    value.begin(d.location);
  }

  public Action next(char pushed) {
//added a finally clause    IncrementOnExit<unsigned> ioe(d.location);//increment before we dig deep lest we forget given the multiple returns.
    //   try {
    d.last = pushed;

    if (pushed == '\n') {
      d.column = 1;//match pre-increment below.
      ++d.row;
    } else {
      ++d.column;
    }

    UTF8 ch = new UTF8(pushed);

    if (pushed == 0) {//end of stream
      if (inQuote) {
        switch (phase) {
          case Before: //file ended with a start quote.
            return Illegal;
          case Inside: //act like endquote was missing
            return endValue(true);
          case After:
            return Illegal;//shouldn't be able to get here.
        }
      } else {
        switch (phase) {
          case Before:
            return Done;
          case Inside:
            return endValue(true);
          case After:
            return EndItem; //eof push out any partial node
        }
      }
      return Illegal;//can't get here
    }


    if (utfFollowers.decrement()) {//it is to be treated as a generic text char
      ch.setto(Ascii.k);
    } else if (ch.isMultibyte()) {//first byte of a utf8 multibyte character
      utfFollowers.setto(ch.numFollowers());
      ch.setto(Ascii.k);
    }

    //we still process it
    if (inQuote) {
      if (ch.is('"')) {//end quote
        return endValue(false);
      }
      if (ch.is('\\')) {
        utfFollowers.increment();//ignores all escapes, especially \"
      }
      //still inside quotes
      if (phase == Before) {//first char after quote is first care of token
        beginValue();
        return BeginValue;
      }
      return Continue;
    }

    switch (phase) {
      case Inside:
        if (ch.isWhite()) {
          return endValue(false);
        }
        if (ch.in(seperators)) {
          return endValue(true);
        }
        return Continue;
//    break;//end Inside

      case After:
        if (ch.isWhite()) {
          return Continue;
        }
        if (ch.in(seperators)) {
          phase = Before;
          return EndItem;
        }
        return Illegal;
//    break;

      case Before:  //expecting name or value
        if (ch.isWhite()) {
          return Continue;
        }
        if (ch.in(seperators)) {
          endValue(true);
          return EndValueAndItem;//a null one
        }
        if (ch.is('"')) {
          inQuote = true;
          return Continue;//but not yet started in chunk, see if(inQuote)
        }
        beginValue();
        return BeginValue;
    } // switch
    return Illegal;// in case we have a missing case above, should never get here.
  }

}
