package pers.hal42.database;


import pers.hal42.transport.EasyProperties;

import java.lang.reflect.Method;
import java.util.Vector;

class TableRowCleaner {
  TableProfile table;
  int maxid;
  Method m;
  boolean hasFunction;
  boolean hasDefaults;
  ColumnProfile pk;
  EasyProperties newDefaults;
}

class TableRowCleanerVector extends Vector {
  public TableRowCleaner itemAt(int index) {
    return(TableRowCleaner) elementAt(index);
  }
}
