package pers.hal42.database.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;

/**
 * An SQL expression of the form FUNCNAME(parameter....).
 *
 * @author Chris Bitmead
 * @created October 18, 2001
 */
public class DbExprFuncDef extends DbExpr {
  String func;
  Object args[];


  /**
   * @param func function name
   */
  public DbExprFuncDef(String func) {
    super();
    this.func = func;
  }

  public DbExprFuncDef(String func, Object arg1) {
    super();
    this.func = func;
    args = new Object[1];
    args[0] = arg1;
  }

  public DbExprFuncDef(String func, Object arg1, Object arg2) {
    super();
    this.func = func;
    args = new Object[2];
    args[0] = arg1;
    args[1] = arg2;
  }


  /**
   * Sets the sqlValues attribute of the DbExprFuncDef object
   *
   * @param ps a prepared statement
   * @param i  The new sqlValues value
   * @return index of next (potential) substitution parameter
   * @throws SQLException Description of Exception
   */
  public int setSqlValues(PreparedStatement ps, int i) throws SQLException {
    for (Object arg : args) {
      i = setSqlValue(ps, i, arg, null);
    }
    return i;
  }


  /**
   * Gets the queryString attribute of the DbExprFuncDef object
   *
   * @return The queryString value
   */
  public StringBuilder getQueryString() {
    StringBuilder rtn = new StringBuilder(100);
    rtn.append(func).append("(");
    for (int c = 0; c < args.length; c++) {
      if (c != 0) {
        rtn.append(", ");
      }
      rtn.append(getString(args[c]));
    }
    rtn.append(")");
    return String.valueOf(rtn);
  }


  /**
   * Description of the Method
   *
   * @param coll Description of Parameter
   */
  public void usesTables(Set coll) {
    for (Object arg : args) {
      usesTables(coll, arg);
    }
  }
}
