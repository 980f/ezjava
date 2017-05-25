package pers.hal42.database.sql;

import org.jetbrains.annotations.NotNull;
import pers.hal42.database.ColumnProfile;
import pers.hal42.database.TableProfile;
import pers.hal42.logging.ErrorLogStream;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * A class used to select tabular data from an SQL database. The constructor is
 * not public. To obtain a DbSelector call DbDatabase.selector(); Example: To
 * select FRED's record from the people table... <PRE>
 * DbDatabase db = ...;
 * DbTable people = db.getTable("PEOPLE");
 * DbSelector selector = db.selector();
 * selector.addColumn(people.getColumn("NAME"));
 * selector.addColumn(people.getColumn("AGE"));
 * selector.setWhere(people.getColumn("NAME").equal("FRED"));
 * DbTable result = selector.execute();
 * DbIterator it = result.iterator();
 * while (it.hasNextRow()) {
 * DbRow row = it.nextRow();
 * System.out.println(row.getValue("NAME") + " " + row.getValue("AGE"));
 * }
 * </PRE> This is equivilent to... <PRE>
 * SELECT NAME, AGE FROM PEOPLE WHERE PEOPLE.NAME='FRED';
 * </PRE> To get more fancy we can join the people table with the team table to
 * find the captain of the person's favourite team. Then we can also order by
 * the person's name, while igoring upper/lower case distinctions... <PRE>
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
 * DbRow row = it.nextRow();
 * System.out.println(row.getValue("NAME") + " " + row.getValue("CAPTAIN"));
 * }
 * </PRE> This is equivilent to... <PRE>
 * SELECT PEOPLE.NAME, TEAM.CAPTAIN FROM PEOPLE, TEAM WHERE TEAM.NAME = PEOPLE.FAVOURITE_TEAM
 * ORDER BY LOWER(PEOPLE.NAME)
 * </PRE> To get fancier still, we can make use of sub-selects. To find all the
 * people who happen to be captains of teams... <PRE>
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
 * DbRow row = it.nextRow();
 * System.out.println(row.toString());
 * }
 * </PRE> This is equivilent to... <PRE>
 * SELECT * from PEOPLE WHERE PEOPLE.NAME IN (SELECT CAPTAIN FROM TEAM);
 *
 * @author Chris Bitmead
 * @created 5 September 2001
 */
public class DbSelector extends DbExpr {
  ResultSet result;
  Map<DbExpr, String> asMap = new HashMap();
  List<DbExpr> columnList = new ArrayList<>();
  DbExpr where;
  List<DbExpr> orderBy = new LinkedList<>();
  ResultSet resultSet;
  //	PreparedStatement stmt;
  DbExpr limit;
  DbExpr offset;
  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(DbSelector.class);

  DbSelector() {
    super();
    result = null;
  }


  /**
   * Set the where condition for this query.
   *
   * @param where The new where value
   */
  public void setWhere(DbExpr where) {
    this.where = where;
  }

  /**
   * Set the entire orderby list in one go.
   *
   * @param l The new orderBy value
   */
  public void setOrderBy(List<DbExpr> l) {
    orderBy = l;
  }

  public int setSqlValues(PreparedStatement stmt, int i) throws SQLException {
    return setSqlValues(stmt, i, null);
  }

  /**
   * Don't get the whole result set, get only a limited number of rows. Should
   * be used in conjunction with ORDER BY in order to make the returned rows
   * deterministic.
   *
   * @param n The new limit value
   */
  public void setLimit(int n) {
    String sql = "limit";
    limit = new DbMiscExpr(sql, n);
  }

  /**
   * Don't get the first results, but skip n result rows. Should be used in
   * conjunction with ORDER BY in order to make the returned rows deterministic.
   *
   * @param n The new offset value
   */
  public void setOffset(int n) {
    String sql = "offset";
    offset = new DbMiscExpr(sql, n);
  }

