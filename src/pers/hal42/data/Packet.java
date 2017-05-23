package pers.hal42.data;

import pers.hal42.lang.StringX;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.math.MathX;
import pers.hal42.text.Ascii;
import pers.hal42.text.Formatter;
import pers.hal42.text.Fstring;
import pers.hal42.text.TextList;

/**
 * wraps a byte array with methods common to serial communications.
 * <p>
 * This class doesn't synchronize stuff because it is intended to be used sequentially by one agent at a time.
 * It is filled by an agent who WHEN COMPLETE hands it to an agent that reads it, generally without modification.
 */
public class Packet {
  /////////////
  public int errorCount = 0;
  protected byte[] buffer = new byte[0];
  protected int nexti = 0; //next available cursor==bytes present
  protected boolean ended = false;
  protected int size = 0;  //pre-allocated space
  int instance = 0;
  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(Packet.class);
  //for comparing instances, don't want to trust object compare. // --- this shouldn't be necessary; test to see
  static int instanceCounter = 0;

  public Packet(int maxsize) {
    start(maxsize);
    instance = ++instanceCounter;
  }

  public int Size() {
    return size;
  }

  /**
   * @return free space remaining
   */
  protected int space() {
    return size - nexti;
  }

  /**
   * @return their is space for @param need bytes.
   */
  protected boolean haveSpaceFor(int need) {
    return space() >= need;
  }

  /**
   * @return bytes already in buffer
   */
  public int ptr() {
    return nexti;
  }

