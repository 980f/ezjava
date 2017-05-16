package pers.hal42.transport;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

class PropertiesSet extends Hashtable<String, Properties> {
  public void addProps(String key, Properties props) {
    put(key, props);
  }

  public Properties getProps(String key) {
    return get(key);
  }

  public String[] getSetNames() {
    String names[] = new String[this.size()];
    int i = 0;
    for (Enumeration enumn = this.keys(); enumn.hasMoreElements(); ) {
      names[i++] = (String) enumn.nextElement();
    }
    return names;
  }
}


