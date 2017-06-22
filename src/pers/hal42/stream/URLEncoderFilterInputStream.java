package pers.hal42.stream;

import org.jetbrains.annotations.NotNull;
import pers.hal42.lang.Monitor;
import pers.hal42.lang.ReflectX;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;

/**
 * The class implements a URL encoder FilterInputStream.
 * By setting up such an input stream, an application can read bytes from the underlying input stream in URL-encoded format, converting characters outside the sets {0-9}, {A-Z}, {a-z} into their hexadecimal %'ed formats (eg: %20 for space).
 * <p>
 * NOTE: can probably replace all super.read() with in.read()
 */

public class URLEncoderFilterInputStream extends FilterInputStream {
  private final Monitor thisMonitor = new Monitor("URLEncoderFilterInputStream");
  protected StringBuffer tmpBuff = new StringBuffer(2); // really shouldn't use "String" anything
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
   * Creates a new URLEncoded intput stream to write data to the specified underlying output stream.
   *
   * @param in the underlying output stream.
   */
  public URLEncoderFilterInputStream(InputStream in) {
    super(in);
  }

  public int available() throws IOException {
    return super.available() + tmpBuff.length();
  }

  public void reset() throws IOException /* literally */ {
    throw (new IOException("Can't reset a " + ReflectX.shortClassName(this)));
  }

  public void mark(int readlimit) {
    // see markSupported
  }

  public boolean markSupported() {
    // too difficult for me to mess with right now
    return false;
  }

  public long skip(long n) throws IOException {
    long count = 0;  // probably cleaner way -- i'm in a hurry; works
    for (long i = 0; i < n; i++) {
      if (read() == -1) {
        break;
      } else {
        count++;
      }
    }
    return count;
  }

  /**
   * Reads the specified byte from this URLEncoded input stream.
   *
   * @throws IOException if an I/O error occurs.
   */
  public int read() throws IOException {
    try (AutoCloseable free = thisMonitor.getMonitor()) {
      // if the temp buffer has anything in it, use that, otherwise
      // +++ there is a better way; fixup later
      if (tmpBuff.length() > 0) {
        // do this instead
        int b = (byte) tmpBuff.charAt(0);
        tmpBuff.deleteCharAt(0);
        return b;
      } else {
        // check to see if it needs converting.  If so, convert it
        // otherwise, pass it through
        int b = super.read(); // +++ in.read() instead?
        if (b == -1) {
          return -1;//was a BUG to not return here, a guaranteed irrelevant and serious exception
        }
        if (dontNeedEncoding.get(b)) {
          if (b == ' ') {
            b = '+';
          }
          return b;
        } else {
          //todo:2 this is ox2 method.
          for (int i = 0; i < 2; i++) {
            int b2 = (i == 0) ? (b >> 4) : b;
            char ch = Character.forDigit(b2 & 0xF, 16);
            // converting to use uppercase letter as part of
            // the hex value if ch is a letter.
            if (Character.isLetter(ch)) {
              ch -= caseDiff;
            }
            tmpBuff.append(ch);
          }
          return '%';
        }
      }
    } catch (Exception e) {//false alarm
      e.printStackTrace();
      return 0;
    }
  }

  public int read(@NotNull byte[] b) throws IOException {
    return read(b, 0, b.length);
  }

  public int read(@NotNull byte[] b, int off, int len) throws IOException {
    int end = off + len;
    int count = 0;
    for (int i = off; i < end; i++) {
      int c = read();
      if (c == -1) {
        break;
      }
      b[i] = (byte) c;
      count++;
    }
    return ((count == 0) && (len > 0)) ? -1 : count;
  }
}
