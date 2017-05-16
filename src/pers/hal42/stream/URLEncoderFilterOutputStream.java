package pers.hal42.stream;

import pers.hal42.lang.Monitor;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.BitSet;

/**
 * The class implements a URL encoder FilterOutputStream. By setting up such an output stream, an application can write bytes to the underlying output stream in URL-encoded format, converting characters outside the sets {0-9}, {A-Z}, {a-z} into their hexadecimal %'ed formats (eg: %20 for space).
 * <p>
 * NOTE: can probably replace all super.write(whatever) with out.append(whatever)
 */

public class URLEncoderFilterOutputStream extends FilterOutputStream {
  private final Monitor thisMonitor = new Monitor("URLEncoderFilterOutputStream");
  static final int caseDiff = ('a' - 'A');
  // stolen from URLEncoder.java
  static BitSet dontNeedEncoding;

  static {
    dontNeedEncoding = new BitSet(256);
    int i;
    for (i = 'a'; i <= 'z'; i++) {
      dontNeedEncoding.set(i);
    }
    for (i = 'A'; i <= 'Z'; i++) {
      dontNeedEncoding.set(i);
    }
    for (i = '0'; i <= '9'; i++) {
      dontNeedEncoding.set(i);
    }
    dontNeedEncoding.set(' '); /* encoding a space to a + is done in the encode() method */
    dontNeedEncoding.set('-');
    dontNeedEncoding.set('_');
    dontNeedEncoding.set('.');
    dontNeedEncoding.set('*');
  }

  /**
   * Creates a new URLEncoded output stream to write data to the
   * specified underlying output stream.
   *
   * @param out the underlying output stream.
   */
  public URLEncoderFilterOutputStream(OutputStream out) {
    super(out);
  }

  /**
   * Writes the specified byte to this URLEncoded output stream.
   *
   * @param b the byte to be written.
   * @throws IOException if an I/O error occurs.
   */
  public void write(int b) throws IOException {
    try {
      thisMonitor.getMonitor();
      // check to see if it needs converting.  If so, conert it
      // otherwise, pass it through
      b = b & 255; // prevents dumbass negatives
      if (dontNeedEncoding.get(b)) {
        if (b == ' ') {
          b = '+';
        }
        super.write(b);
      } else {
        super.write('%');
        for (int i = 0; i < 2; i++) {
          int b2 = (i == 0) ? (b >> 4) : b;
          char ch = Character.forDigit(b2 & 0xF, 16);
          // converting to use uppercase letter as part of
          // the hex value if ch is a letter.
          if (Character.isLetter(ch)) {
            ch -= caseDiff;
          }
          super.write((byte) ch);
        }
      }
    } finally {
      thisMonitor.freeMonitor();
    }
  }
}
