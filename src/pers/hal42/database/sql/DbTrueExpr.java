package pers.hal42.database.sql;
import java.sql.*;

/**
 *  An expression that always evaluates to true. If not optimised away but
 *  DbAndExpr, this will probably result in "0 = 0" in the SQL. <P>
 *
 *  The use of true expressions makes it very convenient for dynamically
 *  generated queries...
 * <PRE>
 * DbExpr e = db.trueExpr();
 * if (name != null) {
 *   e = e.and(table.getColumn("NAME")).equals(name));
 *  }
 *  if (age != null) {
 *    e = e.and(table.getColumn("AGE")).equals(age));
 *  }
 *  </PRE>
 *  This gets converted to...
 *  <PRE>
 *  (0 = 0) AND ( NAME = ? ) AND ( AGE = ? )
 *  </PRE>
 *  and then DbAndExpr optimises this to...
 *  <PRE>
 *  ( NAME = ? ) AND ( AGE = ? )
 *  </PRE>
 *
 * @author     Bitmec
 * @created    26 September 2001
 * @see        DbAndExpr
 */
public class DbTrueExpr extends DbCriterion {
  public DbTrueExpr() {
    super("0", "=", "0");
  }
}
