package pers.hal42.util;
import pers.hal42.io.ByteFifo;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ByteArrayFIFO extends ObjectFifo {

  public ByteArrayFIFO() {
  }

  public byte [] nextByteArray() {
    return (byte []) next();
  }

//  /**
//   * Creates N byte arrays of maxlength (or less on the last one), and returns the new size of the FIFO
//   */
//  public int putSplit(byte [] bytes, int maxlength) {
//    ByteFifo bfifo = new ByteFifo(bytes.length);
//    OutputStream os = bfifo.getOutputStream();
//    try {
//      os.write(bytes);
//    } catch (Exception ex) {
//      // +++ ???
//    } finally {
//      return putSplit(bfifo, maxlength);
//    }
//  }

  /**
   * Creates N byte arrays of maxlength (or less on the last one), and returns the new size of the FIFO
   */
  public synchronized int putSplit(ByteFifo bfifo, int maxlength) {
    return putSplit(bfifo.getInputStream(), maxlength);
  }

  // +++ get rid of this function and instead use full byte[].
  private synchronized int putSplit(InputStream is, int maxlength) {
    // --- potential for infinite loop
    while(true) {
      byte [] bytes = null;
      int avail = 0;
      try {
        avail = is.available();
      } catch (Exception e) {
        // +++ bitch
      }
      if(avail < maxlength) {
        if(avail == 0) {
          break;
        } else {
          bytes = new byte[avail];
        }
      } else {
        bytes = new byte[maxlength];
      }
      try {
        is.read(bytes);
        put(bytes);
      } catch (Exception e) {
        // +++ bitch
      }
    }
    return this.Size();
  }

  public synchronized int putSplit(byte [ ] origbytes, int maxlength) {
    return putSplit(new ByteArrayInputStream(origbytes), maxlength);
  }

  public synchronized byte [][] getArray() {
    int length = Size();
    byte [][] bytes = new byte[length][];
    byte [] bytearray = null;
    for(int i = 0; (bytearray = nextByteArray()) != null;i++) {
      bytes[i] = bytearray;
    }
    return bytes;
  }

  public byte [] getCatAll() {
    byte [][] all = getArray();
    int totallen = 0;
    for(int i = all.length; i-->0;) {
      totallen+=all[i].length;
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream(totallen);
    for(int i = 0; i < all.length; i++) { // must be in order
      try {
        baos.write(all[i]);
      } catch (Exception ex) {
        // +++ ???
      }
    }
    return baos.toByteArray();
  }
}
