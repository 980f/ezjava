package pers.hal42.database;

import pers.hal42.text.TextList;

/**
 * Title:        $Source: /cvs/src/net/paymate/database/IndexProfile.java,v $
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:      PayMate.net
 * @author PayMate.net
 * @version $Revision: 1.7 $
 */

public class IndexProfile {
  public String name = "uninitialized";
  public TableProfile table = null;
  public ColumnProfile [] fields = new ColumnProfile[0];
  public boolean unique = false;
  public QueryString whereclause = null;

  public IndexProfile(String name, TableProfile table, ColumnProfile [] fields) {
    this(name, table, fields, false);
  }

  public IndexProfile(String name, TableProfile table, ColumnProfile [] fields, boolean unique) {
    this(name, table, fields, unique, null);
  }

  public IndexProfile(String name, TableProfile table, ColumnProfile [] fields, boolean unique, QueryString whereclause) {
    this.name = name;
    this.table = table;
    this.fields = fields;
    this.unique = unique;
    this.whereclause = whereclause;
  }

  public IndexProfile(String name, TableProfile table, ColumnProfile field) {
    this(name, table, field, false);
  }

  public IndexProfile(String name, TableProfile table, ColumnProfile field, QueryString whereclause) {
    this(name, table, field, false, whereclause);
  }

  public IndexProfile(String name, TableProfile table, ColumnProfile field, boolean unique, QueryString whereclause) {
    this(name, table, new ColumnProfile[1], unique, whereclause);
    fields[0] = field; // array gets set in command's line's body
  }

  public IndexProfile(String name, TableProfile table, ColumnProfile field, boolean unique) {
    this(name, table, field, unique, null);
  }

  public TextList columnNames() {
    TextList tl = new TextList();
    for(int i = 0; i < fields.length; i++) {
      tl.add(fields[i].name());
    }
    return tl;
  }

  public String columnNamesCommad() {
    TextList tl = columnNames();
    return tl.asParagraph(",");
  }

}
