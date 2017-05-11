package pers.hal42.transport;

import java.util.Properties;
import java.util.Hashtable;
import java.util.Enumeration;

// I left it props since I don't know what possible values to code into classes
class PropertiesSet extends Hashtable {
  public void addProps(String key, Properties props) {
    put(key, props);
  }
  public Properties getProps(String key){
    return (Properties) get(key);
  }
  public String[] getSetNames() {
    String names[] = new String[this.size()];
    int i = 0;
    for(Enumeration enumn = this.keys(); enumn.hasMoreElements();) {
      names[i++] = (String)enumn.nextElement();
    }
    return names;
  }
  // +++ maybe make this thing give "smart" defaults?
}


