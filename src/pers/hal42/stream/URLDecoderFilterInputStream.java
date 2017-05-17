package pers.hal42.stream;

import pers.hal42.lang.Monitor;
import pers.hal42.logging.ErrorLogStream;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class URLDecoderFilterInputStream extends FilterInputStream {

  protected StringBuffer tmpBuff = new StringBuffer(3); // really shouldn't use "String" anything
  private Monitor thisMonitor = new Monitor("URLDecoderFilterInputStream");
  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(URLDecoderFilterInputStream.class);

  /**
   * Creates a new URLEncoded input stream to write data to the specified underlying output stream.
   *
   * @param in the underlying output stream.
   */
  public URLDecoderFilterInputStream(InputStream in) {
    super(in);
  }

  public int available() throws IOException {
    return super.available() + tmpBuff.length();
  }

  public void reset() throws IOException /* literally */ {
    throw (new IOException("Can't reset a " + URLDecoderFilterInputStream.class.getName()));
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

  public int read(byte[] b) throws IOException {
    return read(b, 0, b.length);
  }

  public int read(byte[] b, int off, int len) throws IOException {
    int end = off + len;
    int count = 0;
    for (int i = off; i < end; i++) {
      int c = read();
      if (c == -1) {
        break;
      }
      b[i] = (byte) ((char) c);
      count++;
    }
    return ((count == 0) && (len > 0)) ? -1 : count;
  }

  /**
   * Reads the specified byte from this URLEncoded input stream.
   *
   * @throws IOException if an I/O error occurs.
   */
  public int read() throws IOException {
    int retval = 0;
    try (AutoCloseable free = thisMonitor.getMonitor()) {
      int c = -1;
      // be sure we have at least one character in the buffer
      if (tmpBuff.length() == 0) {
        getOne();
      }
      // after attempting to read one, if it is still empty, return -1
      if (tmpBuff.length() > 0) {
        // otherwise, something's in the buffer; see if it needs conversion
        if (tmpBuff.charAt(0) == '%') {
          while ((tmpBuff.length() < 3) && getOne()) {
          }
          // we got what we could; so attempt the conversion
          if (tmpBuff.length() >= 3) {
            int chr = -1;
            try {
              String toParse = tmpBuff.substring(1, 3);
              chr = Integer.parseInt(toParse, 16);
            } catch (NumberFormatException e) {
              // if it is misformatted, just spew as normal
            }
            if (chr != -1) { // conversion took
              tmpBuff.delete(0, 3);   // shouldn't except (deletes the 3 chars starting at position 1)
              tmpBuff.insert(0, (char) chr); // prepend the converted character
            }
          }
        } else {
          if (tmpBuff.charAt(0) == '+') {
            tmpBuff.setCharAt(0, ' ');
          }
        }
        // return the first available char
        c = tmpBuff.charAt(0);
        tmpBuff.deleteCharAt(0);
      }
// this was converting +'s to ' 's that ought not be (that were +'s before encoding), so I moved it into the function above.
//      if(c == '+') {
//        c = ' ';
//      }
      retval = (byte) c; // one exit point
    } finally {
      return retval;
    }
  }

  private boolean getOne() throws IOException {
    int chr = super.read();
    if (chr != -1) {
      tmpBuff.append((char) chr);
      return true;
    }
    return false;
  }
}
