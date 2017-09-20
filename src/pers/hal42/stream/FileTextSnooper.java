package pers.hal42.stream;

import pers.hal42.lang.ReflectX;
import pers.hal42.logging.ErrorLogStream;

import java.io.FileInputStream;

/**
 * utility that will read a file and display on debug stream, and will follow a 'live' filelike thing such as a socket or a terminal
 */

public class FileTextSnooper {
  public int minLength = 10; // this is the default; change it if you want; eventually make it a parameter +++
  public String filename = null;
  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(FileTextSnooper.class);
  private static final String LF = System.getProperty("line.separator");

  public FileTextSnooper(String filename) {
    this.filename = filename;
  }

  public void snoop() {
    FileInputStream file = null;
    int MORETHAN = minLength - 1;
    dbg.VERBOSE(ReflectX.shortClassName(this) + " " + filename);
    try {
      // push the file
      file = new FileInputStream(filename);
    } catch (Exception e) {
      dbg.Caught(e);
    }
    if (file != null) {
      StringBuffer sb = new StringBuffer();
      while (true) {
        // read from the file, one character at a time
        int i = -1;
        try {
          if (file.available() == 0) {
            break;
          }
          i = file.read();
          if (i == -1) {
            break;
          }
        } catch (Exception e) {
          dbg.Caught(e);
        }
        byte b = (byte) i;
        // check to see if it is TEXT (no control characters except CRLF & TAB (printable)).
        if ((b > 31) && (b < 127)) {
          sb.append((char) b);
        } else {
          // if you read in 4 or more, print them as a line (dbg)
          if (sb.length() > MORETHAN) {
            // eject it
            dbg.VERBOSE(String.valueOf(sb));
          }
          sb.setLength(0); // clears it, for starting again
        }
      }
    }
  }

  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("usage: ...FileTextSnooper filename");
    } else {
      dbg.bare = true;
      snoop(args[0]);
    }
  }

  public static void snoop(String filename) {
    FileTextSnooper snooper = new FileTextSnooper(filename);
    snooper.snoop();
  }

}
