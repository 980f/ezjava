package pers.hal42.database.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;

/**
 * This expression has the result of putting parenthesis around another
 * expression. i.e. "expr" becomes "( expr )". It should be noted that you
 * won't usually use this class in your application code. Normally the order of
 * evaluation of the SQL is implied by the parethesis in your Java code. i.e.
 * <PRE>
 * DbExpr x = a.and((b.or(c)).and(d))
 * </PRE> This will automatically retain the Java evaluation order... <PRE>
 * A AND ( (B OR C) AND D)
 * </PRE>
 *
 * @author pcguest
 * @created October 18, 2001
 */

public class DbParenthesis extends DbExpr {
  DbExpr expr;

  public DbParenthesis(DbExpr expr) {
    super();
    this.expr = expr;
  }

  public int setSqlValues(PreparedStatement ps, int i) throws SQLException {
    return expr.setSqlValues(ps, i);
  }


  /**
   * Gets the queryString attribute of the DbParenthesis object
   *
   * @return The queryString value
   * @throws Exception Description of Exception
   */
  public StringBuilder getQueryString() {
    return "( " + expr.getQueryString() + " )";
  }


  /**
   * Description of the Method
   *
   * @param c Description of Parameter
   */
  public void usesTables(Set c) {
    expr.usesTables(c);
  }
}
