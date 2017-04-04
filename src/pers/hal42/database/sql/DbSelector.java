package pers.hal42.database.sql;
import pers.hal42.database.ColumnProfile;
import pers.hal42.database.TableProfile;
import pers.hal42.logging.ErrorLogStream;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *  A class used to select tabular data from an SQL database. The constructor is
 *  not public. To obtain a DbSelector call DbDatabase.selector(); Example: To
 *  select FRED's record from the people table... <PRE>
 * DbDatabase db = ...;
 * DbTable people = db.getTable("PEOPLE");
 * DbSelector selector = db.selector();
 * selector.addColumn(people.getColumn("NAME"));
 * selector.addColumn(people.getColumn("AGE"));
 * selector.setWhere(people.getColumn("NAME").equal("FRED"));
 * DbTable result = selector.execute();
 * DbIterator it = result.iterator();
 * while (it.hasNextRow()) {
 *     DbRow row = it.nextRow();
 *     System.out.println(row.getValue("NAME") + " " + row.getValue("AGE"));
 * }
 * </PRE> This is equivilent to... <PRE>
 * SELECT NAME, AGE FROM PEOPLE WHERE PEOPLE.NAME='FRED';
 * </PRE> To get more fancy we can join the people table with the team table to
 *  find the captain of the person's favourite team. Then we can also order by
 *  the person's name, while igoring upper/lower case distinctions... <PRE>
 * DbDatabase db = ...;
 * DbSelector selector = db.selector();
 * DbTable people = db.getTable("PEOPLE");
 * DbTable team = db.getTable("TEAM");
 * DbSelector selector = db.selector();
 * selector.addColumn(people.getColumn("NAME"));
 * selector.addColumn(team.getColumn("CAPTAIN"));
 * selector.setWhere(team.getColumn("NAME").equal(people.getColumn("FAVOURITE_TEAM"));
 * selector.addOrderBy(people.getColumn("NAME").lower(), false) // Order by NAME ignoring case.
 * DbTable result = selector.execute();
 * DbIterator it = result.iterator();
 * while (it.hasNextRow()) {
 *     DbRow row = it.nextRow();
 *     System.out.println(row.getValue("NAME") + " " + row.getValue("CAPTAIN"));
 * }
 * </PRE> This is equivilent to... <PRE>
 * SELECT PEOPLE.NAME, TEAM.CAPTAIN FROM PEOPLE, TEAM WHERE TEAM.NAME = PEOPLE.FAVOURITE_TEAM
 *   ORDER BY LOWER(PEOPLE.NAME)
 * </PRE> To get fancier still, we can make use of sub-selects. To find all the
 *  people who happen to be captains of teams... <PRE>
 * DbDatabase db = ...;
 * DbTable people = db.getTable("PEOPLE");
 * DbTable team = db.getTable("TEAM");
 * DbSelector subselector = db.selector();
 * subselector.addColumn(team.getColumn("CAPTAIN"));
 * DbSelector selector = db.selector();
 * selector.addAll(people);
 * selector.setWhere(people.getColumn("NAME").in(subselector));
 * DbTable result = selector.execute();
 * DbIterator it = result.iterator();
 * while (it.hasNextRow()) {
 *     DbRow row = it.nextRow();
 *     System.out.println(row.toString());
 * }
 * </PRE> This is equivilent to... <PRE>
 * SELECT * from PEOPLE WHERE PEOPLE.NAME IN (SELECT CAPTAIN FROM TEAM);
 *
 *
 *
 *
 *
 *
 *
 * @author     Chris Bitmead
 * @created    5 September 2001
 */
public class DbSelector extends DbExpr {
  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(DbSelector.class);

  ResultSet result;
  Map asMap = new HashMap();
  List columnList = new ArrayList();
  DbExpr where;
  List orderBy = new LinkedList();
  ResultSet resultSet;
//	PreparedStatement stmt;
  DbExpr limit;
  DbExpr offset;

  DbSelector() {
    super();
    result = null;
  }


  /**
   *  Set the where condition for this query.
   *
   * @param  where  The new where value
   */
  public void setWhere(DbExpr where) {
    this.where = where;
  }

  /**
   *  Set the entire orderby list in one go.
   *
   * @param  l  The new orderBy value
   */
  public void setOrderBy(List l) {
    orderBy = l;
  }

  public int setSqlValues(PreparedStatement stmt, int i) throws SQLException {
    return setSqlValues(stmt, i, null);
  }

  /**
   *  Don't get the whole result set, get only a limited number of rows. Should
   *  be used in conjunction with ORDER BY in order to make the returned rows
   *  deterministic.
   *
   * @param  n                The new limit value
   */
  public void setLimit(int n) {
    String sql = "limit";
    limit = new DbMiscExpr(sql, new Integer(n));
  }

  /**
   *  Don't get the first results, but skip n result rows. Should be used in
   *  conjunction with ORDER BY in order to make the returned rows deterministic.
   *
   * @param  n                The new offset value
   */
  public void setOffset(int n) {
    String sql = "offset";
    offset = new DbMiscExpr(sql, new Integer(n));
  }

  /**
   *  Get the query string represented by this query.
   *
   * @return                  The queryString value
   * @exception  Exception  Description of Exception
   */
  public String getQueryString(){
    String rtn = "SELECT ";
    int i = 0;
    Iterator fieldi = columnList.iterator();
    Set tables = new HashSet();
    selectTables(tables);
    while (fieldi.hasNext()) {
      Object col = fieldi.next();
      if (i != 0) {
        rtn += ", ";
      }
      rtn += getString(col);
      String as = (String) asMap.get(col);
      if (as != null) {
        rtn += " AS " + as;
      }
      i++;
    }
    i = 0;
    rtn += " FROM ";
    if (tables.size() == 0) {
//      try {
//        Props props = Props.singleton("dbvendor");
//        String dummyTable = props.getProperty(db.getProperty("vendor") + ".dummyTable");
//        if (dummyTable != null) {
//          // for Oracle
//          rtn += dummyTable;
//        }
//      } catch (IOException e) {
//        throw new Exception(e);
//      }
    } else {
      Iterator tablei = tables.iterator();
      while (tablei.hasNext()) {
        TableProfile tab = (TableProfile) tablei.next();
        if (i++ != 0) {
          rtn += ", ";
        }
        rtn += tab.fullname();
      }
    }
    if (where != null) {
      rtn += " WHERE ";
      rtn += where.getQueryString();
    }
    rtn += orderByClause(orderBy);
    if (limit != null) {
      rtn += " " + limit.getQueryString();
    }
    if (offset != null) {
      rtn += " " + offset.getQueryString();
    }
    dbg.VERBOSE(rtn);
    return rtn;
  }

  /**
   *  Add the given object to the select column list.
   *
   * @param  col              A DbColumn, DbExpr or literal value
   * @return                  Description of the Returned Value
   * @exception  Exception  Description of Exception
   */
  public DbSelector addColumn(Object col) {
    return addColumn(col, null);
  }

  /**
   *  Add the given object to the select column list with an "AS" alias.
   *
   * @param  col              a DbColumn, DbExpr or literal value
   * @param  as               a column alias
   * @return                  Description of the Returned Value
   */
  public DbSelector addColumn(Object col, String as) {
    columnList.add(col);
    asMap.put(col, as);
    return this;
  }

  /**
   *  Add all the columns from the given table to the select list. A bit like
   *  SELECT * from table.
   *
   * @param  table            the table whose columns we wish to add
   * @exception  Exception  Description of Exception
   */
  public void addAll(TableProfile table) {
    for (int i = 0; i < table.columns().length; i++) {
      addColumn(table.column(i));
    }
  }

//  /**
//   *  Add all the columns from the given table to the select list. A bit like
//   *  SELECT * from table.
//   *
//   * @param  table            the table whose columns we wish to add
//   * @param  o                The feature to be added to the AllExcept attribute
//   * @exception  Exception  Description of Exception
//   */
//  public void addAllExcept(DbTable table, ColumnProfile o) {
//    for (int i = 0; i < table.names.length; i++) {
//      ColumnProfile col = table.getColumn(i);
//      if (!col.equals(o)) {
//        addColumn(col);
//      }
//    }
//  }

  /**
   *  Add all the columns from the given table to the select list. A bit like
   *  SELECT * from table.
   *
   * @param  table            the table whose columns we wish to add
   * @param  set              The feature to be added to the AllExcept attribute
   * @exception  Exception  Description of Exception
   */
  public void addAllExcept(TableProfile table, Set set) {
    for (int i = 0; i < table.columns().length; i++) {
      ColumnProfile col = table.column(i);
      if (!set.contains(col)) {
        addColumn(col);
      }
    }
  }

  /**
   *  Add an ORDER BY clause to this select. The column actually need not be a
   *  plain column. It could be a column with a function applied. e.g.
   *  addOrderBy(table.getColumn("NAME").upper, false);
   *
   * @param  column  the column to order by
   * @param  desc    whether to sort in descending order
   */
  public void addOrderBy(DbExpr column, boolean desc) {
    orderBy.add(new DbOrderBy(column, desc));
  }

  /**
   *  Execute and get a JDBC ResultSet.
   *
   * @exception  Exception  Description of Exception
   * @deprecated no reason given
   */
  public PreparedStatement executeToResultSet() {
//      PreparedStatement stmt = dbcon.con.prepareStatement(getQueryString());
//      setSqlValues(stmt, 1, null);
 //			resultSet = stmt.executeQuery();
//      return stmt;
 return null;//
  }

  /**
   *  Execute and return a DbTable.
   *
   * @return                  Description of the Returned Value
   * @exception  Exception  Description of Exception
   * @deprecated gutted
   */
  public ResultSet execute() {
//    PreparedStatement stmt = executeToResultSet(dbcon);
//    result.setStatement(stmt);
//    return result;
    return null;
  }

  public String toString() {
    try {
      return getQueryString();
    } catch (Exception e) {
      return e.toString();
    }
  }

  public void selectTables(Set c) {
    Iterator fieldi = columnList.iterator();
    while (fieldi.hasNext()) {
      Object col = fieldi.next();
      usesTables(c, col);
    }
    if (where != null) {
      where.usesTables(c);
    }
  }

  /**
   *  Substitute the literal values in the Prepared Statement.
   *
   * @param  stmt              the PreparedStatement
   * @param  i                 the parameter number we are up to
   * @param  intoList          The new sqlValues value
   * @return                   Description of the Returned Value
   * @exception  Exception   Description of Exception
   * @exception  SQLException  Description of Exception
   */
  int setSqlValues(PreparedStatement stmt, int i, List intoList) throws SQLException {
    Iterator columni = columnList.iterator();
    Iterator intoi = null;
    if (intoList != null) {
      intoi = intoList.iterator();
    }
    while (columni.hasNext()) {
      Object col = columni.next();
      ColumnProfile intocol = null;
      if (intoi != null) {
        intocol = (ColumnProfile) intoi.next();
      }
      i = setSqlValue(stmt, i, col, intocol);
    }
    if (where != null) {
      i = where.setSqlValues(stmt, i);
    }
    return i;
  }

  /**
   *  Generate the order by clause.
   *
   * @param  orderBy          Description of Parameter
   * @return                  Description of the Returned Value
   * @exception  Exception  Description of Exception
   */
  String orderByClause(List orderBy) {
    String rtn = "";
    if (orderBy != null) {
      int i = 0;
      Iterator keys = orderBy.iterator();
      while (keys.hasNext()) {
        DbOrderBy orderField = (DbOrderBy) keys.next();
        if (i++ == 0) {
          rtn += " ORDER BY ";
        } else {
          rtn += ", ";
        }
        rtn += orderField.getQueryString();
      }
    }
    return rtn;
  }
}