  /**
   * Get the query string represented by this query.
   *
   * @return The queryString value
   */
  public StringBuilder getQueryString() {
    StringBuilder rtn = new StringBuilder(100);
    rtn.append("SELECT ");
    int i = 0;
    Iterator<DbExpr> fieldi = columnList.iterator();
    Set<TableProfile> tables = new HashSet<>();
    selectTables(tables);
    while (fieldi.hasNext()) {
      DbExpr col = fieldi.next();
      if (i != 0) {
        rtn.append(", ");
      }
      rtn.append(getString(col));
      String as = asMap.get(col);
      if (as != null) {
        rtn.append(" AS ").append(as);
      }
      i++;
    }
    i = 0;
    rtn.append(" FROM ");
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
      rtn.append("DUAL");//works for most db's
    } else {
      for (TableProfile tab : tables) {
        if (i++ != 0) {
          rtn.append(", ");
        }
        rtn.append(tab.fullname());
      }
    }
    if (where != null) {
      rtn.append(" WHERE ");
      rtn.append(where.getQueryString());
    }
    rtn.append(orderByClause(orderBy));
    if (limit != null) {
      rtn.append(" ").append(limit.getQueryString());
    }
    if (offset != null) {
      rtn.append(" ").append(offset.getQueryString());
    }
    dbg.VERBOSE(rtn.toString());
    return rtn;
  }

  /**
   * Add the given object to the select column list.
   *
   * @param col A DbColumn, DbExpr or literal value
   */
  public DbSelector addColumn(DbExpr col) {
    return addColumn(col, null);
  }

  /**
   * Add the given object to the select column list with an "AS" alias.
   *
   * @param col a DbColumn, DbExpr or literal value
   * @param as  a column alias
   * @return Description of the Returned Value
   */
  public DbSelector addColumn(DbExpr col, String as) {
    columnList.add(col);
    asMap.put(col, as);
    return this;
  }

  /**
   * Add all the columns from the given table to the select list. A bit like
   * SELECT * from table.
   *
   * @param table the table whose columns we wish to add
   * @throws Exception Description of Exception
   */
  public void addAll(TableProfile table) {
    for (ColumnProfile column : table.columns()) {
      addColumn(columnExpr(column));
    }
  }

  /**
   * Add all the columns from the given table to the select list. A bit like
   * SELECT * from table.
   *
   * @param table the table whose columns we wish to add
   * @param set   The feature to be added to the AllExcept attribute
   * @throws Exception Description of Exception
   */
  public void addAllExcept(TableProfile table, Set<ColumnProfile> set) {
    for (ColumnProfile col : table.columns()) {
      if (!set.contains(col)) {
        addColumn(columnExpr(col));  //todo: addmethod for making dpexpre for column
      }
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
   * Add an ORDER BY clause to this select. The column actually need not be a
   * plain column. It could be a column with a function applied. e.g.
   * addOrderBy(table.getColumn("NAME").upper, false);
   *
   * @param column the column to order by
   * @param desc   whether to sort in descending order
   */
  public void addOrderBy(DbExpr column, boolean desc) {
    orderBy.add(new DbOrderBy(column, desc));
  }

  /**
   * Execute and get a JDBC ResultSet.
   *
   * @throws Exception Description of Exception
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
   * Execute and return a DbTable.
   *
   * @return Description of the Returned Value
   * @throws Exception Description of Exception
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
      return String.valueOf(getQueryString());
    } catch (Exception e) {
      return e.toString();
    }
  }

  public void selectTables(Set c) {
    for (Object col : columnList) {
      usesTables(c, col);
    }
    if (where != null) {
      where.usesTables(c);
    }
  }

  /**
   * Substitute the literal values in the Prepared Statement.
   *
   * @param stmt     the PreparedStatement
   * @param i        the parameter number we are up to
   * @param intoList The new sqlValues value
   * @return Description of the Returned Value
   * @throws Exception    Description of Exception
   * @throws SQLException Description of Exception
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
   * Generate the order by clause.
   *
   * @param orderBy Description of Parameter
   * @return Description of the Returned Value
   * @throws Exception Description of Exception
   */
  StringBuilder orderByClause(List<DbExpr> orderBy) {
    StringBuilder rtn = new StringBuilder(100);
    if (orderBy != null) {
      int i = 0;
      for (DbExpr anOrderBy : orderBy) {
        DbOrderBy orderField = (DbOrderBy) anOrderBy;
        if (i++ == 0) {
          rtn.append(" ORDER BY ");
        } else {
          rtn.append(", ");
        }
        rtn.append(orderField.getQueryString());
      }
    }
    return rtn;
  }

  @NotNull
  private static DbLiteral columnExpr(ColumnProfile column) {
    return new DbLiteral(column.name());
  }
}
