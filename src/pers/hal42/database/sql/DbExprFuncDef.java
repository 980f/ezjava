package pers.hal42.database.sql;
import java.sql.*;
import java.util.*;

/**
 *  An SQL expression of the form FUNCNAME(parameter....).
 *
 *@author     Chris Bitmead
 *@created    October 18, 2001
 */
public class DbExprFuncDef extends DbExpr {
  String func;
  Object args[];


  /**
   *  Constructor for the DbExprFuncDef object
   *
   *@param  db    Description of Parameter
   *@param  func  Description of Parameter
   */
  public DbExprFuncDef(String func) {
    super();
    this.func = func;
  }


  /**
   *  Constructor for the DbExprFuncDef object
   *
   *@param  db    Description of Parameter
   *@param  func  Description of Parameter
   *@param  arg1  Description of Parameter
   */
  public DbExprFuncDef(String func, Object arg1) {
    super();
    this.func = func;
    args = new Object[1];
    args[0] = arg1;
  }


  /**
   *  Constructor for the DbExprFuncDef object
   *
   *@param  db    Description of Parameter
   *@param  func  Description of Parameter
   *@param  arg1  Description of Parameter
   *@param  arg2  Description of Parameter
   */
  public DbExprFuncDef(String func, Object arg1, Object arg2) {
    super();
    this.func = func;
    args = new Object[2];
    args[0] = arg1;
    args[1] = arg2;
  }


  /**
   *  Sets the sqlValues attribute of the DbExprFuncDef object
   *
   *@param  ps                The new sqlValues value
   *@param  i                 The new sqlValues value
   *@return                   Description of the Returned Value
   *@exception  Exception   Description of Exception
   *@exception  SQLException  Description of Exception
   */
  public int setSqlValues(PreparedStatement ps, int i) throws SQLException {
    for (int c = 0; c < args.length; c++) {
      i = setSqlValue(ps, i, args[c], null);
    }
    return i;
  }


  /**
   *  Gets the queryString attribute of the DbExprFuncDef object
   *
   *@return                  The queryString value
   *@exception  Exception  Description of Exception
   */
  public String getQueryString() {
    String rtn = func + "(";
    for (int c = 0; c < args.length; c++) {
      if (c != 0) {
        rtn += ", ";
      }
      rtn += getString(args[c]);
    }
    rtn += ")";
    return rtn;
  }


  /**
   *  Description of the Method
   *
   *@param  coll  Description of Parameter
   */
  public void usesTables(Set coll) {
    for (int c = 0; c < args.length; c++) {
      usesTables(coll, args[c]);
    }
  }
}
