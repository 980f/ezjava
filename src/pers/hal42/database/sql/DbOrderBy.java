package pers.hal42.database.sql;

/**
 * A class that represents an ORDER BY clause in a SELECT.
 *
 * An order by clause consists of a column and an optional DESC descending
 * clause.
 *
 * Usually this class will not be referred to directly in an application.
 * More usually you will use DbSelector.addOrderBy().
 *
 * @author Chris Bitmead
 */

public class DbOrderBy {
  boolean descending;
  DbExpr column;

  public DbOrderBy(DbExpr column, boolean descending) {
    this.column = column;
    this.descending = descending;
  }

  /**
   * Is this clause descending order?
   */
  public boolean getDescending() {
    return descending;
  }

  /**
   * The column to sort on.
   */
  public DbExpr getColumn() {
    return column;
  }

  public String getQueryString() {
    String rtn = column.getQueryString();
    if (descending) {
      rtn += " DESC";
    }
    return rtn;
  }
}
