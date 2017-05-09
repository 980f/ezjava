package pers.hal42.database;

/**
 * Title:        $Source: /cvs/src/net/paymate/database/Constraint.java,v $
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:      PayMate.net
 * @author PayMate.net
 * @version $Revision: 1.2 $
 */

public class Constraint {

  public String name = "uninitialized";
  public TableProfile table = null;
  public ColumnProfile field = null;

  public Constraint(String name, TableProfile table, ColumnProfile field) {
    this.name = (name != null) ? name.toLowerCase() : name;
    this.table = table;
    this.field = field;
  }
}
