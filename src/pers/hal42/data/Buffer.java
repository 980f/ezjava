package pers.hal42.data;

import pers.hal42.logging.ErrorLogStream;
import pers.hal42.math.MathX;
import pers.hal42.text.Ascii;

/**
 * a safe to use (no gratuitous exceptions) NON expanding (after construction) byte array.
 */
public class Buffer {

  public int errorCount = 0;
  protected byte[] buffer;
  protected int writeptr = 0; //next available cursor==bytes present
  protected int alloc = 0;  //pre-allocated space
  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(Buffer.class);

  /**
   * constructor(s) hidden to allow for pooling.
   */
  protected Buffer(int alloc) {
    this.alloc = alloc < 0 ? 0 : alloc;
    buffer = new byte[this.alloc];
  }

  public boolean NonTrivial() {
    return buffer != null && used() > 0;
  }

  public int allocated() {
    return alloc;
  }

  /**
   * @return free space remaining
   */
  public int free() {
    return alloc - writeptr;
  }

  /**
   * @return whether there is space for @param need bytes.
   */
  public boolean haveSpaceFor(int need) {
    return free() >= need;
  }

  /**
   * @return bytes already in buffer
   */
  public int used() {
//    dbg.VERBOSE("used = " + writeptr);
    return writeptr;
  }

  /**
   * @return whether @param index is of an existing byte.
   */
  protected boolean inUse(int index) {
    return 0 <= index && index < used();
  }

  /** mark packet as having an error if  @param ok is <b> false </b>  */
  protected boolean anError(boolean ok) {
    if (!ok) {
      ++errorCount;
    }
    return ok;//pass-thru convenience
  }

  /**
   * mark packet as having an error
   */
  protected boolean anError() {
    return anError(false);//awkward looking here, but looks good at point of use
  }

  /////////////////////
  // access functions

  /**
   * @return copy of bytes from buffer @param start for @param length, NOT "end"
   * returns whatever is available, doesn't throw on arguments not in buffer
   */
  public byte[] extract(int start, int length) {
    if (start + length > used()) {//asking for data past end of input
      length = used() - start;//tweak to return what is here
    }
    if (length < 0) {//no data
      return new byte[0];//return empty, not null
    }
    byte[] toReturn = new byte[length];
    System.arraycopy(buffer, start, toReturn, 0, length);
    dbg.VERBOSE("Extracted " + Ascii.bracket(toReturn) + " from " + Ascii.bracket(buffer) + " for length " + length);
    return toReturn;
  }

  /**
   * @return copy of all bytes in packet
   */
  public byte[] packet() {
    byte[] packet = extract(0, used());
    dbg.VERBOSE("Returning packet:" + Ascii.bracket(packet));
    return packet;
  }

  /**
   * @return subset of bytes each converted to char
   * todo: check arguments! this guy will throw arrayaccess and such
   */
  public StringBuffer subString(int start, int length) {
    if (length >= 0 && inUse(start)) { //we will get something
      if (!inUse(start + length - 1)) {
        length = used() - start;
      }
      StringBuffer sub = new StringBuffer(length);
      sub.setLength(length);
      while (length-- > 0) {
        sub.setCharAt(length, (char) (bight(start + length)));
      }
      return sub;
    } else {
      return new StringBuffer(0);
    }
  }

  /**
   * @return a positive 8 bit value, unless there is an error in which case returns @see MathX.INVALIDINTEGER
   */
  public int bight(int index) {
    return inUse(index) ? (255 & ((int) buffer[index])) : MathX.INVALIDINTEGER;
  }

  /**
   * @return last bight in buffer, 256 if buffer empty. [+++ should return -1 if empty?]
   */
  public int last() {
    return bight(writeptr - 1);//: MathX.INVALIDINTEGER;//bogus value on error
  }
// end access functions
///////////////////////

  /**
   * zero the contents then @return 'this'
   */
  public Buffer reset() {
    errorCount = 0;
    while (writeptr-- > 0) {
      buffer[writeptr] = 0;
    }
    writeptr = 0;// in case writeptr somehow became negative.
    return this;
  }

  /**
   * @return success in putting byte into packet
   */
  public boolean append(byte b) {//often extended
    if (haveSpaceFor(1)) {
      buffer[writeptr++] = b;
      return true;
    } else {
      return anError();
    }
  }

  /**
   * @return true if index was ok
   */
  public boolean replace(int index, byte b) {
    if (inUse(index)) {
      buffer[index] = b;
      return true;
    } else if (index == used()) {
      return append(b);
    } else {
      return anError();
    }
  }

  /**
   * appends byte from the int. useful for when the int is from InputStream.read()
   */
  public boolean append(int n) {
    return n >= 0 && append((byte) n);
//+++ debate proper return, do we make this "end of packet" when given a negative number?
  }

  /**
   * append a string of ascii decimal or ascii hex
   * padder is added at end if the given string was odd in length
   */
  public boolean appendNibbles(String s, char padder) {
    if (haveSpaceFor((s.length() + 1) / 2)) {
      for (int i = 0; i < s.length(); ) {//#preserve order
        byte pack2 = (byte) ((s.charAt(i++) & 0xF) << 4);
        pack2 |= (byte) (((i < s.length() ? s.charAt(i++) : padder) & 0xF));
        append(pack2);
      }
      return true;
    }
    return false;
  }

  /**
   * @param end one past the last byte, like string.substring()
   * @param start first byte desired
   * @return true if all bytes were appended, object is UNMODIFIED on failure
   */
  public boolean append(byte[] ba, int start, int end) {
    if (haveSpaceFor(end - start)) {
      while (start < end) {
        append(ba[start++]);
      }
      return true;
    }
    return false;
  }

  public boolean append(byte[] ba, int start) {
    return append(ba, start, ba.length);
  }

  public boolean append(byte[] ba) {
    return append(ba, 0, ba.length);
  }

  public boolean append(String s) {
    return s == null || append(s.getBytes());
  }

  public boolean append(StringBuffer sb) {
    return sb == null || append(sb.toString());
  }

  public String toSpam() {
    return Ascii.bracket(packet());
  }

  /**
   * !must be overriden in each extension, if that extension has extra data items.
   */
  public Buffer Clone() {
    return new Buffer(this.alloc);
  }

  public static boolean NonTrivial(Buffer probate) {
    return probate != null && probate.NonTrivial();
  }

  public static Buffer New(int alloc) {
    return new Buffer(alloc);
  }

}
//$Id: Buffer.java,v 1.12 2003/07/27 05:34:56 mattm Exp $
