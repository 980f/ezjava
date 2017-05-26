package pers.hal42.ext;

import pers.hal42.lang.Char;
import pers.hal42.text.Ascii;

import static pers.hal42.ext.PushedParser.Action.*;
import static pers.hal42.ext.PushedParser.Phase.*;

/**
 * Created by andyh on 4/3/17.
 * <p>
 * Inner loop of parsing logic, identifies strings separated by the given separators.
 * This reacts to quotes, and to slashes, but doesn't do escape processing.
 * It doesn't actually extract the token strings, it returns where they are by index into the stream that is passed to it a character at a time.
 * todo:2 enhance quoting to allow single quotes where doulbe is now required.
 */
public class PushedParser {

  /**
   * lexer states and events
   * <table border="1"> <tbody>
   * <tr> <td>from\to</td> <td>Before</td>            <td>Inside</td>       <td>After</td>      </tr>
   * <tr> <td>Before</td>  <td>empty item</td>        <td>begin token</td>  <td>--</td>         </tr>
   * <tr> <td>Inside</td>  <td>end item and token</td><td>(no event)</td>   <td>end token</td>  </tr>
   * <tr> <td>After</td>   <td>end item</td>          <td>--</td>           <td>(no event)</td> </tr>
   * </tbody>     </table>
   */
  protected enum Phase {
    Before, //nothing known, expect name or value
    Inside, //inside quotes
    After, //inside unquoted text: we're tolerant so this can be a name or a symbolic value
  }
  public enum Action {
    //these tend to pass through all layers, to where the character source is:
    Continue,   //continue scanning
    Illegal,    //not a valid char given state, user must decide how to recover.
    Done,     //pass through EOF
    //the next two tend to be lumped together:
    EndItem,   //well separated seperator
    EndTokenAndItem,  //seperator ended item.

    //no-one seems to care about these events, we'll keep them in case value must be extracted immediatley after the terminating character
    BeginToken, //record cursor, it is first char of something.
    EndToken,  //just end the token

  }

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
  public Diag d = new Diag();
  protected String separators;
  protected Phase phase = Before;
  protected boolean inQuote;
  /**
   * 'cursor' recorded at start and end of token
   */
  protected Span token = new Span();
  /**
   * whether value was found with quotes around it
   */
  protected boolean wasQuoted;
  //  /**
//   * used to skip over utf extended chars
//   */
//  protected CountDown utfFollowers = new CountDown(0);
  boolean slashed = false;

  public PushedParser() {
    reset(true);
  }

  protected void itemCompleted() {
    token.clear();
    phase = Before;
    inQuote = false;//usually already is, except on reset
  }

  public void reset(boolean fully) {
    itemCompleted();

    if (fully) {
      d.location = 0;
      d.row = 1; //1-based to match most text editor line numbering.
      d.column = 0;//see above
      d.last = 0;
    }
  }

  /**
   * call this when you have shifted the data in the buffer that these indices are offsets in to.
   * Intended use is to support the input stream being read in blocks. When you physically remove bytes that are no longer in scope to make room for more you call this so that any field that is partially scanned will stay coordinated.
   */
  public void shift(int offset) {
    //this line did not make sense, column and row should always reflect absolute indexes: d.column = d.location - offset;//only correct for normal use cases
    token.shift(offset);
    d.location -= offset;
  }

  protected void lookFor(String seps) {
    separators = seps;
  }

  private Action endToken(boolean andItem) {
    wasQuoted = inQuote;
    inQuote = false;
    //if a field ends with whitespace we are still looking for a seperator. If the seperator ends the field then pass andItem==true.
    //this is relevant if fields are separate by just whitespace, we don't support that.
    //someone should document what adjacent quoted strings do, hopefully this just appends the text like in C source code. IE "abc""def" abc"def" should be the same as "abcdef"
    phase = andItem ? Before : After;
    token.highest = d.location;
    return andItem ? EndTokenAndItem : EndToken;
  }

  private void beginValue() {
    phase = Inside;
    token.begin(d.location);
  }

  public Action next(byte pushed) {
    try {
      d.last = (char) pushed; //d will see raw utf8 chars. None of those are framing so it doesn't matter.

      if (pushed == '\n') {
        d.column = 1;//match pre-increment below.
        ++d.row;
      } else {
        ++d.column;
      }

      Char ch = new Char(pushed);

      if (pushed == 0) {//end of stream
        if (inQuote) {
          switch (phase) {
          case Before: //file ended with a start quote.
            return Illegal;
          case Inside: //act like endquote was missing
            return endToken(true);
          case After:
            return Illegal;//shouldn't be able to get here.
          }
        } else {
          switch (phase) {
          case Before:
            return Done;
          case Inside:
            return endToken(true);
          case After:
            return EndItem; //eof push out any partial node
          }
        }
        return Illegal;//can't get here
      }

      //we do not convert escape sequences here, we honor them somewhat
      if (slashed) {//it and the follower are to be treated as a generic text char
        slashed=false;
        ch.setto(Ascii.k);//will treat the same as some other non-framing character
      } else if (ch.isSlash()) {//first byte of a utf8 multibyte character
        slashed=true;
        ch.setto(Ascii.k);
      }

      if (inQuote) {
        if (ch.is('"')) {//end quote
          return endToken(false);
        }
        //still inside quotes
        if (phase == Before) {//first char after quote is first char of token
          beginValue();
          return BeginToken;
        }
        return Continue;
      }
      //not quoted nor is char a quote or a slash or the character after a slash
      switch (phase) {
      case Inside:
        if (ch.isWhite()) {
          return endToken(false);
        }
        if (ch.in(separators)) {
          return endToken(true);
        }
        return Continue;//end Inside

      case After:
        if (ch.isWhite()) {
          return Continue;
        }
        if (ch.in(separators)) {
          phase = Before;
          return EndItem;
        }
        return Illegal;

      case Before:  //expecting name or value
        if (ch.isWhite()) {
          return Continue;
        }
        if (ch.in(separators)) {
          endToken(true);
          return EndTokenAndItem;//a null one
        }
        if (ch.is('"')) {
          inQuote = true;
          return Continue;//but not yet started in chunk, see if(inQuote)
        }
        beginValue();
        return BeginToken;
      } // switch
      return Illegal;// in case we have a missing case above, should never get here.
    } finally {
      ++d.location;
    }
  }

}
