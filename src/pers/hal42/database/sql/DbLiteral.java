package pers.hal42.database.sql;
import java.sql.*;
import java.util.*;

/**
 *  Inserts a piece of literal text within the SQL expression. This can be
 *  useful for non-portable hacks into the SQL code.
 *
 *@author     pcguest
 *@created    October 18, 2001
 */

public class DbLiteral extends DbExpr {
  String str;


  /**
   *  Constructor for the DbLiteral object
   *
   *@param  db  Description of Parameter
   *@param  s   Description of Parameter
   */
  public DbLiteral(String s) {
    super();
    str = s;
  }


  /**
   *  Sets the sqlValues attribute of the DbLiteral object
   *
   *@param  ps                                       The new sqlValues value
   *@param  i                                        The new sqlValues value
   *@return                                          Description of the
   *      Returned Value
   *@exception  java.sql.SQLException                Description of Exception
   *@exception  javatools.db.DbException  Description of Exception
   */
  public int setSqlValues(PreparedStatement ps, int i) throws SQLException {
    return i;
  }


  /**
   *  Gets the queryString attribute of the DbLiteral object
   *
   *@return    The queryString value
   */
  public String getQueryString() {
    return str;
  }
}
