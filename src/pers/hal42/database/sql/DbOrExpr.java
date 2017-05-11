package pers.hal42.database.sql;
import java.sql.*;

/**
 *  An expression of the form A OR B. The reason we have this class as well as
 *  DbCriterion, is that this class will optimise away unnecessary segments.
 *  i.e. A OR FALSE will be optimised to just A. The reason you may find a FALSE
 *  expression in your code is the use of DbDatabase.falseExpr(). This is a very
 *  convenient thing for dynamically generated queries.
 *  Of course we could just leave the dummy false expressions in the
 *  final SQL and presumably the dbms can optimise it away fine, but it looks a
 *  bit ugly and nasty to have these dummy expressions in the result.
 *  @see DbFalseExpr.
 * @author     Bitmec
 * @created    26 September 2001
 */
public class DbOrExpr extends DbCriterion {

  public DbOrExpr(Object o1, Object o2) {
    super(o1, "OR", o2);
  }

  public int setSqlValues(PreparedStatement ps, int i) throws SQLException {
    if (c1 instanceof DbTrueExpr || c2 instanceof DbTrueExpr) {
      return i;
    } else if (c1 instanceof DbFalseExpr) {
      return setSqlValue(ps, i, c2, null);
    } else if (c2 instanceof DbFalseExpr) {
      return setSqlValue(ps, i, c1, null);
    } else {
      return super.setSqlValues(ps, i);
    }
  }

  public String getQueryString() {
    // An optimisation hack
    if (c1 instanceof DbTrueExpr || c2 instanceof DbTrueExpr) {
      return "";
    } else if (c1 instanceof DbFalseExpr) {
      return getString(c2);
    } else if (c2 instanceof DbFalseExpr) {
      return getString(c1);
    } else {
      return super.getQueryString();
    }
  }
}
