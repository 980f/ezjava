/**
 *
 * This class implements an output stream in which the data is written into and read from a byte array.
 * The buffer automatically grows as data is written to it.
 *
 * This class implements an input stream which contains an internal buffer that contains bytes that may be read from the stream.
 * An internal counter keeps track of the next byte to be supplied by the <code>read</code> method.
 *
 * todo:2 use a circular buffer to reduce the copying overhead. Reference C code is available.
 * todo:0 replace synchronized methods with synchronizing on some object, to refine scope.
 * todo:0 make blocking read work. Users desiring unblocked reads can check size.
 * todo:1 add refinements to 'ensureCapacity' mimicking ju.Vector class.
 */


package pers.hal42.stream;

import pers.hal42.thread.Waiter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ByteFifo {
// !!! Can't use this here since the ByteFifo is used in one!  Causes infinite loop!
//alh: loop must be deeply embedded, we are using classes that use ELS...
//  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(ByteFifo.class);

  /**
   * The buffer where data is stored.
   */
  protected byte buf[];
  /**
   * The number of valid bytes in the buffer.
   */
  protected int count;

  // the stream stuff
  protected ByteFifoOutputStream bafos = null;
  private boolean bafosClosed = false;
  protected ByteFifoInputStream  bafis = null;
  private boolean bafisClosed = false;

  /**
   * Creates a new byte array output stream. The buffer capacity is
   * initially 32 bytes, though its size increases if necessary.
   */
  public ByteFifo(boolean blocking) {
    this(32, blocking);
  }



  /**
   * Creates a new byte array output stream, giving the buffer the initial
   * capacity specified by the <code>size</code> parameter,
   * though its size increases if necessary.
   */
  public ByteFifo(int size, boolean blocking) {
    if (size < 0) {
      //throw new IllegalArgumentException("Negative initial size: " + size);
      size=0; //value is just advisory, exceptions are for fatal conditions.
    }
    buf = new byte[size];
    count = 0;
    bafos = new ByteFifoOutputStream(this);
    bafis = blocking ?new ByteFifoBlockingInputStream(this) : new ByteFifoNonBlockingInputStream(this);
  }

  /**
   * Resets the <code>count</code> field of this object
   * to zero, so that all currently accumulated output in the
   * buffer is discarded. The object can be used again,
   * reusing the already allocated buffer space.
   * +++ how does this affect a thread awaiting a byte?
   */
  public synchronized void clear() {
    count = 0;
  }

  /**
   * Returns the current size of the buffer.
   *
   * @return  the value of the <code>count</code> field, which is the number
   *          of valid bytes in this output stream.
   */
  public int size() {
    return count;
  }

  /**
   * Creates a newly allocated byte array. Its size is the current
   * size of this object, and the valid contents of the buffer
   * have been copied into it.
   *
   * @return  the current contents of this object, as a byte array.
   */
  public synchronized byte [] toByteArray() {
    byte newbuf[] = new byte[count];
    System.arraycopy(buf, 0, newbuf, 0, count);
    return newbuf;
  }

  /**
   * Converts the buffer's contents into a string, translating bytes into
   * characters according to the platform's default character encoding.
   *
   * @return String translated from the buffer's contents.
   */
  public String toString() {
    return new String(buf, 0, count);
  }

///////////////////////////////////////
// stream-based stuff
///////////////////////////////////////

  /*package*/ synchronized void write(int b) { // package since it should only be written to by the OutputStream.
// mutex starting here ...
    int newcount = count + 1;
    ensureCapacity(newcount);
    buf[count] = (byte)b;
    count = newcount;
    //notify read() that bytes have come in!
    bafis.wakeup();
// mutex ending here ...
  }

  private synchronized void ensureCapacity(int newcount) {
    if(newcount > buf.length) {
      int oldcapacity = buf.length; // for reporting
      byte newbuf[] = new byte[Math.max(buf.length << 1, newcount)];
      System.arraycopy(buf, 0, newbuf, 0, count);
      buf = newbuf;
    }
  }

  /**
   * atomic read&adjust counter
   * only should be used by ByteFifoInputStream which will do any blocking or other stream related stuff
   */

//MAKE THIS BLOCK !!! ??? NO! put blocking in stream class.
  /*package*/ synchronized int simpleRead() { // only supposed to be used by the inputstream
    int ret = -1;
// mutex starting here ...
    if(count > 0) {
      ret = buf[0] & 255;
      System.arraycopy(buf, 1, buf, 0, --count);
    }
// mutex ending here ...
    return ret;
  }

  public synchronized int available() {
    return count;
  }

  public OutputStream getOutputStream() {
    return bafos;
  }

  public InputStream getInputStream() {
    return bafis;
  }

}

