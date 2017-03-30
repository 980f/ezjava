/**
 * Title:        TempFile<p>
 * Description:  Handles termporary files<p>
 * Copyright:    2000<p>
 * Company:      PayMate.net<p>
 * @author       PayMate.net
 * @version      $Id: TempFile.java,v 1.1 2003/07/27 19:36:56 mattm Exp $
 */
package pers.hal42.stream;

import pers.hal42.lang.DateX;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.util.BaseConverter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class TempFile {

  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(TempFile.class);
//+++ where is the contructor?  Can we use this anywhere?
// +++ you need to create a constructor and pass it a path.  This class has never been tested.
  protected File file = null;
  protected FileOutputStream fos = null;
  private boolean closed = false;
  protected String fname = "";

  public String filename() {
    return fname;
  }

  public OutputStream outputStream() {
    if(closed) {
      dbg.VERBOSE("Attempting to reaccess closed stream: " + filename());
      return null;
    }
    while(fos == null) {
      while(file == null) {
        // give it a new name and try to create it again
        try {
          file = File.createTempFile("paymate", BaseConverter.itoa(DateX.Now().getTime())); // very random
          fos = new FileOutputStream(file);
        } catch (Exception t) {
          dbg.ERROR("Exception trying to create a temporary file. ");// Trying again ...");
          dbg.Caught(t);
        }
        //todo: make this try several times but not forever
      }
      fname = file.getPath();
      file.deleteOnExit(); // in case we miss it for some reason (crash?)
    }
    return fos;
  }

  public void close() {
    if(!closed) {
      if(fos != null) {
        try {
          fos.close();
        } catch (Exception t) {
          // who cares?  stub
        }
      }
    }
  }

  public void finalize() {
    close();
    if(file != null) {
      //noinspection ResultOfMethodCallIgnored
      file.delete(); // ignore return value for now
    }
  }

}
