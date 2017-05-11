package pers.hal42.util;

import java.io.PrintStream;

public class TextProgress {

  protected int length;
  protected boolean solidBar;
  protected boolean first; // indicates when first run is
  protected int runningCount;
  protected PrintStream outputStream;
  protected StringBuilder indentText = new StringBuilder();
  protected static final char backspace = '\b';
  protected static final char space     = ' ';

  // change these at runtime if you want ...
  // for solidBar: [|||....]
  // and not:      [..|....]
  public char indicator = '|';
  public char fill      = '.'; // I change this to a space for solids: [|||    ]
  public char startChar = '[';
  public char endChar   = ']';
  public int  indent    = 2;

/**
 * internal - actually does the printing of characters
 */
  protected void print(int position) {
    // make indentText here if it isn't done yet
    if(indentText.length() != indent) {
      for(int i = 0; i < indent; i++) {
        indentText.append(space);
      }
    }
    // don't backspace if first !
    if(first) {
      first = false;
      outputStream.print(indentText);
      outputStream.print(startChar); // only printed the first time (not erased)
    } else {
      // eraseToStart
      for(int c = length+1; c-->0;) {
        outputStream.print(backspace);
      }
    }
    for(int i = 1; i <= length; i++) {
      outputStream.print((solidBar ? (i <= position) : (i == position)) ? indicator : fill);
    }
    outputStream.print(endChar);
  }

/**
 * clears the entire progressbar (erases it completely from screen)
 */
  public void clear() {
    // do this dance since backspace doesn't ERASE the characters
    for(int c = length+indent+1; c-->0;) { // 1 for startChar
      outputStream.print(backspace);
    }
    for(int c = length+indent+1; c-->0;) { // 1 for startChar
      outputStream.print(space);
    }
    for(int c = length+indent+1+1; c-->0;) { // 1 for startChar, other for who knows
      outputStream.print(backspace);
    }
  }

/**
 * indicates that you are about to start all over again,
 * meaning that the next time set() or step() is called,
 * clear() will not be run first (it will not backspace)
 */
  public void reset() {
    first = true;
    runningCount = 1;
  }

/**
 * sets the position of the progress bar cursor
 */
  public void set(int position) {
    position = (position < 1) ? 1 : Math.min(length, position);
    runningCount = position;
    print(position);
  }

/**
 * can be used to automatically step through the positions
 */
  public void step() {
    if(++runningCount > length) {
      runningCount = 1;
    }
    set(runningCount);
  }

/**
 * constructor
 */
  public TextProgress(PrintStream outputStream, int length, boolean solidBar) {
    this.length       = length;
    this.solidBar     = solidBar;
    this.outputStream = outputStream;
    reset();
  }

/**
 * constructor that uses System.out as the PrintStream to send text to (typical)
 */
  public TextProgress(int length, boolean solidBar) {
    this(System.out, length, solidBar);
  }

}
