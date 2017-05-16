package pers.hal42.database.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * An SQL expression of the form FUNCNAME(parameter....).
 *
 * @author Chris Bitmead
 */
public class DbMiscExpr extends DbExpr {
  String func;
  Object args[];

  public DbMiscExpr(String func) {
    super();
    this.func = func;
  }

  public DbMiscExpr(String func, Object arg1) {
    super();
    this.func = func;
    args = new Object[1];
    args[0] = arg1;
  }

  public DbMiscExpr(String func, Object arg1, Object arg2) {
    super();
    this.func = func;
    args = new Object[2];
    args[0] = arg1;
    args[1] = arg2;
  }

  public StringBuilder getQueryString() {
    StringTokenizer st = new StringTokenizer(func, "?");
    StringBuilder rtn = new StringBuilder();
    int c = 0;
    while (st.hasMoreTokens()) {
      rtn.append(st.nextToken());
      if (c < args.length) {
        rtn.append(getString(args[c++]));
      }
    }
    return rtn.toString();
  }

  public int setSqlValues(PreparedStatement ps, int i) throws SQLException {
    for (Object arg : args) {
      i = setSqlValue(ps, i, arg, null);
    }
    return i;
  }

  public void usesTables(Set coll) {
    for (Object arg : args) {
      usesTables(coll, arg);
    }
  }
}
