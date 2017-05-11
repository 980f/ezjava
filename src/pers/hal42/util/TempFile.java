package pers.hal42.util;

import pers.hal42.lang.DateX;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.math.BaseConverter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

// !!!! NOTE: the temporary file will be deleted from storage when this object is destroyed!

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
      dbg.VERBOSE("Attempted to reaccess closed stream: " + filename());
      return null;
    }
    int maxretries=10;
    while(fos == null) {
      while(file == null) {
        // give it a new name and try to create it again
        try {
          file = File.createTempFile("paymate", BaseConverter.itoa(DateX.Now().getTime())); // very random
          fos = new FileOutputStream(file);
        } catch (Exception ignored) {
          if(--maxretries<=0){
            dbg.ERROR("Abandoned trying to create a temporary file.");
            return null;
          }
          dbg.ERROR("Exception trying to create a temporary file.  Trying again ...");
          dbg.Caught(ignored);
        }
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
      file.delete();
    }
  }

}