class ByteFifoOutputStream extends OutputStream {
  private ByteFifo fifo = null;
  private boolean isClosed = false;

  public ByteFifoOutputStream(ByteFifo fifo) {
    this.fifo = fifo;
  }

  public synchronized void write(int b) throws IOException {
    if(isClosed) {
      throw new IOException("Stream is closed");
    } else {
      fifo.write(b);
    }
  }

  public synchronized void close() throws IOException {
    if(isClosed) {
      throw new IOException("Stream already closed");
    } else {
      isClosed = true;
    }
  }

}

class ByteFifoNonBlockingInputStream extends ByteFifoInputStream {
  public ByteFifoNonBlockingInputStream(ByteFifo fifo) {
    super(fifo);
  }
}

class ByteFifoBlockingInputStream extends ByteFifoInputStream {
  private Waiter forData = new Waiter();

  public ByteFifoBlockingInputStream(ByteFifo fifo) {
    super(fifo);
    forData=Waiter.Create(0,false,null);
  }

  /*package*/ void wakeup(){ //write data has arrived.
    forData.Stop();
  }

  public synchronized void close() throws IOException {
    boolean except = !isClosed;
    super.close();
    if(except) {
      forData.forceException();
    }
  }

  public int read() throws IOException {
    if(!isClosed) {
      synchronized(this){//must enclose all references to fifo object in a single synch
        if(available()==0){
          forData.Start(Long.MAX_VALUE);//doesn't actually start, just configs
          switch(forData.run()){
            default://should never happen
            case Waiter.Ready://should never happen
              throw new IOException("Input Logic Error");
            case Waiter.Notified://data available, from wakeup()
              break;//flow throuogh to simple read().
            case Waiter.Timedout://should never happen under normal use, could return "endOfStream" condition
              throw new IOException("Input Timed Out");
//              break;
            case Waiter.Interrupted://should never happen, perhaps reserve for stream close
            case Waiter.Excepted://should never happen, reserved for problems starting the wait.
              throw new IOException("Stream Closed while waiting for input");  //throw something different than timedout
//              break;
          }
        }
        return super.read();
      }
    } else {
      return super.read(); // will throw
    }
  }
}

abstract class ByteFifoInputStream extends InputStream {
  private ByteFifo fifo = null;
  protected boolean isClosed = false;
  public ByteFifoInputStream(ByteFifo fifo) {
    this.fifo = fifo;
  }

  /**
   *
   * @return byte read from fifo, blocks if fifo is empty
   * @throws IOException when stream closed while waiting for input. all other exceptions are pathological.
   */
  public int read() throws IOException {
    if(isClosed) {
      throw new IOException("Stream is closed");
    } else {
      synchronized(this){//must enclose all references to fifo object in a single synch
        return fifo.simpleRead();
      }
    }
  }

  /*package*/ void wakeup(){ //write data has arrived.
    // stub
  }

  public int available() throws IOException {
    if(isClosed) {
      throw new IOException("Stream is closed");
    } else {
      return fifo.available();
    }
  }

  public synchronized void close() throws IOException {
    if(isClosed) {
      throw new IOException("Stream already closed");//must we? why is this an error condition?
    } else {
      isClosed = true;
    }
  }

}
//$Id: ByteFifo.java,v 1.10 2004/01/19 17:03:25 mattm Exp $
