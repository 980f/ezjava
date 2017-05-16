package pers.hal42.database.sql;

/**
 * An expression that always evaluates to false. If not optimised away by
 * DbTrueExpr, it will probably result in "0 = 1" in the SQL.
 * <p>
 * The use of false expressions makes it very convenient for dynamically
 * generated queries...
 * <p>
 * <PRE>
 * DbExpr e = db.falseExpr();
 * if (name != null) {
 * e = e.or(table.getColumn("NAME")).equals(name));
 * }
 * if (age != null) {
 * e = e.or(table.getColumn("AGE")).equals(age));
 * }
 * </PRE>
 * This gets converted to...
 * <PRE>
 * (0 = 1) OR ( NAME = ? ) OR ( AGE = ? )
 * </PRE>
 * and then DbOrExpr optimises this to...
 * <PRE>
 * ( NAME = ? ) OR ( AGE = ? )
 * </PRE>
 *
 * @author Bitmec
 * @created 26 September 2001
 * @see DbOrExpr
 */

public class DbFalseExpr extends DbCriterion {
  public DbFalseExpr() {
    super("0", "=", "1");
  }
}
