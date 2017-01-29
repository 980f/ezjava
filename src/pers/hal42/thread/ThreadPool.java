
package pers.hal42.util;

// +++ search for places that do "new Thread("
// +++ and replace with a function to get one from here

public class ThreadPool {

  public static final Thread newThread(Runnable toRun, String name) {
    Thread ret = null;
    // +++ first, wade through the pool looking for an unused thread
    // +++ next, mark the thread as used
    // +++ finally,
    return ret;
  }

  public ThreadPool() {
  }
}
