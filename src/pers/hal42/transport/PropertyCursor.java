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
  /** wehtehr to do String.valueof() around all put items */
  public boolean stringify = true;

  public PropertyCursor(Properties wrapped, boolean stringify) {
    this.wrapped = wrapped;
    this.stringify = stringify;
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

  /** @returns @param value after putting it into the properties. */
  public String setProperty(String key, String value) {
    try {
      cursor.append(key);
      wrapped.setProperty(cursor.toString(), value);
      return value;
    } finally {
      clip();
    }
  }

  /** @returns @param value after putting it into the properties. */
  public Object putProperty(String key, Object value) {
    if (stringify) {
      setProperty(key, String.valueOf(value));
    } else {
      try {
        cursor.append(key);
        wrapped.put(cursor.toString(), value);
      } finally {
        clip();
      }
    }
    return value;
  }

  @SuppressWarnings("unchecked")
  public <V> V pullProperty(String key, V onMissing) {
    try {
      cursor.append(key);
      final Object prop = wrapped.get(cursor.toString());
      return prop != null ? (V) prop : onMissing;
    } finally {
      clip();
    }
  }

  private void clip() {
    cursor.setLength(lastdot >= 0 ? lastdot : 0);
  }

  /** @returns an autoclosable that will call pop, so that you can use try-with-resources to keep the conceptual name stack sane. */
  public Finally push(String more) {
    cursor.append(more).append(Sep);
    lastdot = cursor.length();
    return popper;
  }

  @Override //Finally
  public void pop() {
    lastdot = 1 + cursor.lastIndexOf(Sep, lastdot - 2);
    clip();
  }
}
