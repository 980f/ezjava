package pers.hal42.database.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;

/**
 * An SQL expression of the form EXPRESSION OPERATOR EXPRESSION
 *
 * @author Chris Bitmead
 */

public class DbCriterion extends DbExpr {
  Object c1;
  String op;
  Object c2;

  public DbCriterion(Object c1, String op, Object c2) {
    super();
    this.c1 = c1;
    this.op = op;
    this.c2 = c2;
  }

  public StringBuilder getQueryString() {
    return new StringBuilder(100).append("(").append(getString(c1)).append(" ").append(op).append(" ").append(getString(c2)).append(")");
  }

  public int setSqlValues(PreparedStatement ps, int i) throws SQLException {
    i = setSqlValue(ps, i, c1, null);
    i = setSqlValue(ps, i, c2, null);
    return i;
  }

  public void usesTables(Set c) {
    usesTables(c, c1);
    usesTables(c, c2);
  }
}
