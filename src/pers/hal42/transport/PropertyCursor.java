package pers.hal42.transport;

import pers.hal42.lang.Finally;

import java.util.Properties;

public class PropertyCursor implements Finally.Lambda {
  private final static String Sep = ".";
  /** raw access, handy with inline construction. */
  public Properties wrapped;
  protected StringBuilder cursor = new StringBuilder(100);
  private int lastdot = 0;
  private Finally popper = new Finally(this);
  /** used by data sources, not used internally */
  public boolean enumsAsSymbol = true;
  public PropertyCursor(Properties wrapped) {
    this.wrapped = wrapped;
  }

  public String getProperty(String key, String onMissing) {
    try {
      cursor.append(key);
      final String prop = wrapped.getProperty(cursor.toString());
      return prop != null ? prop : onMissing;
    } finally {
      clip();
    }
  }

  public String setProperty(String key, String value) {
    try {
      cursor.append(key);
      wrapped.setProperty(cursor.toString(), value);
      return value;
    } finally {
      clip();
    }
  }

  public Object putProperty(String key, Object value) {
    try {
      cursor.append(key);
      wrapped.put(cursor.toString(), value);
      return value;
    } finally {
      clip();
    }
  }

  private void clip() {
    cursor.setLength(lastdot >= 0 ? lastdot : 0);
  }

  Finally push(String more) {
    cursor.append(more).append(Sep);
    lastdot = cursor.length();
    return popper;
  }

  @Override
  public void pop() {
    lastdot = 1 + cursor.lastIndexOf(Sep, lastdot - 2);
    clip();
  }
}
