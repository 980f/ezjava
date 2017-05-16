package pers.hal42.thread;

import pers.hal42.lang.Monitor;
import pers.hal42.logging.ErrorLogStream;

public class LibraryBook {

  private boolean value = false;
  private Monitor checkoutMutex = new Monitor("checkoutMonitor_" + mutexCounter.incr());
  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(LibraryBook.class);
  private static Counter mutexCounter = new Counter(); // just for naming and counting

  public LibraryBook() {
  }

  // for standins
  public boolean Checkout() {
    boolean gotit = false;
    checkoutMutex.getMonitor();
    try {
      if (!value) {
        value = true;
        gotit = true;
      }
    } catch (Exception e) {
      dbg.Caught(e);
    } finally {
      checkoutMutex.freeMonitor();
      return gotit;
    }
  }

  public void Return() {
    checkoutMutex.getMonitor();
    try {
      if (value) {
        value = false;
      } else {
        // +++ ???
      }
    } catch (Exception e) {
      dbg.Caught(e);
    } finally {
      checkoutMutex.freeMonitor();
    }
  }

  public boolean isCheckedOut() {
    return value;
  }

}
