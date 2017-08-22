package pers.hal42.transport;

import java.util.Properties;

public class PropertyCursor extends TreeName {
  /** raw access, handy with inline construction. */
  public Properties wrapped;
  /** used by data sources, not used internally */
  public boolean enumsAsSymbol = true;
  /** wehtehr to do String.valueof() around all put items */
  public boolean stringify = true;

  public PropertyCursor(Properties wrapped, boolean stringify) {
    super(".");
    this.wrapped = wrapped;
    this.stringify = stringify;
  }

  public String getProperty(String key, String onMissing) {
    final String prop = wrapped.getProperty(leaf(key));
    return prop != null ? prop : onMissing;
  }

  /** @returns @param value after putting it into the properties. */
  public String setProperty(String key, String value) {
    wrapped.setProperty(leaf(key), value);
    return value;
  }

  /** @returns @param value after putting it into the properties. */
  public Object putProperty(String key, Object value) {
    if (stringify) {
      setProperty(key, String.valueOf(value));
    } else {

      wrapped.put(leaf(key), value);
    }
    return value;
  }

  @SuppressWarnings("unchecked")
  public <V> V pullProperty(String key, V onMissing) {
    final Object prop = wrapped.get(leaf(key));
    return prop != null ? (V) prop : onMissing;
  }

}
