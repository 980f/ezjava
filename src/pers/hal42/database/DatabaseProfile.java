package pers.hal42.database;

import pers.hal42.lang.StringX;

import java.util.Vector;

/**
 * DOM of a database.
 */
public class DatabaseProfile extends Vector<TableProfile> {
  private String name = null;

  public DatabaseProfile(String name) {
    this.name = name;
  }

  public TableProfile itemAt(int index) {
    return elementAt(index);
  }

  public String name() {
    return name;
  }

  /**
   * @returns table profile for @param tablename, null if not found.
   */
  public TableProfile tableFromName(String tablename) {
    for (TableProfile tp : this) {
      if ((tp != null) && StringX.equalStrings(tp.name(), tablename, true/*ignoreCase*/)) {
        return tp;
      }
    }
    return null;
  }
}
