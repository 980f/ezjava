/*
 * $Id: URLDecoderFilterOutputStream.java,v 1.4 2001/05/22 19:13:35 mattm Exp $
 *
 * Copyright 2000 PayMate.Net, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of PayMate.Net, Inc.
 * Use is subject to license terms.
 *
 */

package pers.hal42.stream;

import pers.hal42.lang.Monitor;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * The class implements a URL decoder FilterOutputStream. By setting up such
 * an output stream, an application can write bytes to the underlying
 * output stream in URL-decoded format
 *
 * NOTE: can probably replace all super.write(whatever) with out.append(whatever)
 *
 */



public class URLDecoderFilterOutputStream extends FilterOutputStream {

  protected StringBuffer tmpBuff = new StringBuffer(3); // really shouldn't use "String" anything
  private Monitor tmpBuffMonitor = new Monitor("tmpBuff");

  /**
   * Creates a new URLDecoded output stream to write data to the
   * specified underlying output stream.
   *
   * @param   out   the underlying output stream.
   */
  public URLDecoderFilterOutputStream(OutputStream out) {
    super(out);
  }

  public void flush() throws IOException {
    try {
      tmpBuffMonitor.getMonitor();
      // if anything is left in teh buffer, it is not a valid format,
      // so spew it as-is
      // +++ in that case, should we except?
      for(int i = 0; i < tmpBuff.length(); i++) {
        super.write(tmpBuff.charAt(i));
      }
      tmpBuff.setLength(0);
      super.flush();  // +++ should be out.flush()?
    } finally {
      tmpBuffMonitor.freeMonitor();
    }
  }

  /**
   * Writes the specified byte to this URLDecoded output stream.
   *
   * @param      c   the character as ascii to be written.
   * @exception  IOException  if an I/O error occurs.
   */
  public synchronized void write(int c) throws IOException {
    try {
      tmpBuffMonitor.getMonitor();
      if((c == '%') || (tmpBuff.length() > 0)) {
        tmpBuff.append(c);
        if(tmpBuff.length() == 3) {
          // prime for decoding
          int chr;
          try {
            super.write(Integer.parseInt(tmpBuff.substring(1),16));
          } catch (NumberFormatException e) {
            // if it is misformatted, just spew as normal
            flush();
          } finally {
            tmpBuff.setLength(0);
          }
        }
      } else {
        if(c == '+') {
          c = ' ';
        }
        super.write((byte)c);
      }
    } finally {
      tmpBuffMonitor.freeMonitor();
    }
  }
}