  /**
   * @return we have the number of bytes expected or allowed.
   */
  public boolean hasEnded() {//use instead of 'isComplete' for non-lrc uses
    return ended;
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

  /**
   * @return copy of bytes from buffer @param start for @param length, NOT "end"
   */
  public byte[] extract(int start, int length) {
    if (start + length > nexti) {//asking for data past end of input
      length = nexti - start;//tweak to return what is here
    }
    if (length < 0) {//no data
      return new byte[0];//return empty, not null
    }
    byte[] toReturn = new byte[length];
    System.arraycopy(buffer, start, toReturn, 0, length);
    return toReturn;
  }

  /**
   * @return copy of all bytes in packet
   */
  public byte[] packet() {
    return extract(0, nexti);
  }

  public StringBuffer subString(int start, int length) {
//+++ check arguments!
    //byte to string conversion happens here!
    StringBuffer sub = new StringBuffer(length);
    sub.setLength(length);
    while (length-- > 0) {
      sub.setCharAt(length, (char) (bight(start + length)));
    }
    return sub;
  }

  public void reset() {
    ended = false;
    errorCount = 0;
    while (nexti-- > 0) {
      buffer[nexti] = 0;
    }
    nexti = 0;
  }

  /**
   * reinitialize buffer for new length
   * @return true if succeeded at allocation
   */
  public boolean start(int size) {
    if (this.size < size) {
      buffer = new byte[size];
      this.size = buffer.length;
      nexti = 0;
    }
    reset();
    return size > 0;//mostly just so that other extensions can return a status on their starts.
  }

  /**
   * @param content becomes content of completed buffer
   */
  protected boolean stuff(byte[] content) {
    reset();//prophylactic
    if (content != null) {
      int size = content.length;
      if (this.size < size) {//reallocate at need
        buffer = new byte[this.size = size];
      }
      System.arraycopy(content, 0, buffer, 0, nexti = size);
    }
    ended = true; //so that if we ignore the return from this hopefully loops will still terminate
    return size > 0;
  }

  /**
   * @return success in putting byte into packet
   * usually overridden for things like etx detection and incremental lrc computation.
   */
  public boolean append(byte b) {//often extended
    dbg.Push("Packet.append");//#gc
    try {
      if (!ended) {
        if (nexti < size) {//haveSpaceFor(1);
          buffer[nexti++] = b;
//          dbg.VERBOSE("added to "+Ascii.image(buffer,0,nexti));
          return true;
        } else {
          dbg.ERROR("no room!");
          ended = true; //so that if we ignore the return from this hopefully loops will still terminate
          return anError();
        }
      } else {
        return false;
      }
    } finally {
      dbg.Exit();//#gc
    }
  }

  public boolean replace(int index, byte b) {
    if (0 <= index && index < nexti) {//if there is such a position
      buffer[index] = b;
      return true;
    } else if (index == nexti) {//else it might be right after the end of what we have so far
      return append(b);
    } else {//caller is beign stupid.
      return anError();
    }
  }

  /**
   * @return a positive 8 bit value, unless there is an error in which case returns @see MathX.INVALIDINTEGER (-1)
   */
  public int bight(int index) {
    try {
      return 255 & ((int) buffer[index]);
    } catch (Exception any) {
      return MathX.INVALIDINTEGER;
    }
  }

  /**
   * @return last bight in buffer, 256 if buffer empty. [+++ should return -1 if empty?]
   */
  public int last() {
    return nexti > 0 ? bight(nexti - 1) : 256;//bogus value on error
  }

  /**
   * appends byte from the int. useful for when the int is from InputStream.read()
   */
  public boolean append(int n) {
    if (n >= 0) {
      return append((byte) n);
    } else {
      return false;//+++ debate proper return, do we make this "end of packet"?
    }
  }

  /**
   * append a string of ascii decimal or ascii hex
   * padder is added at end if the given string was odd in length
   */
  public boolean appendNibbles(String s, char padder) {
    if (haveSpaceFor((s.length() + 1) / 2)) {
      for (int i = 0; i < s.length(); ) {
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

  protected boolean end() {
    dbg.VERBOSE("setting ended flag");
    ended = true;
    return true;
  }

  public boolean isOk() {
    return buffer != null;
  }

  public String toSpam(int clip) {
    StringBuffer readable = new StringBuffer(buffer.length);
    readable.append("buffer[#").append(instance).append("]:");
    if (clip > nexti) {
      clip = nexti;
    }
    for (int i = 0; i < clip; i++) {
      readable.append(" ").append(Formatter.ox2(buffer[i]));
    }
    return String.valueOf(readable);
  }

  public String toSpam() {
    return toSpam(last());
  }

  /**
   * @return true if no more bytes will be accepted
   */
  public boolean isComplete() {
    return !haveSpaceFor(1);
  }

  /**
   * text type fixed length fields are left justified blank extended
   */
  public Packet appendAlpha(int size, String text) {
    append(Fstring.fill(text, size, ' '));
    return this;
  }

  /**
   * number type fixed length fields are UNSIGNED right justified zero extended
   */
  public Packet appendInt(int fixedsize, int number) {
    appendNumeric(fixedsize, Integer.toString(number));
    return this;
  }

  public Packet appendLong(int fixedsize, long number) {
    appendNumeric(fixedsize, Long.toString(number));
    return this;
  }

  /**
   * @param size is the number of digits in the fixed length field
   * @param digits is a string of digits
   * if more digits than field then leading zeroes are clipped
   * if still more digits than field then LEADING digits are lost. probably should restore throwing of exception...
   * if fewer digits than field, field is LEFT filled with zeroes.
   */
  public Packet appendNumeric(int size, String digits) {
    digits = StringX.OnTrivial(digits, "0"); //not our job to complain about bad values
    int overflow = digits.length() - size;
    //+++ check digits for non-decimal characters, we have that somewhere....
    if (overflow > 0) {
      dbg.ERROR("too many digits:size=" + size + " number=" + digits);
      //try to strip leading zeroes
      String clip = digits.substring(0, overflow);
      for (int i = overflow; i-- > 0; ) {
        if (clip.charAt(i) != '0') {
//          throw new IllegalArgumentException();
          dbg.ERROR("Loss of precision:" + clip);
          anError(); //mark as bad but proceed to simplify the debug cycle of the user of this class.
          break;
        }
      }
      append(digits.substring(overflow));
    } else {
      append(Fstring.righted(digits, size, '0'));
    }
    return this;
  }

  ///////////////
  public TextList dump(TextList spam) {
    return dump(spam, false);
  }

  public TextList dump(TextList spam, boolean entireBuffer) {
    if (spam == null) {
      spam = new TextList();
    }
    spam.add("packet.Size()", Size());
    spam.add("packet.used", nexti);
    spam.add("packet.ended", ended);
    spam.add("packet.errCnt", errorCount);
    spam.add("packet.isComplete()", isComplete());
    spam.add("packet.hasEnded()", hasEnded());
    spam.add("packet.isOk()", isOk());
    if (entireBuffer) {
      spam.add("entirebuffer", Ascii.bracket(this.buffer)); // too many nulls!
    } else {
      spam.add("usedbuffer", Ascii.bracket(packet()));
    }
    return spam;
  }

}
//$Id: Packet.java,v 1.21 2003/11/08 07:55:32 mattm Exp $
