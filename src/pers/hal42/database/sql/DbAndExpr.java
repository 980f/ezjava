package pers.hal42.database.sql;
import java.sql.*;

/**
 * An expression of the form A AND B. The reason we have this class as well as
 * DbCriterion, is that this class will optimise away unnecessary segments.
 * i.e. A AND TRUE will be optimised to just A. The reason you may find a
 * TRUE expression in your code is the use of DbDatabase.trueExpr().
 * Of course we could just leave the dummy true expressions in the final SQL
 * and presumably the dbms can optimise it away fine, but it looks a bit ugly
 * and nasty to have these dummy expressions in the result.
 * @see DbTrueExpr
 */

public class DbAndExpr extends DbCriterion {

  public DbAndExpr(Object o1, Object o2) {
    super(o1, "AND", o2);
  }
  public String getQueryString() {
    // An optimisation hack
    if (c1 instanceof DbFalseExpr || c2 instanceof DbFalseExpr) {
      return "";
    } else if (c1 instanceof DbTrueExpr) {
      return getString(c2);
    } else if (c2 instanceof DbTrueExpr) {
      return getString(c1);
    } else {
      return super.getQueryString();
    }
  }
  public int setSqlValues(PreparedStatement ps, int i) throws java.sql.SQLException {
    if (c1 instanceof DbFalseExpr || c2 instanceof DbFalseExpr) {
      return i;
    } else if (c1 instanceof DbTrueExpr) {
      return setSqlValue(ps, i, c2, null);
    } else if (c2 instanceof DbTrueExpr) {
      return setSqlValue(ps, i, c1, null);
    } else {
      return super.setSqlValues( ps,  i);
    }
  }
}
