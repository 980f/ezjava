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

  /** @returns this after appending @param joiners columns to the exist columns. */
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

  /** @returns a new list of column names */
  public TextList fieldNames() {
    TextList tl = new TextList();
    for(ColumnProfile item: columns) {
      tl.Add(item.name());
    }
    return tl;
  }

  /** @returns a <b>copy</b> of this table's columns. */
  public ColumnProfile [] columns() {
    ColumnProfile [] cols = new ColumnProfile [columns.length];
    System.arraycopy(columns, 0, cols, 0, columns.length);
    return cols;
  }

  public int numColumns() {
    return columns.length;
  }

  public int width() {
    if(columns != null) {
      int ret = 0;
      for(ColumnProfile item : columns) {
        ret += item.size();
      }
      return ret;
    } else {
      return -1;
    }
  }

  /**@returns column named @param fieldName, null if no such column or fieldName is null */
  public ColumnProfile column(String fieldName) {
    if(fieldName != null) {
      for(ColumnProfile column: columns) {
        if(fieldName.equalsIgnoreCase(column.name())) {
          return column;
        }
      }
      return null;//column not found
    } else {
      dbg.ERROR("fieldName = NULL !!!");
      return null;//bad argument
    }
  }

  /** @returns @param i-th column, null if i is bad */
  public ColumnProfile column(int i) {
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
    if(fieldName != null) {
      for(ColumnProfile column: columns) {
        if(fieldName.equalsIgnoreCase(column.name())) {
          return true;
        }
      }
      dbg.VERBOSE(name() + "." + fieldName + " does NOT exist.");
      return false;//not found
    } else {
      dbg.ERROR(fieldName + " = NULL !!!");
      return false;//bogus fieldname
    }
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
