package pers.hal42.database.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * A class that represents an ORDER BY clause in a SELECT.
 * <p>
 * An order by clause consists of a column and an optional DESC descending
 * clause.
 * <p>
 * Usually this class will not be referred to directly in an application.
 * More usually you will use DbSelector.addOrderBy().
 *
 * @author Chris Bitmead
 */

public class DbOrderBy extends DbExpr {
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

  @Override
  public int setSqlValues(PreparedStatement ps, int i) throws SQLException {
    return 0;
  }

  public StringBuilder getQueryString() {
    StringBuilder rtn = column.getQueryString();
    if (descending) {
      rtn.append(" DESC");
    }
    return rtn;
  }
}
