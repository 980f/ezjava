package pers.hal42.stream;


import pers.hal42.logging.ErrorLogStream;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static pers.hal42.math.MathX.INVALIDINTEGER;

public class NiceInputStream {
  //just enough pushback for undoing bitstream damage.
  byte[] pushback = new byte[3];
  int pushed = 0;//==empty
  //extendors should think twice about changing these to protected...try to use existing functions.
  private boolean bigendian;
  private InputStream wrapped;
  //bit stream reading
  private int bitbuff = 0;
  private int bitPtr = 32;//past last bit, ensures load on first call to signedField
  private static ErrorLogStream dbg;//set to concrete class in constructor

  public NiceInputStream(byte[] data, boolean msbfirst) {
    this(new ByteArrayInputStream(data), msbfirst);
  }

  public NiceInputStream(InputStream is, boolean msbfirst) {
    wrapped = is;
    bigendian = msbfirst;
    dbg = ErrorLogStream.getForClass(this.getClass());
  }

  /**
   * make a nice stream with system's endedness
   */
  public NiceInputStream(InputStream is) {
    this(is, !littleEndedSystem());
  }

  public boolean pushBack(byte b) {
    dbg.WARNING("Pushback:" + b + " [" + pushed + "]");
    if (pushed < pushback.length) {
      pushback[pushed++] = b;
      return true;
    }
    return false;
  }

  public NiceInputStream changeEndian(boolean bigend) {
    bigendian = bigend;
    return this;
  }

  /**
   * blow off given number of bytes of input
   */
  public NiceInputStream skip(int count) {
    try {
      flushBits();
      count -= pushed;
      if (count > 0) {
        //noinspection ResultOfMethodCallIgnored
        wrapped.skip(count);
        pushed = 0;
      } else {
        pushed -= count;
      }
    } catch (java.io.IOException ignored) {
      //don't care if we go off the end of input.
    }
    return this;

  }

  /**
   * @return internal stream
   */
  public InputStream is() {
    return wrapped;
  }

  /**
   * @return unsigned 8 bits, MathX.INVALIDINTEGER if error on stream
   */
  public int unsigned8() {
    try {
      if (pushed > 0) {
        return pushback[--pushed];//+_+ could use a byte stack...
      } else {
        return wrapped.available() > 0 ? wrapped.read() : 0;
      }
    } catch (Exception ex) {
      return INVALIDINTEGER;
    }
  }

  /**
   * @return signed 8 bits, MathX.INVALIDINTEGER if error on stream
   */
  public int signed8() {
    int rawbyte = unsigned8();
    return rawbyte >= 128 ? rawbyte - 255 : rawbyte;// passes MathX.INVALIDINTEGER unchanged
  }

  /**
   * @return unsigned 16 bits with supplied endedness
   * some protocols actually use both endednesses within them, the bastards!
   */
  public int u16(boolean bigendian) {
    if (bigendian) {//override's this's
      return (unsigned8() << 8) + unsigned8();
    } else {
      return unsigned8() + (unsigned8() << 8);
    }
  }

  /**
   * @return unsigned 16 bits with stream's endedness
   */
  public int u16() {
    return u16(bigendian);
  }

  /**
   * @return unsigned 16 bits
   */
  public int s16() {
    int usigned = u16();
    return usigned >= 65536 ? usigned - 65536 : usigned;
  }

  /**
   * @deprecated untested and probably buggy
   */
  public int signedField(int numbits) {
    int piece;
    piece = bitbuff << bitPtr;//if bitPtr>=32 then this yields 0
    bitPtr += numbits;
    if (bitPtr > 32) {//then we need some bits from next word
      bitbuff = u32(true);//alwasy do this bigendian, without affecting stream setting
      bitPtr -= 32;
      piece |= bitbuff >>> (numbits - bitPtr);
    }
    piece >>= (32 - numbits); //&= ~((1<<bitPtr)-1);
    return piece;
  }

  public void flushBits() {
    //remove fraction of a byte
    int bytesFree = 4 - ((bitPtr + 7) >> 3);
    dbg.WARNING("flushBits:" + bitPtr + " " + bytesFree);
    // now points to msb of first unused byte in bitbuff
    while (bytesFree-- > 0) {
      pushBack((byte) bitbuff);
      bitbuff >>= 8;
    }
    bitPtr = 32;
  }

  /**
   * @deprecated should be simple....but doesn't seem to work.
   */
  public int bitsAvailable() {
    int bitsonhand = (32 - bitPtr);
    try {
      return bitsonhand + 8 * wrapped.available();
    } catch (java.io.IOException dammit) {
      //won't ever happen but compiler is dumb (available() never actually throws)
    } finally {
      return bitsonhand;
    }
  }

  /**
   * @return 32 bits with stream's endedness
   */
  public int u32() {
    return u32(bigendian);
  }

  /**
   * @return 32 bits with given endedness
   */
  public int u32(boolean bigendian) {
    int first = u16(bigendian);
    if (first != INVALIDINTEGER) {
      int second = u16(bigendian);
      if (second != INVALIDINTEGER) {
        if (bigendian) {//overrides this's
          return (first << 16) + second;
        } else {
          return first + (second << 16);
        }
      }
    }
    return INVALIDINTEGER;
  }

  /**
   * return the jvm's opinion of what endedness data is likely to have on this platform
   */
  public static boolean littleEndedSystem() {
    return System.getProperty("sun.cpu.endian", "little").equals("little");
  }

}
