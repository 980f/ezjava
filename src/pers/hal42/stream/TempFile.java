package pers.hal42.stream;

import pers.hal42.lang.DateX;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.math.BaseConverter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class TempFile {

  protected String fname = "";
  protected File file = null;
  protected FileOutputStream fos = null;
  private boolean closed = false;
  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(TempFile.class);

  public String filename() {
    return fname;
  }

  public OutputStream outputStream(String extra) {
    if (closed) {
      dbg.VERBOSE("Attempting to reaccess closed stream: " + filename());
      return null;
    }
    while (fos == null) {
      while (file == null) {
        // give it a new name and try to create it again
        try {
          file = File.createTempFile(extra, BaseConverter.itoa(DateX.Now().getTime())); // very random
          fos = new FileOutputStream(file);
        } catch (Exception t) {
          dbg.ERROR("Exception trying to create a temporary file. ");// Trying again ...");
          dbg.Caught(t);
        }
      }
      fname = file.getPath();
      file.deleteOnExit(); // in case we miss it for some reason (crash?)
    }
    return fos;
  }

  public void close() {
    if (!closed) {
      IOX.Close(fos);
    }
  }

  public void finalize() {
    close();
    if (file != null) {
      //noinspection ResultOfMethodCallIgnored
      file.delete(); // ignore return value for now
    }
  }

}
