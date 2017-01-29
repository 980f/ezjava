package pers.hal42.util;
import  java.io.*;

// !!!! NOTE: the temporary file will be deleted from storage when this object is destroyed!

public class TempFile {

  private static final ErrorLogStream dbg = new ErrorLogStream(TempFile.class.getName());
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
          file = File.createTempFile("paymate", BaseConverter.itoa(Safe.Now().getTime())); // very random
          if(file!=null) {
            fos = new FileOutputStream(file);
          }
        } catch (Exception t) {
          dbg.ERROR("Exception trying to create a temporary file.  Trying again ...");
          dbg.Caught(t);
        }
        // +++ make this try several times but not forever
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
      file.delete(); // ignore return value for now
    }
  }

}
