/**
 * Title:        TableProfile
 * Description:  Contains information for a table, as used by database profiling, etc.
 * Copyright:    Copyright (c) 2000
 * Company:      PayMate.net
 * @author       PayMate.net
 * @version      $Id: TableProfile.java,v 1.33 2005/03/19 15:59:04 andyh Exp $
 */

package pers.hal42.database;

import pers.hal42.lang.ObjectX;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.text.TextList;

import java.util.Arrays;

// +++ @@@ %%% Get when reading existing info:
//    Primary keys
//    Foreign keys
//    Index

public class TableProfile implements Comparable {
  protected static final ErrorLogStream dbg = ErrorLogStream.getForClass(TableProfile.class);


  private TableInfo ti = null;
  protected ColumnProfile columns[] = null;
  public PrimaryKeyProfile primaryKey = null;
  public ForeignKeyProfile [] foreignKeys = null;
  public IndexProfile [] indexes = null;
  public TableType type = null;
  // Coded profiles enter here ...
  protected TableProfile(TableInfo ti, TableType type, ColumnProfile [] columns) {
    this.ti = ti;
    this.columns = columns;
    if(type != null) {
      this.type = type;
    }
  }

  public static TableProfile create(TableInfo ti, TableType type, ColumnProfile [] columns) {
    TableProfile tp = new TableProfile(ti, type, columns);
    return tp;
  }

  public TableProfile join(ColumnProfile [] joiners) {
    ColumnProfile [] merger= new ColumnProfile [columns.length+joiners.length] ;
    System.arraycopy(columns,0,merger,0,columns.length);
    System.arraycopy(joiners,0,merger,columns.length,joiners.length);
    columns=merger;
    return this;
  }

  public String fullname() {
    // for now, but +++ need to make it a child of a database object and really make a full name!
    return name();
  }

  public String name() {
    return ti.name();
  }

  public String all() {
    return ti.name()+".*";
  }

  public TextList fieldNames() {
    TextList tl = new TextList();
    for(int i = 0; i < columns.length; i++) {
      tl.Add(columns[i].name());
    }
    return tl;
  }

  public ColumnProfile [] columns() {
    ColumnProfile [] cols = new ColumnProfile [columns.length];
    System.arraycopy(columns, 0, cols, 0, columns.length);
    return cols;
  }

  public int numColumns() {
    return columns.length;
  }

  public int width() {
    int ret = -1;
    if(columns != null) {
      ret = 0;
      for(int i = columns.length; i-->0;) {
        ret += columns[i].size();
      }
    }
    return ret;
  }

  public ColumnProfile column(String fieldName) {
    ColumnProfile ret = null;
    if(fieldName != null) {
      for(int i = 0; i < columns.length; i++) {
        if(fieldName.equalsIgnoreCase(columns[i].name())) {
          ret = columns[i];
          break;
        }
      }
    } else {
      dbg.ERROR("fieldName = NULL !!!");
    }
    return ret;
  }

  public ColumnProfile column(int i) {
    // +++ bitch on errors
    return ((i >=0) && (i < columns.length)) ? columns[i] : null;
  }
  /**
   * SQL-92 says that database field names are NOT case-sensitive ...
   * American National Standard X3.135-1992, pg 69,
   * 5.2 Lexical elements / <token> and <separator>, Syntax Rules #10:
   * The <identifier body> of a <regular identifier> is equivalent to an <identifier body> in which
   * every letter that is a lower-case letter is replaced by the equivalent upper-case letter or letters.
   * This treatment includes determination of equivalence, representation in the Information and
   * Definition Schemas, representation in the diagnostics area, and similar uses.
   */
  public boolean fieldExists(String fieldName) {
    boolean ret = false;
    if(fieldName != null) {
      for(int i = 0; i < columns.length; i++) {
        if(fieldName.equalsIgnoreCase(columns[i].name())) {
          ret = true;
          break;
        }
      }
    } else {
      dbg.ERROR(fieldName + " = NULL !!!");
    }
    dbg.VERBOSE(name() + "." + fieldName + " does " + (ret ? "" : "NOT ") + "exist.");
    return ret;
  }

  public boolean isLogType() {
    return type != null && type ==TableType.log;
  }

  public void sort() {
    Arrays.sort(columns);
  }

  public final String toString() {
    return name();
  }

  public int compareTo(Object o) {
    int i = 0;
    if(ObjectX.NonTrivial(o)) {
      try {
        TableProfile tp = (TableProfile) o;
        i = name().compareTo(tp.name());
      } catch (Exception e) {
        dbg.ERROR("Compared different types!");
      }
      return i;
    } else {
      return 1; // this is bigger than null
    }
  }
}
//$Id: TableProfile.java,v 1.33 2005/03/19 15:59:04 andyh Exp $
