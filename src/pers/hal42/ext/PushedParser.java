package pers.hal42.ext;

import jdk.nashorn.internal.ir.JumpToInlinedFinally;
import pers.hal42.lang.ObjectX;

import static pers.hal42.ext.PushedParser.Action.*;
import static pers.hal42.ext.PushedParser.Phase.*;

/**
 * Created by andyh on 4/3/17.
 */
public class PushedParser {

   String seperators;
  static class  Diag {
    int  location=0;//number of chars total
    /** number of newelines that have been seen */
    int row=0;//number of lines, for error messages
    /** number of bytes inspected since last newline was seen */
    int column=0;//where in row
    //last char examined, retained for error messages
    char last=0;

  void reset(){
    location = 0;
    row = 0;
    column = 0;
    last=0;
  }

} ;
  Diag d;

  boolean inQuote;
  /** used to skip over utf extended chars */
  CountDown utfFollowers;

/** lexer states and events
 * <table border="1"> <tbody>
 <tr> <td>from\to</td> <td>Before</td>            <td>Inside</td>       <td>After</td>      </tr>
 <tr> <td>Before</td>  <td>empty item</td>        <td>begin token</td>  <td>--</td>         </tr>
 <tr> <td>Inside</td>  <td>end item and token</td><td>(no event)</td>   <td>end token</td>  </tr>
 <tr> <td>After</td>   <td>end item</td>          <td>--</td>           <td>(no event)</td> </tr>
 </tbody>     </table> */
enum Phase {
  Before, //nothing known, expect name or value
  Inside, //inside quotes
  After, //inside unquoted text: we're tolerant so this can be a name or a symbolic value
};
Phase phase;

  /** 'location' recorded at start and end of token */
  Span value;
  /** whether value was found with quotes around it */
  boolean wasQuoted;

enum Action {
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

};
//
//  String lookFor(String seps){
//
//  }
//    /**
//     *  records locations for text extents, passes major events back to caller
//     */
//    Action next(char pushed);
//
//    /** called by entity that called next() after it has handled an item, to make sure we don't duplicate items if code is buggy. */
//    void itemCompleted();
//
//    /** @param fully is whether to prepare for a new stream, versus just prepare for next item. */
//    void reset(bool fully);
//
//    /** subtract the @param offset from all values derived from location, including location.
//     * this is useful when a buffer is reused, such as in reading a file a line at a time. */
//    void shift(unsigned offset);
//
//private:
//  Action endValue(bool andItem);
//  void beginValue();

  void itemCompleted(){
    value.clear();
    phase=Before;
    inQuote=false;//usually already is, except on reset
  } // Parser::next

  void reset(boolean fully){
    itemCompleted();

    if(fully) {
      d.location = 0;
      d.row = 0;
      d.column = 0;
      d.last=0;
    }
  }

  void shift(int offset){
    d.column=d.location-offset;//only correct for normal use cases
    value.shift(offset);
    d.location-=offset;
  }

  PushedParser(){
    reset(true);
  }

void lookFor(String seps){
    seperators=seps;
  }

  Action endValue(boolean andItem){
    wasQuoted=inQuote;
    inQuote=false;
    phase = andItem?Before:After;
    value.highest=d.location;
    return andItem?EndValueAndItem:EndValue;
  }

  void beginValue(){
    phase = Inside;
    value.begin( d.location);
  }

  Action next(char pushed){
//added a finally clause    IncrementOnExit<unsigned> ioe(d.location);//increment before we dig deep lest we forget given the multiple returns.
 //   try {
    d.last=pushed;

    if(pushed=='\n') {
      d.column = 1;//match pre-increment below.
      ++d.row;
    } else {
      ++d.column;
    }

    UTF8 ch(pushed);

    if(pushed==0) {//end of stream
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


    if(utfFollowers) {//it is to be treated as a generic text char
      ch = 'k';
    } else if(ch.isMultibyte()) {//first byte of a utf8 multibyte character
      utfFollowers = ch.numFollowers();
      ch = 'k';
    }

    //we still process it
    if(inQuote) {
      if(ch.is('"')) {//end quote
        return endValue(false);
      }
      if(ch.is('\\')){
        ++utfFollowers;//ignores all escapes, especially \"
      }
      //still inside quotes
      if(phase==Before) {//first char after quote is first care of token
        beginValue();
        return BeginValue;
      }
      return Continue;
    }

    switch (phase) {
      case Inside:
        if(ch.isWhite()) {
          return endValue(false);
        }
        if(ch.in(seperators)){
          return endValue(true);
        }
        return Continue;
//    break;//end Inside

      case After:
        if(ch.isWhite()) {
          return Continue;
        }
        if(ch.in(seperators)){
          phase=Before;
          return EndItem;
        }
        return Illegal;
//    break;

      case Before:  //expecting name or value
        if(ch.isWhite()) {
          return Continue;
        }
        if(ch.in(seperators)){
          endValue(true);
          return EndValueAndItem;//a null one
        }
        if(ch.is('"')){
          inQuote = true;
          return Continue;//but not yet started in chunk, see if(inQuote)
        }
        beginValue();
        return BeginValue;
    } // switch
    return Illegal;// in case we have a missing case above, should never get here.
  }

}
