package pers.hal42.database;


import pers.hal42.transport.EasyProperties;

import java.lang.reflect.Method;

class TableRowCleaner {
  TableProfile table;
  int maxid;
  Method m;
  boolean hasFunction;
  boolean hasDefaults;
  ColumnProfile pk;
  EasyProperties newDefaults;
}

