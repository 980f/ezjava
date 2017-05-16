package pers.hal42.database.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Inserts a piece of literal text within the SQL expression. This can be
 * useful for non-portable hacks into the SQL code.
 *
 * @author pcguest
 * @created October 18, 2001
 */

public class DbLiteral extends DbExpr {
  String str;

  public DbLiteral(String s) {
    super();
    str = s;
  }

  public int setSqlValues(PreparedStatement ps, int i) throws SQLException {
    return i;
  }


  /**
   * Gets the queryString attribute of the DbLiteral object
   *
   * @return The queryString value
   */
  public StringBuilder getQueryString() {
    return str;
  }
}
