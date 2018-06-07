package org.apache.commons.logging;

public class LogFactory {

  public static Log getLog(Class aClass) {
    return new Log(aClass);
  }
}
