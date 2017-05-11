package pers.hal42.database.sql;
import java.sql.*;
import java.util.*;

/**
 *  This expression has the result of putting parenthesis around another
 *  expression. i.e. "expr" becomes "( expr )". It should be noted that you
 *  won't usually use this class in your application code. Normally the order of
 *  evaluation of the SQL is implied by the parethesis in your Java code. i.e.
 *  <PRE>
 * DbExpr x = a.and((b.or(c)).and(d))
 * </PRE> This will automatically retain the Java evaluation order... <PRE>
 * A AND ( (B OR C) AND D)
 * </PRE>
 *
 *@author     pcguest
 *@created    October 18, 2001
 */

public class DbParenthesis extends DbExpr {
  DbExpr expr;


  /**
   *  Constructor for the DbParenthesis object
   *
   *@param  db    Description of Parameter
   *@param  expr  Description of Parameter
   */
  public DbParenthesis(DbExpr expr) {
    super();
    this.expr = expr;
  }


  /**
   *  Sets the sqlValues attribute of the DbParenthesis object
   *
   *@param  ps                                       The new sqlValues value
   *@param  i                                        The new sqlValues value
   *@return                                          Description of the
   *      Returned Value
   *@exception  java.sql.SQLException                Description of Exception
   *@exception  javatools.db.Exception  Description of Exception
   */
  public int setSqlValues(PreparedStatement ps, int i) throws SQLException {
    return expr.setSqlValues(ps, i);
  }


  /**
   *  Gets the queryString attribute of the DbParenthesis object
   *
   *@return                  The queryString value
   *@exception  Exception  Description of Exception
   */
  public String getQueryString() {
    return "( " + expr.getQueryString() + " )";
  }


  /**
   *  Description of the Method
   *
   *@param  c  Description of Parameter
   */
  public void usesTables(Set c) {
    expr.usesTables(c);
  }
}
