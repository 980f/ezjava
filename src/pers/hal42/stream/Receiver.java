package pers.hal42.stream;

import pers.hal42.text.Ascii;

/**
 * Title:        $Source: /cvs/src/net/paymate/serial/Receiver.java,v $
 * Description:  bitbucket implementation of serial data stream receiver
 * Copyright:    Copyright (c) 2001
 * Company:      PayMate.net
 *
 * @author PayMate.net
 * @version $Id: Receiver.java,v 1.11 2004/01/15 21:35:42 mattm Exp $
 */

// +++ move me to np.io package !!!

abstract public class Receiver {
  //////////////////
//psuedo bytes to send for exceptional cases
  public static final int EndOfInput = -1;  //see inputstream class
  public static final int TimedOut = -2;     //no data received
  public static final int ByteError = -3;    //parity error, framing error, overrun error
  public static final int PacketError = -4;
  public static final int Defective = -5;   //port is defective. you really should quit using it.

  /////////////////////////////////
  // timeout special values, for 'onByte()' return
  /**
   * may lock the receiver...
   */
  public static final int TimeoutNever = -10;
  /**
   * quit calling the receiver onByte()
   */
  public static final int TimeoutNow = -11;

  /**
   * @return timeout for next byte
   * synchronized with onTimeout
   */
  abstract public int onByte(int b);

  /**
   * called when a wad of bytes are available.
   */
  public int onBytes(byte[] b) {
    synchronized (this) {//ensure block is not interleaved with new input on other threads.
      int accum = 0;
      for (byte item : b) {//#PRESERVE order!
        accum = onByte(item & 255);//lookahead is of bytes alone, no codes will be mebedded
      }
      return accum;
    }
  }

  public static String imageOf(int eventcode) {
    switch (eventcode) {
      case EndOfInput:
        return "EndOfInput";
      case ByteError:
        return "ByteError";
      case PacketError:
        return "PacketError";
      case Defective:
        return "DefectivePort";
      case TimedOut:
        return "TimedOut";
      case TimeoutNever:
        return "TimeoutNever";
      case TimeoutNow:
        return "TimeoutNow";
    }
    return Ascii.bracket(eventcode < 0 ? Integer.toString(eventcode, 16) : Ascii.image(eventcode));
  }

}
//$Id: Receiver.java,v 1.11 2004/01/15 21:35:42 mattm Exp $
