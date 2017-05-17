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

    try (AutoCloseable free = checkoutMutex.getMonitor()) {
      if (!value) {
        value = true;
        return true;
      } else {
        return false;
      }
    } catch (Exception e) {
      dbg.Caught(e);
      return false;
    }
  }

  public boolean Return() {
    try (AutoCloseable free = checkoutMutex.getMonitor()) {
      if (value) {
        value = false;
        return true;
      } else {
        return false;
      }
    } catch (Exception e) {
      dbg.Caught(e);
      return false;
    }
  }

  public boolean isCheckedOut() {
    return value;
  }

}
