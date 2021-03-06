package pers.hal42.stream;

import pers.hal42.lang.StringX;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.text.Fstring;
import pers.hal42.text.TextColumn;

import java.io.*;

public class HexDump {

  public static final String rightIndention = "    ";
  public static final String hexBlank = "   ";
  public static final int hexWidth = hexBlank.length();
  public static final String markerTrailer = "h: ";
  public static final String separator = " :";
  public static final String headerWing = " ----- ";
  public static final int numWide = 16;
  public static final int availableHeaderWidth = ((numWide * 4) + 17 - (2 * headerWing.length()));
  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(HexDump.class);

/*
  public static final String dump(String header, String content) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    dump(header, content, baos, System.getProperty("line.separator"));
    return String.valueOf(baos);
  }
*/

  public static void dump(String header, String content, OutputStream os) {
    dump(header, content, os, System.getProperty("line.separator"));
  }

  public static void dump(String header, String content, OutputStream os, String eol) {
    try {
      byte[] bytes = StringX.TrivialDefault(content, "").getBytes();
      dump(header, new ByteArrayInputStream(bytes), os, eol);
    } catch (Exception e) {
      dbg.Caught(e, "dump(): Exception opening ByteArrayInputStream.");
    }
  }

  public static void dump(String header, InputStream is, OutputStream os) {
    dump(header, is, os, System.getProperty("line.separator"));
  }

  public static void dump(String header, InputStream is, OutputStream os, String eol) {
    if (os == null) {
      dbg.VERBOSE("dump(): OutputStream == null!");
    } else {
      OutputStreamWriter osw = new OutputStreamWriter(os);
      TextColumn formattedHeader = new TextColumn(StringX.TrivialDefault(header, ""), availableHeaderWidth);
      // now that we have formatted the header, let's output it:
      int lines = formattedHeader.size();
      for (int i = 0; i < lines; i++) {
        headline(formattedHeader.itemAt(i), osw, eol);
      }
      // now that we did the header, let's do the content
      if (is == null) {
        dbg.VERBOSE("dump(): InputStream == null!");
      } else {
        // suck out 16-byte chunks and send them to the formatter
        try {
          byte[] b = new byte[numWide];
          int numRead = numWide;
          long start = 0;
          while (numRead > 0) {
            // what is better, recreating a byte array or zeroing it out?
            numRead = is.read(b, 0, numWide);
            if (numRead > 0) {
              for (int i = numRead; i < numWide; i++) {
                b[i] = 0;
              }
              contentline(start, b, numRead, osw, eol);
              start += numRead;
            }
          }
        } catch (Exception e) {
          dbg.Caught(e, "dump(): Exception reading stream!");
        }
      }
      try {
        osw.flush();
      } catch (Exception e) {
        // +++ ???
      }
    }
  }

  private static void headline(String headerTxt, OutputStreamWriter osw, String eol) {
    try {
      osw.write(headerWing);
      osw.write(headerTxt);
      osw.write(headerWing);
      osw.write(eol);
    } catch (Exception e) {
      dbg.Caught(e, "headline(): Exception outputting header!");
    }
  }

  /**
   * content should be <= 16 bytes
   */
  private static void contentline(long start, byte[] content, int size, OutputStreamWriter osw, String eol) {
    try {
      Fstring hexMarkerFmt = new Fstring(8, '0');
      Fstring hexFormatter = new Fstring(hexWidth - 1, '0');
      osw.write(String.valueOf(hexMarkerFmt.righted(Long.toHexString(start).toUpperCase())));
      osw.write(markerTrailer);
      for (int i = 0; i < numWide; i++) {
        if (i < size) {
          osw.write(' ');
          osw.write(String.valueOf(hexFormatter.righted(Integer.toHexString(content[i]).toUpperCase())));
        } else {
          // now any necessary padding
          osw.write(hexBlank);
        }
        if (i == 7) {
          osw.write(separator);
        }
      }
      osw.write(rightIndention);
      // now do the actual character display
      for (int i = 0; i < size; i++) {
        byte b = content[i];
        // only print printable characters
        if ((b >= ' ') && (b <= '~')) { // +_+ what Character functions will determine this?
          osw.write(b);
        } else {
          osw.write(".");
        }
      }
      osw.write(eol);
    } catch (Exception e) {
      dbg.Caught(e, "contentline(): Exception outputting contents!");
    }
  }

  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("takes a filename for input; outputs to stdout");
    } else {
      try {
        FileInputStream fis = new FileInputStream(args[0]);
        dump("Filename: " + args[0], fis, System.out);
      } catch (Exception e) {
        dbg.Caught(e);
      }
    }
  }
}
