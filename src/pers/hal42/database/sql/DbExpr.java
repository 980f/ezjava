package pers.hal42.database.sql;

import pers.hal42.database.ColumnProfile;
import pers.hal42.database.ColumnTypes;
import pers.hal42.logging.ErrorLogStream;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;

/**
 * An sql expression class. This is the abstract base class for any type of SQL  expression.
 *
 * @author Chris Bitmead
 * @created 29 August 2001
 */

public abstract class DbExpr {

  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(DbExpr.class);

  public DbExpr() {
  }


  public abstract int setSqlValues(PreparedStatement ps, int i) throws SQLException;

  /**
   * Any DbExpr needs to be able to convert into the SQL string equivilent.
   *
   * @return The queryString value
   * @throws Exception Description of Exception
   */
  public abstract StringBuilder getQueryString();

  /**
   * Gets the null attribute of the DbExpr object
   *
   * @return The null value
   */
  public DbCriterion isNull() {
    return new DbCriterion(this, "IS", new DbLiteral("NULL"));
  }

  /**
   * Gets the notNull attribute of the DbExpr object
   *
   * @return The notNull value
   */
  public DbCriterion isNotNull() {
    return new DbCriterion(this, "IS NOT", new DbLiteral("NULL"));
  }

  /**
   * Description of the Method
   *
   * @param c Description of Parameter
   */
  public void usesTables(Set c) {
  }

  /**
   * Description of the Method
   *
   * @param e Description of Parameter
   * @return Description of the Returned Value
   */
  public DbExpr and(DbExpr e) {
    return new DbAndExpr(this, e);
  }

  /**
   * Description of the Method
   *
   * @param e Description of Parameter
   * @return Description of the Returned Value
   */
  public DbExpr or(DbExpr e) {
    return new DbOrExpr(this, e);
  }

  /**
   * Description of the Method
   *
   * @return Description of the Returned Value
   */
  public DbExpr max() {
    return new DbExprFuncDef("MAX", this);
  }

  /**
   * Description of the Method
   *
   * @return Description of the Returned Value
   */
  public DbExpr min() {
    return new DbExprFuncDef("MIN", this);
  }

  /**
   * Description of the Method
   *
   * @return Description of the Returned Value
   */
  public DbExpr upper() {
    return new DbExprFuncDef("UPPER", this);
  }

  /**
   * Description of the Method
   *
   * @return Description of the Returned Value
   */
  public DbExpr lower() {
    return new DbExprFuncDef("LOWER", this);
  }

  /**
   * Description of the Method
   *
   * @return Description of the Returned Value
   * @throws Exception Description of Exception
   */
  public DbExpr dateTrunc() {
    String sql = "";//props.getProperty(db.getProperty("vendor") + ".dateTrunc");
    return new DbMiscExpr(sql, this);
  }

  /**
   * Return an expression representing this column being equal to another value.
   *
   * @param o Description of Parameter
   * @return Description of the Returned Value
   */
  public DbCriterion equal(Object o) {
    return new DbCriterion(this, "=", o);
  }

  /**
   * Description of the Method
   *
   * @param o Description of Parameter
   * @return Description of the Returned Value
   */
  public DbCriterion notEqual(Object o) {
    return new DbCriterion(this, "<>", o);
  }

  /**
   * Return an expression representing this column being LIKE another value.
   *
   * @param o Description of Parameter
   * @return Description of the Returned Value
   */
  public DbCriterion like(Object o) {
    return new DbCriterion(this, "LIKE", o);
  }

  /**
   * Return an expression representing this column being greater than another
   * value.
   *
   * @param o Description of Parameter
   * @return Description of the Returned Value
   */
  public DbCriterion greaterThan(Object o) {
    return new DbCriterion(this, ">", o);
  }

  /**
   * Return an expression representing this column being greater than or equal
   * to another value.
   *
   * @param o Description of Parameter
   * @return Description of the Returned Value
   */
  public DbCriterion greaterThanOrEqual(Object o) {
    return new DbCriterion(this, ">=", o);
  }

  /**
   * Return an expression representing this column being less than another
   * value.
   *
   * @param o Description of Parameter
   * @return Description of the Returned Value
   */
  public DbCriterion lessThan(Object o) {
    return new DbCriterion(this, "<", o);
  }

  /**
   * Return an expression representing this column being less than or equal to
   * another value.
   *
   * @param o Description of Parameter
   * @return Description of the Returned Value
   */
  public DbCriterion lessThanOrEqual(Object o) {
    return new DbCriterion(this, "<=", o);
  }

  /**
   * Description of the Method
   *
   * @param o Description of Parameter
   * @return Description of the Returned Value
   */
  public DbCriterion in(Object o) {
    return new DbCriterion(this, "IN", new DbParenthesis((DbExpr) o));
  }

  /**
   * Description of the Method
   *
   * @param o Description of Parameter
   * @return Description of the Returned Value
   */
  public DbCriterion notIn(Object o) {
    return new DbCriterion(this, "NOT IN", new DbParenthesis((DbExpr) o));
  }

  /**
   * Description of the Method
   *
   * @param c Description of Parameter
   * @param o Description of Parameter
   */
  public static void usesTables(Set c, Object o) {
    if (o != null && o instanceof DbExpr) {
      ((DbExpr) o).usesTables(c);
    }
  }

  /**
   * Substitute the literal value in the Prepared Statement. This is a utility
   * method used by several other classes.
   *
   * @param stmt    the PreparedStatement
   * @param i       the parameter number we are up to
   * @param intocol optional list of columns the result will be
   *                selected into
   * @param col     The new sqlValue value
   * @return Description of the Returned Value
   * @throws SQLException Description of Exception
   * @throws Exception    Description of Exception
   */
  static int setSqlValue(PreparedStatement stmt, int i, Object col, ColumnProfile intocol) throws SQLException {
    ColumnTypes type = ColumnTypes.VARCHAR;
    if (intocol != null) {
      type = intocol.numericType();
    }
    if (col instanceof DbExpr) {
      i = ((DbExpr) col).setSqlValues(stmt, i);
    } else {
      dbg.VERBOSE("setSqlValue: val = '" + col + "'");
      if (col == null) {
        stmt.setNull(i++, type.ordinal()); //todo:0 suspicious,
      } else if ((col instanceof String) && (type == ColumnTypes.BLOB || type == ColumnTypes.BINARY || type == ColumnTypes.VARBINARY || type == ColumnTypes.LONGVARBINARY)) {
        stmt.setBytes(i++, ((String) col).getBytes());
      } else {
        stmt.setObject(i++, col);
      }
    }
    return i;
  }

  /**
   * Utility function to turn o into an SQL expression.
   *
   * @param o Description of Parameter
   * @return The string value
   * @throws Exception Description of Exception
   */
  static String getString(Object o) {
    if (o instanceof DbExpr) {
      return ((DbExpr) o).getQueryString();
    } else {
      return " ? ";
    }
  }
}
