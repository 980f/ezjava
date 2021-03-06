package pers.hal42.database;

import pers.hal42.lang.*;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.logging.Tracer;
import pers.hal42.math.Accumulator;
import pers.hal42.text.Fstring;
import pers.hal42.text.TextList;
import pers.hal42.thread.Counter;
import pers.hal42.timer.StopWatch;
import pers.hal42.transport.EasyProperties;
import pers.hal42.util.PrintFork;

import java.sql.*;
import java.text.MessageFormat;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Function;

import static pers.hal42.database.DBMacros.Op.*;
import static pers.hal42.lang.Index.BadIndex;

/*
 * functions for performing database queries and updates
 * usage:  class MyDatabase extends DBMacros implements Database, ConfigurationManager, HardwareCfgMgr , BusinessCfgMgr
 * */

enum DBFunctionType {
  UPDATE, QUERY, NEXT;

  public static final String[] myTexts = {"Update: ", "Query:  ", "Next(): ",};

  public int numValues() {
    return 3;
  }

  protected final String[] getMyText() {
    return myTexts;
  }
}

public class DBMacros extends GenericDB {

  private static final TableInfo InfoTable = new SimpleTableInfo("information_schema", "tables");

  public enum Op {
    FAILED, ALREADY, DONE;

    int asInt() {
      return ordinal() - 1;//a legacy value
    }

    public boolean Ok() {
      return this != FAILED;
    }
  }

  /**
   * create a schema. @returns whether the operation was exception-free.
   * if @param picky then insist on this being a fresh creation, else ignore whether it already exists.
   */
  public boolean createSchema(String schemaName, boolean picky) {
    QueryString query = QueryString.Clause("CREATE SCHEMA");
    if (!picky) {
      query.cat("IF NOT EXISTS");
    }
    query.cat(schemaName);
//todo:    query.cat("CHARACTER SET = utf8");//maydo: param
//todo:    query.cat("COLLATE = utf8");//maydo: param
    return this.doSimpleAction(query.toString(), null);
  }

  /**
   * @returns last automatically generated value, such as for an auto primary key
   */
  public int getLastAuto() {
    //mysql version:
    return getIntFromQuery(QueryString.Select().cat("LAST_INSERT_ID()"));
  }

  /**
   * this executes profoundly poorly on mysql! It takes *minutes*
   */
  public TableInfoList getTableList(TableInfo tableInfo) {
    // get the list of all tables in the new database
    TableInfoList tables = new TableInfoList();
    ResultSet rs = getTables(tableInfo);
    if (rs != null) {
      while (next(rs)) {
        String name = getTableInfo(rs, "TABLE_NAME");
        String catalog = getTableInfo(rs, "TABLE_CAT");
        String schema = getTableInfo(rs, "TABLE_SCHEM");
        String type = getTableInfo(rs, "TABLE_TYPE");
        String remarks = getTableInfo(rs, "REMARKS");
        dbg.VERBOSE("Found table: CAT=" + catalog + ", SCHEM=" + schema + ", NAME=" + name + ", TYPE=" + type + ", REMARKS=" + remarks);
        if ((name != null) && (name.indexOf("SYS") != 0)) { // +++ We MUST make a rule to NOT start our table names with SYS!
          tables.add(new SimpleTableInfo(catalog, schema, name, type, remarks));
        }
      }
      closeStmt(getStatement(rs));
    }
    return tables.sortByName();
  }

  class NamedStatement {
    public Statement stmt;
    public String name;

    public NamedStatement(Statement stmt, String name) {
      this.stmt = stmt;
      this.name = Thread.currentThread().getName() + ";" + name;
    }
  }

  Counter queryCounter = new Counter(); // used to number queries for loggin purposes
  public static DBMacrosService service = null; // set elsewhere, once
  public static Accumulator queryStats = new Accumulator();
  public static Accumulator updateStats = new Accumulator();
  public static Accumulator nextStats = new Accumulator();
  private static Monitor timerStrMon = new Monitor("DBMacros.timerStr");
  //  public String getCatStringsFromRS(ResultSet rs, String div) {
//    String result = "";
//    String [] results = getStringsFromRS(rs);
//    if(results != null) {
//      for(int i = 0; i < results.length; i++) {
//        String adder = results[i].trim();
//        if(i != 0) {
//          result += div;
//        }
//        result += adder;
//      }
//    }
//    return result.trim();
//  }
//  public String getCatStringsFromQuery(QueryString queryStr, String div) {
//    String result = "";
//    String [] results = getStringsFromQuery(queryStr);
//    if(results != null) {
//      for(int i = 0; i < results.length; i++) {
//        String adder = results[i].trim();
//        if(i != 0) {
//          result += div;
//        }
//        result += adder;
//      }
//    }
//    return result.trim();
//  }
  private static PrintFork pf = null;
  public static final String ALLTABLES = null;
  public static final ErrorLogStream dbg = ErrorLogStream.getForClass(DBMacros.class);
  protected static final Tracer dbv = new Tracer(DBMacros.class, "Validator");
  protected static final int ONLYCOLUMN = 1;//index of column if there is only one

  private static final Fstring timerStr = new Fstring(7, ' ');
  private static final int NOT_A_COLUMN = BadIndex;

  public DBMacros(MysqlConnectionInfo connInfo, String threadname) {
    super(connInfo, threadname);
  }

  public final EasyProperties colsToProperties(QueryString qs, ColumnProfile ignoreColumn) {
    EasyProperties ezc = new EasyProperties();
    try {
      Statement stmt = query(qs);
      if (stmt == null) {
        dbg.ERROR("Can't convert cols to properties if stmt is null!");
      } else {
        ResultSet rs = getResultSet(stmt);
        ezc = colsToProperties(rs, ignoreColumn);
      }
    } catch (Exception ex) {
      dbg.Caught(ex);
    }
    return ezc;
  }

  public TextList getTextListColumnFromRS(ResultSet rs) {
    return getTextListColumnFromRS(rs, ONLYCOLUMN);
  }

  public TextList getTextListColumnFromRS(ResultSet rs, int col) {
    TextList tl = new TextList(50, 50);
    if (col < 1) {
      col = 1;
    }
    while (next(rs)) {
      String tmp = getStringFromRS(col, rs);
//      dbg.WARNING("adding "+tmp);
      tl.add(tmp);
    }
//    dbg.WARNING("returning: " + tl);
    return tl;
  }

  public final EasyProperties rowsToProperties(QueryString qs, String nameColName, String valueColName) {
    EasyProperties ezc = new EasyProperties();
    try {
      Statement stmt = query(qs);
      if (stmt == null) {
        dbg.ERROR("Can't convert rows to properties if stmt is null!");
      } else {
        ResultSet rs = getResultSet(stmt);
        ezc = rowsToProperties(rs, nameColName, valueColName);
      }
    } catch (Exception ex) {
      dbg.Caught(ex);
    }
    return ezc;
  }

  public final EasyProperties rowsToProperties(ResultSet rs, String nameColName, String valueColName) {
    EasyProperties ezc = new EasyProperties();
    try {
      if (rs != null) {
        while (next(rs)) {
          String name = getStringFromRS(nameColName, rs);
          String value = getStringFromRS(valueColName, rs);
          ezc.setString(name, value);
        }
      } else {
        dbg.ERROR("Can't convert rows to properties if resultset is null!");
      }
    } catch (Exception ex) {
      dbg.Caught(ex);
    }
    return ezc;
  }

  private String[] getStringsFromRS(ResultSet rs) {
    TextList strings = new TextList();
    int fieldCount = 0;
    ResultSetMetaData rsmd = getRSMD(rs);
    try {
      dbg.VERBOSE("Calling ResultSetMetaData.getColumnCount() ...");
      fieldCount = rsmd.getColumnCount();
    } catch (Exception t) {
      dbg.Caught(t); // +++ more details?
    } finally {
      dbg.VERBOSE("Done calling ResultSetMetaData.getColumnCount().");
    }
    if (next(rs)) {
      for (int i = 0; i++ < fieldCount; ) {//[1..fieldcount]
        String str = null;
        try {
          str = getStringFromRS(i, rs);
        } catch (Exception ex) {
          dbg.Caught(ex);
        }
        strings.add(str);
      }
    } else {
      dbg.WARNING("No records in ResultSet!");
    }
    return strings.toStringArray();
  }

  private String[] getStringsFromQuery(QueryString queryStr) {
    Statement stmt = query(queryStr);
    String[] str1 = new String[0];
    if (stmt != null) {
      ResultSet rs = getResultSet(stmt);
      if (rs != null) {
        str1 = getStringsFromRS(rs);
      } else {
        dbg.ERROR("getStringsFromQuery()->query() call did not succeed (null resultSet)");
      }
      closeStmt(stmt);//+_+ add try/catch
    } else {
      dbg.ERROR("getStringsFromQuery()->query() call did not succeed (null statement)");
    }
    return str1;
  }

  public String getStringFromQuery(QueryString queryStr) {
    return getStringFromQuery(queryStr, 0);
  }

  /**
   * @returns blob content as string via getBytes, on any fault returns @param def
   */
  public static String blobString(Blob blob, String def) {
    try {
      return new String(blob.getBytes(1, (int) blob.length()));
    } catch (Exception any) {
      return def;
    }
  }

  /**
   * @returns blob content as string via getBytes, on any fault returns null
   */
  public static String blobString(Blob blob) {
    return blobString(blob, null);
  }

  public String getStringFromQuery(QueryString queryStr, ColumnProfile field) {
    return getStringFromQuery(queryStr, field.name());
  }

  public String getStringFromQuery(QueryString queryStr, String field) {
    Statement stmt = query(queryStr);
    String str1 = "";
    if (stmt != null) {
      ResultSet rs = getResultSet(stmt);
      if (rs != null) {
        str1 = getStringFromRS(field, rs);
      } else {
        dbg.ERROR("getStringFromQuery()->query() call did not succeed (null resultSet)");
      }
      closeStmt(stmt);
    } else {
      dbg.ERROR("getStringFromQuery()->query() call did not succeed (null statement)");
    }
    return str1;
  }

  /**
   * @returns the first field of the first recordType of @param query, null if anything goes wrong.
   */
  public String getStringFromQuery(String query) {
    try (Statement stmt = makeStatement()) {
      if (stmt.execute(query)) {
        ResultSet rs = getResultSet(stmt);
        if (rs != null && rs.next()) {
          return rs.getString(1);
        } else {
          return null;
        }
      } else {
        return null;
      }
    } catch (SQLException e) {
      dbg.Caught(e);
      return null;
    }
  }

  public String getStringFromQuery(QueryString queryStr, int field) {
    String result = null;
    String[] results = getStringsFromQuery(queryStr);
    if ((results != null) && (field < results.length)) {
      result = results[field].trim();
    }
    return result;
  }

  /**
   * @returns a new TextList built from the 1-based @param field of the @param query. On SQL exception returns null;
   */
  public TextList getTextListFromQuery(String qs, int field) {
    try (Finally pop = dbg.Push("getTextListFromQuery"); Statement stmt = makeStatement()) {
      ResultSet rs = stmt.executeQuery(qs);
      TextList tl = new TextList(50, 50);
      while (next(rs)) {
        tl.add(getStringFromRS(field, rs));
      }
      return tl;
    } catch (SQLException e) {
      dbg.Caught(e);
      return null;
    }
  }

  public TextList getTextListColumnFromQuery(QueryString qs, ColumnProfile cp) {
    Statement stmt = query(qs);
    ResultSet rs = null;
    if (stmt != null) {
      try {
        rs = getResultSet(stmt);
        if (rs != null) {
          return getTextListColumnFromRS(rs, cp);
        } else {
          dbg.WARNING("getTextListColumnFromQuery() result set is null!");
        }
      } catch (Exception ex) {
        dbg.Caught(ex);
      } finally {
        closeRS(rs);
        closeStmt(stmt);
      }
    }
    return null;
  }

  //  protected int [ ] getIntArrayColumnFromQuery(QueryString qs) {
//    return getIntArrayColumnFromQuery(qs, ONLYCOLUMN);
//  }
//
//  protected int [ ] getIntArrayColumnFromQuery(QueryString qs, int column) {
//    Statement stmt = query(qs);
//    if(stmt != null) {
//      try {
//        ResultSet rs = getResultSet(stmt);
//        return getIntArrayColumnFromRS(rs, column);
//      } catch (Exception ex) {
//        dbg.Caught(ex);
//      } finally {
//        closeStmt(stmt);
//      }
//    } else {
//      dbg.WARNING("getIntArrayColumnFromQuery() stmt=null!");
//    }
//    return null;
//  }
//
//  protected int [ ] getIntArrayColumnFromRS(ResultSet rs, ColumnProfile cp) {
//    int col = 1;
//    if((cp != null) && (rs != null)) {
//      try {
//        dbg.VERBOSE("Calling ResultSet.findColumn() ...");
//        col = rs.findColumn(cp.name());
//      } catch(Exception e) {
//        dbg.Caught(e);
//      } finally {
//        dbg.VERBOSE("Done calling ResultSet.findColumn().");
//      }
//    }
//    return getIntArrayColumnFromRS(rs, col);
//  }
//
//  protected int [ ] getIntArrayColumnFromRS(ResultSet rs, int column) {
//    int [ ] ret = new int [0];
//    int col = 1;
//    if(rs != null) {
//      org.postgresql.jdbc3.Jdbc3ResultSet rs3 = (org.postgresql.jdbc3.Jdbc3ResultSet)rs;
//      ret = new int [rs3.getTupleCount()];
//      for(int i = ret.length; i-->0;) {
//        boolean went = false;
//        try {
//          went = rs3.absolute(i+1);
//        } catch (Exception ex) {
//          dbg.Caught(ex);
//        }
//        if(went) {
//          ret[i] = getIntFromRS(column, rs);
//        } else {
//          dbg.ERROR("Could not load a tuple that the recordset said was there ["+(i+1)+"/"+ret.length+"]!");
//        }
//      }
//    }
//    return ret;
//  }
//
//  protected TextList getTextListColumnFromQuery(QueryString qs) {
//    return getTextListColumnFromQuery(qs, ONLYCOLUMN);
//  }
//
//  protected TextList getTextListColumnFromQuery(QueryString qs, int column) {
//    Statement stmt = query(qs);
//    if(stmt != null) {
//      try {
//        ResultSet rs = getResultSet(stmt);
//        return getTextListColumnFromRS(rs, column);
//      } catch (Exception ex) {
//        dbg.Caught(ex);
//      } finally {
//        closeStmt(stmt);
//      }
//    } else {
//      dbg.WARNING("getTextListColumnFromQuery() stmt=null!");
//    }
//    return null;
//  }
//
  public TextList getTextListColumnFromRS(ResultSet rs, ColumnProfile cp) {
    TextList tl = new TextList(50, 50);
    int col = 1;
    if ((cp != null) && (rs != null)) {
      try (Finally pop = dbg.Push("Calling ResultSet.findColumn() ...")) {
        col = rs.findColumn(cp.name());
      } catch (SQLException e) {
        dbg.Caught(e);
      }
    }
    while (next(rs)) {
      tl.add(getStringFromRS(col, rs));
    }
    return tl;
  }

  public long getLongFromQuery(QueryString queryStr) {
    return getLongFromQuery(queryStr, 0);
  }

  public long getLongFromQuery(QueryString queryStr, String fieldname) {
    return StringX.parseLong(StringX.TrivialDefault(getStringFromQuery(queryStr, fieldname), "-1"));
  }

  public long getLongFromQuery(QueryString queryStr, int field) {
    return StringX.parseLong(StringX.TrivialDefault(getStringFromQuery(queryStr, field), "-1"));
  }

  public int getIntFromQuery(QueryString queryStr) {
    return getIntFromQuery(queryStr, 0);
  }

  public int getIntFromQuery(QueryString queryStr, String fieldname) {
    return StringX.parseInt(StringX.TrivialDefault(getStringFromQuery(queryStr, fieldname), "-1"));
  }
//  private void checkDBthreadSelect() {
//    DBMacros should = PayMateDBDispenser.getPayMateDB();
//    DBMacros is = this;
//    if(should != is) {
//      TextList tl = dbg.whereAmI();
//      String msg = "Thread's db NOT= this!";
//      if(service != null) {
//        service.PANIC(msg);
//      }
//      dbg.ERROR(msg + "\n" + tl);
//    }
//  }

  public int getIntFromQuery(QueryString queryStr, ColumnProfile field) {
    return getIntFromQuery(queryStr, field.name());
  }

  public int getIntFromQuery(QueryString queryStr, int field) {
    return StringX.parseInt(StringX.TrivialDefault(getStringFromQuery(queryStr, field), "-1"));
  }

  public double getDoubleFromQuery(QueryString queryStr, int field) {
    return StringX.parseDouble(getStringFromQuery(queryStr, field));
  }

  public double getDoubleFromRS(int column, ResultSet myrs) {
    return StringX.parseDouble(getStringFromRS(column, myrs));
  }

  public double getDoubleFromRS(String column, ResultSet myrs) {
    return StringX.parseDouble(getStringFromRS(column, myrs));
  }

  /**
   * @returns an executed statement for the sql in @param queryStr, @param throwException is whether to allow exceptions else return null on failure
   */
  public Statement query(QueryString queryStr, boolean throwException) throws Exception {
    return query(queryStr, throwException, true /*canReattempt*/);
  }

  /**
   * @returns an executed statement for the sql in @param queryStr, @param throwException is whether to allow exceptions else return null on failure. @param canReattempt is whether to try to deal with exceptions not related to the query string
   */
  private Statement query(QueryString queryStr, boolean throwException, boolean canReattempt) throws SQLException {
//    checkDBthreadSelect();
    Statement stmt = null;
    SQLException mye = null;
    StopWatch swatch = new StopWatch();
    Connection mycon = null;
    boolean needsReattempt = false;
    long qn = queryCounter.incr();
    try (Finally pop = dbg.Push("query")) {
      mycon = getConnection();
      if (mycon != null) {
        dbg.VERBOSE("Calling Connection.createStatement() ...");
        // +++ deal with other possible parameters to this statement ...
        stmt = mycon.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        dbg.VERBOSE("Done calling Connection.createStatement().");
        String qs = String.valueOf(queryStr);
        dbg.VERBOSE("Calling Statement.executeQuery() ...");
//        preLogQuery(DBFunctionType.QUERY, queryStr, true, qn);
        if (stmt.executeQuery(qs) == null) {
          dbg.ERROR("query() generates a null statement!");
        }
        dbg.VERBOSE("Done calling Statement.executeQuery().");
      } else {
        dbg.ERROR("Cannot call query() unless connection exists!");
      }
    } catch (SQLException t) { // catch throwables (out of memory errors)? +++
      recycleConnection(throwException ? t : null, mycon, queryStr);
      needsReattempt = true;
      if (throwException) {
        mye = t;
        stmt = null;
      } else {
        String toLog = "Exception performing query: " + swatch.millis() + " ms = " + queryStr + "; [recycled] " + t;
        //if not thrown then let it die silently dbg.Caught(t, toLog);
        log(toLog);
      }
    }
    queryStats.add(swatch.millis());
    logQuery(DBFunctionType.QUERY, swatch.millis(), -1, true, qn);
    printWarnings(mycon);
    if (canReattempt && needsReattempt) { // +++ check the time it took last time?
      return query(queryStr, throwException, false /*canReattempt*/);
    } else {
      if (mye != null) {
        throw mye;
      }
      return stmt;
    }
  }

  protected void printWarnings(Connection mycon) {
    try {
      if (mycon != null) {
        SQLWarning warning = mycon.getWarnings();
        while (warning != null) {
          dbg.WARNING(warning.getMessage());
          // +++ do more things with the warning object; it has lots of "stuff" in it!
          warning = warning.getNextWarning();
        }
      }
    } catch (Exception ex) {
      dbg.Caught(ex);
    } finally {
      try {
        if (mycon != null) {
          mycon.clearWarnings();
        }
      } catch (Exception ex) {
        dbg.Caught(ex);
      }
    }
  }

  private boolean recycleConnection(Exception t, Connection mycon, QueryString originalQuery) {
    closeCon(mycon); // pray that nobody else is using it!  probably is hosed, either way

    if (t != null) {
      //    service.PANIC(msg);
      dbg.Caught(t, "DB.CONNCLOSED" + ": " + originalQuery + "\n");
    }
    return true;
  }

  /**
   * @returns an executed statement for the sql in @param queryStr, null on failure
   */
  public Statement query(QueryString queryStr) {
    try {
      return query(queryStr, false);
    } catch (Exception e) {
      dbg.Caught(e);
      return null;
    }
  }

  private int update(QueryString queryStr, boolean throwException) throws SQLException {
//    checkDBthreadSelect();
    if (queryStr == null) {
      dbg.WARNING("queryStr == null");
      return FAILED.asInt();
    }
    String qs = String.valueOf(queryStr);
    StopWatch swatch = new StopWatch();
    Connection mycon = getConnection();
    if (mycon == null) {
      return FAILED.asInt();
    }
    long qn = queryCounter.incr();
    try (Finally pop = dbg.Push("update {0}", (throwException ? "(RAW)" : "")); Statement stmt = mycon.createStatement()) {
      if (stmt != null) {
        // insert it into the table
        dbg.VERBOSE("Calling Connection.executeUpdate() ...");
//            preLogQuery(DBFunctionType.UPDATE, queryStr, true, qn);
        int retval = stmt.executeUpdate(qs); // this is where sql exceptions will most likely occur
        try {
          dbg.VERBOSE("Calling Connection.getAutoCommit() ...");
          if (!mycon.getAutoCommit()) {
            mycon.commit();
          }
        } catch (Exception e) {
          dbg.Caught(e);
        }
        return retval;
      } else {
        dbg.WARNING("Could not create a statement");
      }
      return FAILED.asInt(); // the error code
    } catch (SQLException e) {
      if (throwException) {
        throw e;
      }
      dbg.Caught(e, "Exception performing update: " + queryStr);
      recycleConnection(e, mycon, queryStr); // +++ testing this !!!
      return FAILED.asInt();
    }
//     finally {
//      try {
//        updateStats.add(swatch.millis());
//        logQuery(DBFunctionType.UPDATE, swatch.millis(), retval, true, qn);
//      } catch (Exception e2) {
//        dbg.Caught(e2);
//      } finally {
//        printWarnings(mycon);
//        if (canReattempt && needsReattempt) { // +++ check the time it took last time?
//          return update(queryStr, throwException, false /*canReattempt*/);
//        } else {
//          if (tothrow != null) {
//            dbg.WARNING("Throwing " + tothrow);
//            throw tothrow;
//          }
//        }
//      }
//    }
  }

  public int update(QueryString queryStr) {
    try {
      return update(queryStr, false);
    } catch (Exception e) {
      dbg.Caught(e);
      return FAILED.asInt();
    }
  }

  public final boolean getBooleanFromQuery(QueryString qry, int column) {
    return Bool.For(getStringFromQuery(qry, column));
  }

  public final boolean getBooleanFromQuery(QueryString qry) {
    return getBooleanFromQuery(qry, 0);
  }

  public final boolean getBooleanFromQuery(QueryString qry, ColumnProfile cp) {
    return Bool.For(getStringFromQuery(qry, cp.name()));
  }

  /**
   * Get the tables for the connected database
   */
  private ResultSet getTables(TableInfo ti) {
    ResultSet rs = null;
    try {
      //noinspection ConstantConditions
      if (false) {
        String types[] = {"BASE TABLE",//mysql's text for this.
          //--mysql doesn't allow query on this      "VIEW",
//        "SYSTEM TABLE",
//        "GLOBAL TEMPORARY",
//        "LOCAL TEMPORARY",
//        "ALIAS",
//        "SYNONYM",
        };
        rs = getDatabaseMetadata().getTables(ti.catalog(), ti.schema(), ti.name(), types);
      } else {//mysql stuff, can't get a handle on what they want in the above query but they don't like it.
        Statement stmt = makeStatement();   //need to query tables to get type and remark.
        String query = MessageFormat.format("SELECT * FROM information_schema.tables WHERE table_schema = ''{0}'' and table_name LIKE ''{1}'';", ti.schema(), ti.name());
        if (stmt.execute(query)) {
          rs = stmt.getResultSet();
        }
      }
      if (rs != null) {
        SQLWarning barf = rs.getWarnings();
        dbg.WARNING(barf.getMessage());
      }
    } catch (Exception e) {
      dbg.Caught(e);
    }
    return rs;
  }

  /**
   * @returns schema name that fits @param wildcard and is closest but no greater than @param limit
   */
  public String getSchemaLike(String wildcard, String limit) {
    String query = "select schema_name from information_schema.schemata where schema_name like ? and schema_name <= ? order by schema_name desc limit 1 "; //todo:1 delegate to Quirks class.
    try (PreparedStatement pst = makePreparedStatement(query)) {
      if (pst != null) {
        pst.setString(1, wildcard);
        pst.setString(2, limit);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
          return rs.getString(1);
        } else {
          return null;
        }
      } else {
        dbg.ERROR("Couldn't connect to server to find current schema.");
        return null;
      }
    } catch (SQLException e) {
      dbg.Caught(e);
      return null;
    }
  }

  /**
   * used by getTableList so that we can watch it execute, no one else should use this.
   */
  private String getTableInfo(ResultSet rs, String info) {
    try (Finally pop = dbg.Push("getTableInfo")) {
      dbg.VERBOSE("Calling ResultSet.getString() regarding: " + info + "...");
      return rs.getString(info);
    } catch (SQLException e) {
      dbg.ERROR("Excepted trying to get field {0} from result set threw {1}.", info, e);
      return "";
    }
  }

  public PreparedStatementInserter psi(QueryString qry, int batchSize) throws SQLException {
    PreparedStatementInserter psier = new PreparedStatementInserter(makePreparedStatement(qry.toString()));
    psier.batchSize = batchSize;
    return psier;
  }

  public PreparedStatementInserter psi(QueryString qry) throws SQLException {
    return psi(qry, 1);
  }

  public static boolean Ok(ResultSet rs) throws SQLException {
    return rs != null && rs.next();
  }


  /**
   * check whether a table exists. If you put wildcards in @param ti all this will tell you is that something like that exists.
   */
  public boolean tableExists(TableInfo ti) {
    QueryString qry = QueryString.SelectAll(InfoTable).wherePrepared("table_schema", "table_name");

    try (PreparedStatementInserter psi = psi(qry)) {
      psi.setMany(ti.schema(), ti.name());
      ResultSet rs = psi.execute();
      return Ok(rs);//if there is a record then the table exists
    } catch (SQLException mysqlisabysmal) {
      return false;
    }
  }

  /**
   * +++ Make this create TableProfile objects with the right names, then fill those objects with their data instead of passing String names around.
   */
  public DatabaseProfile profileDatabase(String databaseName, String tablename, boolean sort) {
    dbg.VERBOSE("Profiling database: {0} ...", databaseName);
    SimpleTableInfo tq = new SimpleTableInfo(null, databaseName, tablename, null, null);
    DatabaseProfile tables = new DatabaseProfile(databaseName);
    //noinspection StringEquality
    if (tablename != ALLTABLES) {
      TableProfile tp = profileTable(tq);
      if (sort) {
        tp.sort();
      }
      tables.add(tp);
    } else { // Profile ALLTABLES
      TableInfoList tablelist = getTableList(tq);
      for (int i = 0; i < tablelist.size(); i++) {
        tq.setName(tablelist.itemAt(i).name());
        TableProfile tp = profileTable(tq);
        if (tp.numColumns() > 0) {
          if (sort) {
            tp.sort();
          }
          tables.add(tp);
        } else {
          dbg.ERROR("Number of columns in table is ZERO: " + tp.name());
        }
      }
    }
    dbg.VERBOSE("DONE profiling database: " + databaseName + ".");
    return tables;
  }

  /**
   * Don't let outside classes use Strings for tablenames.  Stick with objects...
   */
  public TableProfile profileTable(TableProfile table) {
    return profileTable(table.tq());
  }

  private TableProfile profileTable(TableInfo tq) {
    Vector<ColumnProfile> cols = new Vector<>(100);
    TableProfile tp = TableProfile.create(tq, null, new ColumnProfile[0]);
    dbg.VERBOSE("profiling table '{0}'", tq.name());
    try (ResultSet cpmd = getDatabaseMetadata().getColumns(tq.catalog(), tq.schema(), tq.name(), null)) {
      if (cpmd != null) {
        while (next(cpmd)) {
          ColumnProfile cp = null;
          try {
            String tablename = StringX.TrivialDefault(getStringFromRS("TABLE_NAME", cpmd), "");
            String columnname = StringX.TrivialDefault(getStringFromRS("COLUMN_NAME", cpmd), "");
            String type = StringX.TrivialDefault(getStringFromRS("TYPE_NAME", cpmd), "").toUpperCase();
            String size = StringX.TrivialDefault(getStringFromRS("COLUMN_SIZE", cpmd), "");
            String nullable = StringX.TrivialDefault(getStringFromRS("IS_NULLABLE", cpmd), "");
            dbg.VERBOSE("About to create ColumnProfile for " + tablename + "." + columnname + " with type of " + type + "!");
            cp = ColumnProfile.create(tp, columnname, type, size, nullable);
            cp.tableCat = StringX.TrivialDefault(getStringFromRS("TABLE_CAT", cpmd), "");
            cp.tableSchem = StringX.TrivialDefault(getStringFromRS("TABLE_SCHEM", cpmd), "");
            cp.decimalDigits = StringX.TrivialDefault(getStringFromRS("DECIMAL_DIGITS", cpmd), "");
            cp.numPrecRadix = StringX.TrivialDefault(getStringFromRS("NUM_PREC_RADIX", cpmd), "");
            cp.nullAble = StringX.TrivialDefault(getStringFromRS("NULLABLE", cpmd), "");
            cp.remarks = StringX.TrivialDefault(getStringFromRS("REMARKS", cpmd), "");
            String columnDefTemp = StringX.TrivialDefault(getStringFromRS("COLUMN_DEF", cpmd), "");
            cp.setDefaultFromDB(columnDefTemp);
            cp.charOctetLength = StringX.TrivialDefault(getStringFromRS("CHAR_OCTET_LENGTH", cpmd), "");
            cp.ordinalPosition = StringX.TrivialDefault(getStringFromRS("ORDINAL_POSITION", cpmd), "");
          } catch (Exception e2) {
            dbg.Caught(e2);
          }
          if (cp != null) {
            cols.add(cp);
            dbg.VERBOSE("Adding column " + cp.name());
          }
        }
      } else {
        dbg.ERROR("cpmd is NULL!");
      }
    } catch (Exception e) {
      dbg.Caught(e);
    }
    ColumnProfile columns[] = new ColumnProfile[cols.size()];
    for (int i = 0; i < cols.size(); i++) {
      columns[i] = cols.elementAt(i);
    }
    tp.join(columns);
    return tp;
  }

  /**
   * @returns true if the table is gone
   */
  public final boolean dropTable(TableInfo tablename) {
    try (Finally pop = dbg.Push("dropTable")) {
      //todo:1 tryspool(ddl)&&
      final int update = update(modeler.genDropTable(tablename.fullName()));
      dbg.WARNING("DropTable {0} update returned {1}", tablename, update);
      return !tableExists(tablename);
    }
  }
//  /** checks the default schema for MINUTES before it decides whether to do something stupid ...*/
//  public boolean tableExists(String tableName) {
//    if (StringX.NonTrivial(tableName)) {
//      TableInfo tq = new SimpleTableInfo(tableName);
//      TableInfoList tablelist = getTableList(tq);
//      for (int i = tablelist.size(); i-- > 0; ) {
//        if (tablelist.itemAt(i).name().equalsIgnoreCase(tableName)) {
//         return true;
//        }
//      }
//    }
//    return false;
//  }

  public boolean fieldExists(TableInfo tableName, String fieldName) {
    TableProfile tp = profileTable(tableName);
    // skip through and look for the fieldname
    return tp != null && tp.fieldExists(fieldName);
  }

  public boolean primaryKeyExists(PrimaryKeyProfile primaryKey) {
    try (Finally pop = dbg.Push("Calling DatabaseMetadata.getPrimaryKeys()")) {
      String tablename = primaryKey.table.name().toLowerCase(); // MUST BE LOWER for PG
      try (ResultSet rs = getDatabaseMetadata().getPrimaryKeys(null/*catalog*/, null/*schema*/, tablename)) {
        if (rs != null) {
          while (next(rs)) {
            String cname = getStringFromRS("PK_NAME", rs);
            if (primaryKey.name.equalsIgnoreCase(cname)) {
              return true;
//              break;
            } else {
              dbg.ERROR("Primary key test: '" + cname + "' is not the same as '" + primaryKey.name + "'.");
            }
          }
        } else {
          dbg.ERROR("ResultSet = NULL !!!");
        }
        String disp = primaryKey.table.name() + " primary key constraint " + primaryKey.name + " does NOT exist.";
        return false;
      } catch (SQLException e) {
        dbg.Caught(e);
        return false;
      }
    }
  }

  public boolean foreignKeyExists(ForeignKeyProfile foreignKey) {
    boolean ret = false;
    DatabaseMetaData pmdmd = getDatabaseMetadata();
    // +++ put this into the table profiler
    ResultSet rs = null;
    try {
      String tablename = foreignKey.table.name().toLowerCase(); // MUST BE LOWER for PG
      rs = pmdmd.getImportedKeys(null, null, tablename);
      while (next(rs)) {
        String cname = modeler.extractFKJustName(getStringFromRS("FK_NAME", rs));
        if (foreignKey.name.equalsIgnoreCase(cname)) {
          ret = true;
          break;
        }
      }
    } catch (Exception e) {
      dbg.Caught(e);
    } finally {
      closeRS(rs);
    }
    dbg.VERBOSE(foreignKey.table.name() + " foreign key constraint " + foreignKey.name + " does " + (ret ? "" : "NOT ") + "exist.");
    return ret;
  }

  public boolean indexExists(IndexProfile index) {
    return indexExists(index.name, index.table.name());
  }
//  public static final int getColumnCount(ResultSetMetaData rsmd) {
//    int ret = 0;
//    try {
//      dbg.VERBOSE("Calling ResultSetMetaData.getColumnCount() ...");
//      ret = rsmd.getColumnCount();
//    } catch (Exception e) {
//      dbg.ERROR("getColumnCount() excepted attempting to get column count from the ResultSetMetaData");
//    } finally {
//      dbg.VERBOSE("Done calling ResultSetMetaData.getColumnCount().");
//      return ret;
//    }
//  }
//
//  public int getColumnCount(ResultSet rs) {
//    return getColumnCount(getRSMD(rs));
//  }
//  public static final int NAME    = 0;
//  public static final int TYPE    = 1;
//  public static final int SIZE    = 2;
//  public static final int NULLABLE= 3;
//
//  public static final String ISNOTNULLABLE = "NOT NULL";
//  public static final String ISNULLABLE = "";
//
//  public static final String getColumnAttr(ResultSetMetaData rsmd, int what, int rsCol) {
//    String ret = null;
//    try {
//      switch(what) {
//        case NAME: {
//          try {
//            dbg.VERBOSE("Calling ResultSetMetaData.getColumnLabel() ...");
//            ret = rsmd.getColumnLabel(rsCol);
//          } finally {
//            dbg.VERBOSE("Done calling ResultSetMetaData.getColumnLabel().");
//          }
//        } break;
//        case TYPE: {
//          try {
//            dbg.VERBOSE("Calling ResultSetMetaData.getColumnTypeName() ...");
//            ret = rsmd.getColumnTypeName(rsCol);
//          } finally {
//            dbg.VERBOSE("Done calling ResultSetMetaData.getColumnTypeName().");
//          }
//        } break;
//        case SIZE: {
//          try {
//            dbg.VERBOSE("Calling ResultSetMetaData.getColumnDisplaySize() ...");
//            ret = "" + rsmd.getColumnDisplaySize(rsCol);
//          } finally {
//            dbg.VERBOSE("Done calling ResultSetMetaData.getColumnDisplaySize().");
//          }
//        } break;
//        case NULLABLE: {
//          try {
//            dbg.VERBOSE("Calling ResultSetMetaData.isNullable() ...");
//            ret = ((rsmd.isNullable(rsCol) == ResultSetMetaData.columnNullable) ? ISNULLABLE: ISNOTNULLABLE);
//          } finally {
//            dbg.VERBOSE("Done calling ResultSetMetaData.isNullable().");
//          }
//        } break;
//      }
//    } catch (Exception e) {
//      dbg.ERROR("getColumnAttr() excepted attempting to get column info from the ResultSetMetaData");
//      dbg.Caught(e);
//    }
//    return ret;
//  }

  public boolean indexExists(String indexName, String tablename) {
    // +++ put this into the table profiler
    if (indexName != null) {
      indexName = indexName.trim();
      ResultSet rs = null;
      try {
        tablename = tablename.toLowerCase(); // MUST BE LOWER CASE for PG !
        dbg.VERBOSE("Running DatabaseMetadata.getIndexInfo() ...");
        rs = getDatabaseMetadata().getIndexInfo(null, null, tablename, false, true);
      } catch (Exception e) {
        dbg.Caught(e);
      } finally {
        dbg.VERBOSE("Done running DatabaseMetadata.getIndexInfo().");
      }
      if (rs != null) {
        try {
          while (next(rs)) {
            String cname = getStringFromRS("INDEX_NAME", rs).trim();
            if (indexName.equalsIgnoreCase(cname)) {
              dbg.VERBOSE("indexName=" + indexName + ", cname=" + cname + ", equal!");
              return true;
            }
            dbg.VERBOSE("indexName=" + indexName + ", cname=" + cname + ", NOT equal!");
          }
        } catch (Exception e) {
          dbg.Caught(e);
        } finally {
          closeRS(rs);
        }
      } else {
        dbg.ERROR("ResultSet = NULL !!!");
      }
      dbg.VERBOSE("Index " + indexName + " does NOT  exist.");
    } else {
      dbg.ERROR("IndexName = NULL !!!");
    }
    return false;
  }

  /**
   * Returns true if the field was properly dropped
   */
  protected final boolean dropField(ColumnProfile column) {
    try (Finally pop = dbg.Push("dropField")) {
      if (fieldExists(column.tq(), column.name())) {
        if (!getDatabaseMetadata().supportsAlterTableWithDropColumn()) {//todo:1 cache all such attributes
          dbg.ERROR("dropField " + column.fullName() + ": was not able to run since the DBMS does not support it!");
          return true;
        } else {
//          dbg.ERROR("dropField " + column.fullName() + ": returned " + update(QueryString.genDropField(column)));
          return !fieldExists(column.tq(), column.name());
        }
      } else {
        dbg.ERROR("dropField " + column.fullName() + ": already dropped.");
        return true;
      }
    } catch (SQLException e) {
      dbg.Caught(e, "Dropping column {0}", column);
      return false;
    }
  }

  /**
   * @returns true if the index no logner exists
   */
  protected final boolean dropIndex(String indexname, String tablename) {
    try (Finally pop = dbg.Push("dropIndex")) {
      if (indexExists(indexname, tablename)) {
        return update(modeler.genDropIndex(indexname)) >= 0;
      } else {
        dbg.ERROR("dropIndex {0}: index doesn't exist; can't drop.", indexname);
        return true;
      }
    }
  }

  class NamedStatementList extends Vector<NamedStatement> {
    private ErrorLogStream dbg;

    public NamedStatementList(ErrorLogStream dbg) {
      this.dbg = dbg;
    }

    public NamedStatement itemAt(int index) {
      NamedStatement retval = null;
      if ((index < size()) && (index >= 0)) {
        retval = elementAt(index);
      }
      return retval;
    }

    public void Remove(Statement stmt) {
      boolean removed = false;
      //todo: Vector should now have a remove item if present
      for (NamedStatement ns : this) {
        if ((ns != null) && ns.equals(stmt)) {
          removed = remove(ns);
          break;
        }
      }
      if (dbg != null) {
        if (removed) {
          dbg.VERBOSE("Statement FOUND for removal: " + stmt);
        } else {
          dbg.WARNING("Statement NOT found for removal: " + stmt);
        }
      }
    }
  }

  // +++ use dbmd's getMaxColumnNameLength to see if the name is too long
  protected final boolean addField(ColumnProfile column) {
    try (Finally pop = dbg.Push("addField")) {
      if (!fieldExists(column.tq(), column.name())) {
        dbg.ERROR("addField {0} returned {1}", column.fullName(), update(/* +++ use: db.generateColumnAdd(tp, cp) (or something similar) instead! */
          modeler.genAddField(column)));
        return fieldExists(column.tq(), column.name());
      } else {
        return true;
      }
    }
  }

  protected final boolean changeFieldType(ColumnProfile from, ColumnProfile to) {
    try (Finally pop = dbg.Push("changeFieldType")) {
//      dbg.ERROR("changeFieldType " + to.fullName() + " returned " + update(QueryString.genChangeFieldType(to)));
      dbg.ERROR("changeFieldType is not yet supported.  Write code in the content validator to handle this!");
      return false;//fieldExists(to.table().name(), to.name()); // +++ instead, need to do the things you do to check that a column is correct, not just check to see if it is added!
    }
  }
//  // for this version of postgresql jdbc, the fetch size is the WHOLE thing!
//  public static final int getResultSetFetchSize(ResultSet rs) {
//    try {
//      return rs.getFetchSize();
//    } catch (Exception ex) {
//      dbg.Caught(ex);
//      return -1;
//    }
//  }
  ////////////////////////////////////
  // database profiling

  protected final boolean changeFieldNullable(ColumnProfile to) {
    try (Finally pop = dbg.Push("changeFieldNullable")) {
      dbg.ERROR("changeFieldNullable " + to.fullName() + " returned " + update(modeler.genChangeFieldNullable(to)));
      TableProfile afterTable = profileTable(to.tq());
      ColumnProfile aftercolumn = afterTable.column(to.name());
      return to.sameNullableAs(aftercolumn);
    }
  }

  protected final boolean changeFieldDefault(ColumnProfile to) {
    try (Finally pop = dbg.Push("changeFieldDefault")) {
      dbg.ERROR("changeFieldDefault " + to.fullName() + " returned " + update(modeler.genChangeFieldDefault(to)));
      TableProfile afterTable = profileTable(to.tq());
      ColumnProfile aftercolumn = afterTable.column(to.name());
      return to.sameDefaultAs(aftercolumn);
    }
  }
//  public int getColumnSize(String tablename, String fieldname) {
//    int ret = 0;
//    TableProfile dbp = profileTable(tablename);
//    if(dbp != null) {
//      ColumnProfile cp = dbp.column(fieldname);
//      if(cp != null) {
//        ret = cp.size();
//      } else {
//        dbg.ERROR("getColumnSize is returning " + ret + " since the cp is null!");
//      }
//    } else {
//      dbg.ERROR("getColumnSize is returning " + ret + " since the dbp is null!");
//    }
//    return ret;
//  }
//  private static final EasyProperties ezp = new EasyProperties();
//  private static boolean ezpset = false;
//  private static final Monitor javaSqlTypesMonitor = new Monitor(DBMacros.class.getName()+".javaSqlTypes");
//  public static final String javaSqlType(String typeNum) {
//    String ret = "";
//    int type = StringX.parseInt(typeNum);
//    // lock
//    try {
//      javaSqlTypesMonitor.getMonitor();
//      // only have to do this once per run
//      if(!ezpset) {
//        // use reflection to find every 'public final static int' in the class java.sql.Types, and add the item to ezp
//        try {
//          Class c = java.sql.Types.class;
//          Field[] fields = c.getFields();
//          for(int i = fields.length; i-->0;) {
//            Field field = fields[i];
//            int ivalue = field.getInt(c); // probably doesn't work
//            String value = String.valueOf(ivalue);
//            ezp.setString(value, field.getName());
//          }
//        } catch (Exception e) {
//          /* abandon all hope ye who enter here */
//        }
//        ezpset = true;
//      }
//    } finally {
//      javaSqlTypesMonitor.freeMonitor();
//    }
//    ret = ezp.getString(typeNum, "");
//    return ret;
//  }

  protected final Op validateAddIndex(IndexProfile index) {
    String indexName = index.name;
    String tableName = index.table.name();
    String fieldExpression = index.columnNamesCommad();
    try (Finally pop = dbv.Push("validateAddIndex")) {
      dbv.mark("Add Index " + indexName + " for " + tableName + ":" + fieldExpression);
      if (!indexExists(index)) {
        QueryString qs = modeler.genCreateIndex(index);
        if (update(qs) != -1) {
          return DONE;
        } else {
          return FAILED;
        }
      } else {
        return ALREADY;
      }
    }
  }

  protected final Op validateAddField(ColumnProfile column) {
    try (Finally pop = dbv.Push("validateAddField(fromProfile)")) {
      dbv.mark("Add field " + column.fullName());
      if (!fieldExists(column.tq(), column.name())) {
        boolean did = addField(column);
        Op success = (did ? DONE : FAILED);
        dbv.ERROR("{0} field {1}", (success.Ok() ? "Added" : "!! COULD NOT ADD"), column.fullName());
        return success;
      } else {
        dbv.ERROR("Field {0} already added.", column.fullName());
        return ALREADY;
      }
    }
  }

  // +++ generalize internal parts between validate functions!
  protected final Op validateAddPrimaryKey(PrimaryKeyProfile primaryKey) {
    try (Finally pop = dbv.Push("validateAddPrimaryKey")) {
      String blurb = "Primary Key " + primaryKey.name + " for " + primaryKey.table.name() + "." + primaryKey.field.name();
      dbv.mark("Add " + blurb);
      if (!primaryKeyExists(primaryKey)) {
        QueryString qs = modeler.genAddPrimaryKeyConstraint(primaryKey);
        if (update(qs) != -1) {
          return DONE;
        } else {
          return FAILED;
        }
      } else {
        return ALREADY;
      }
    }
  }
  //todo:2 use dbmd's getMaxColumnNameLength to see if the name is too long

  // +++ generalize internal parts and move to DBMacros!
  protected final Op validateAddForeignKey(ForeignKeyProfile foreignKey) {
    String functionName = "validateAddForeignKey";
    try (Finally pop = dbv.Push(functionName)) {
      String blurb = "Foreign Key " + foreignKey.name + " for " + foreignKey.table.name() + "." + foreignKey.field.name() + " against " + foreignKey.referenceTable.name();
      dbv.mark("Add " + blurb);
      if (!foreignKeyExists(foreignKey)) {
        QueryString qs = modeler.genAddForeignKeyConstraint(foreignKey);
        if (update(qs) != -1) {
          dbv.VERBOSE(": SUCCEEDED!");
          return DONE;
        } else {
          return FAILED;
        }
      } else {
        dbv.VERBOSE(blurb + " already added.");
        return ALREADY;
      }
    }
  }
//  protected final Op validateAddTable(TableProfile table) {
//    String functionName = "validateAddTable";
//    try (Finally pop = dbv.Push(functionName)) {
//      dbv.mark("Add table " + table.name());
//      if (!tableExists(table.name())) {
//        boolean did = createTable(table);
//        dbv.ERROR((did ? "Added" : "!! COULD NOT ADD") + " table " + table.name());
//        return (did ? DONE : FAILED);
//      } else {
//        dbv.VERBOSE("Table " + table.name() + " already added.");
//        return ALREADY;
//      }
//    }
//  }
//  protected final boolean dropTableConstraint(String tablename, String constraintname) {
//    String toDrop = "drop constraint " + tablename + "." + constraintname;
//    try (Finally pop = dbv.Push("dropTableConstraint")) {
//      TableProfile tempprof = TableProfile.create(new SimpleTableInfo(tablename), null, null);
//      return !tableExists(tablename) || update(modeler.genDropConstraint(tempprof, new Constraint(constraintname, tempprof, null))) >= 0;
//    }
//  }

  protected final boolean renameField(String table, String oldname, String newname) {
    int count = 0;
    TableInfo tq = new SimpleTableInfo(table);
    if (fieldExists(tq, oldname)) {
      count = update(modeler.genRenameColumn(table, oldname, newname));
    }
    return (count == 0);
  }
//  //todo:1 use dbmd's getMaxTableNameLength to see if the name is too long
//  protected final boolean createTable(TableProfile tp) {
//    try (Finally pop = dbg.Push("haveTable")) {
//      if (!tableExists(tp.name())) {
//        dbg.ERROR("haveTable " + tp.name() + " returned " + update(modeler.genCreateTable(tp)));
//        return tableExists(tp.name());
//      } else {
//        return true;
//      }
//    }
//  }

  public Blob makeBlob(byte[] content) {
    return new Blobber(content);
  }

  public Blob makeBlob(String content) {
    if (content != null) {
      return new Blobber(content.getBytes());
    } else {
      return new Blobber("");//todo:1 decide whether null blobs are a good thing.
    }
  }

  /**
   * make and execute a statement, call the actor if the resultset has something in it.
   *
   * @returns whether a non-empty results set was actually acquired, and if so actor has been invoked. (it won't have been if there is an sql exception)
   */
  public boolean doSimpleQuery(String query, Consumer<ResultSet> actor) {
    try (Statement stmt = makeStatement()) {
      StopWatch timer = new StopWatch(true);
      stmt.setFetchSize(Integer.MIN_VALUE);//hint that we want row at a time fetching from the server, necessary for large sets and irrelevangt for small.
      try (ResultSet rs = stmt.executeQuery(query)) {//allow null strings to throw an exception, so that the log tells the user how they screwed up
        dbg.WARNING("Simplequery took {0} seconds, {1}", timer.seconds(), query);
        if (rs.next()) {
          if (actor != null) {
            actor.accept(rs);
          }
          return true;//#don't conditionalize upon actor being invoked.
        } else {
          return false;
        }
      }
    } catch (SQLException e) {
      dbg.Caught(e, query);
      return false;
    }
  }

  /**
   * @returns an object created by @param generator which is given a resultset from executing @param query
   */
  public <T> T getObject(String query, Function<ResultSet, T> generator) {
    try (Statement stmt = makeStatement()) {
      ResultSet rs = stmt.executeQuery(query);//allow null strings to throw an exception, so that the log tells the user how they screwed up
      if (rs.next() && (generator != null)) {
        return generator.apply(rs);
      } else {
        return null;
      }
    } catch (SQLException e) {
      dbg.Caught(e, query);
      return null;
    }
  }

  /**
   * make and execute a statement, call the actor passing whether there is a result set.
   *
   * @returns whether actor has been invoked (true even if it is null ;). Will be false if there is an sql exception but also if there is no result set.
   */
  public boolean doSimpleAction(String query, Consumer<Boolean> actor) {
    try (Statement stmt = makeStatement()) {
      boolean b = stmt.execute(query);
      if (actor != null) {
        actor.accept(b);
      }
      return true;
    } catch (SQLException e) {
      dbg.Caught(e, query);
      return false;
    }
  }

  /**
   * @returns integer from executing @param selector, -1 on some of the faults. If -1 is a legitimate response and not acceptible on error then don't use this convenience method.
   */
  public int getIntFrom(PreparedStatement selector, int whichcolumn) throws SQLException {
    final ResultSet rs = selector.executeQuery();
    return rs.next() ? getIntFromRS(whichcolumn, rs) : BadIndex;
  }

  /**
   * @param actor will receive a result set that has already been next'ed once, use a do{}while(rs.next());, else has already been exhausted.
   * @returns whether actor was called at least once.
   */
  public boolean doRows(String query, Consumer<ResultSet> actor) {
    try (Statement st = makeStatement()) {
      ResultSet rs = st.executeQuery(query);
      if (rs.next() && actor != null) {
        actor.accept(rs);
        return true;
      } else {
        return false;
      }
    } catch (SQLException e) {
      dbg.Caught(e, query);
      return false;
    }
  }

  /**
   * if the query doesn't actually have any prepared params then give @param prepare a null.
   *
   * @param actor will receive a result set that has already been next'ed once, use a do{}while(rs.next());, else has already been exhausted.
   * @returns whether actor was called at least once.
   */
  public boolean doPreparedStatement(String query, Consumer<PreparedStatement> preparer, Consumer<ResultSet> actor) {
    try (PreparedStatement pst = makePreparedStatement(query)) {
      if (preparer != null) {
        preparer.accept(pst);
      }
      ResultSet rs = pst.executeQuery();//autocloses when statement closes.
      if (rs.next() && actor != null) {
        actor.accept(rs);
        return true;
      } else {
        return false;
      }
    } catch (SQLException e) {
      dbg.Caught(e, query);
      return false;
    }
  }

  /**
   * @param actor will receive a result set that if @param iterate is false has already been next'ed once, use a do{}while(rs.next());, else has already been exhausted.
   * @returns whether actor was called at least once.
   */
  public boolean doPreparedStatement(PreparedStatement pst, Consumer<ResultSet> actor, boolean iterate) {
    try (ResultSet rs = pst.executeQuery()) {
      if (actor != null) {
        if (rs.next()) {//always try once
          actor.accept(rs);
          if (iterate) {
            while (rs.next()) {
              actor.accept(rs);
            }
          }
          return true;
        } else {
          return false;
        }
      } else {
        return false;
      }
    } catch (SQLException e) {
      dbg.Caught(e, "running" + pst);
      return false;
    }
  }

  /**
   * @param <T> the type of the input to the operation
   * @since 1.8
   */
  @FunctionalInterface
  public interface SqlConsumer<T> {

    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     */
    void accept(T t) throws SQLException;
    //add more Consumer features as needed
  }

  /**
   * insert value[s] and run a query that of a nature that never generates a resultset. @returns whether operation executed, any results or updatecount are lost.
   */
  public boolean doPreparedDML(String query, SqlConsumer<PreparedStatement> preparer) {
    try (PreparedStatement pst = makePreparedStatement(query)) {
      if (preparer != null) {
        preparer.accept(pst);
      }
      pst.execute();
      return true;
    } catch (SQLException e) {
      dbg.Caught(e, query);
      return false;
    }
  }

  public ResultSet getColumns(TableInfo ti) throws SQLException {
    //noinspection ConstantConditions
    return getDatabaseMetadata().getColumns(null, ti.schema(), ti.name(), null);
  }

  public static ResultSetMetaData getRSMD(ResultSet rs) {
    ResultSetMetaData rsmd = null;
    if (rs != null) {
      try {
        dbg.VERBOSE("Calling ResultSet.getMetaData() ...");
        rsmd = rs.getMetaData();
      } catch (SQLException e) {
        dbg.ERROR("Exception occurred getting ResultSetMetaData!");
        dbg.Caught(e);
      } finally {
        dbg.VERBOSE("Done calling ResultSet.getMetaData().");
      }
    }
    return rsmd;
  }

  public static EasyProperties colsToProperties(ResultSet rs, ColumnProfile ignoreColumn) {
    EasyProperties ezc = new EasyProperties();
    try {
      if ((rs != null) && (next(rs))) {
        ResultSetMetaData rsmd = getRSMD(rs);
        for (int col = rsmd.getColumnCount() + 1; col-- > 1; ) {// functions start with column 1!
          String name = rsmd.getColumnName(col);
          if (ObjectX.NonTrivial(ignoreColumn) && StringX.equalStrings(ignoreColumn.displayName(), name)) {
            dbg.WARNING("colsToProperties: Ignoring field " + ignoreColumn.fullName());
          } else {
            ColumnType dbt = ColumnType.valueOf(rsmd.getColumnTypeName(col));
            if (dbt == ColumnType.BOOL) {
              ezc.setBoolean(name, getBooleanFromRS(col, rs));
            } else {
              ezc.setString(name, getStringFromRS(col, rs));
            }
          }
        }
      } else {
        dbg.ERROR("Can't convert cols to properties if resultset is null!");
      }
    } catch (Exception ex) {
      dbg.Caught(ex);
    }
    return ezc;
  }

  private static void logQuery(DBFunctionType qt, long millis, int retval, boolean regularLog, long qn) {
    try (AutoCloseable free = timerStrMon.getMonitor()) {
      StringBuilder toLog = new StringBuilder();//"exception attempting to log the query";
      // create a pool of DBFunctionTypes and Fstrings that you can reuse [no mutexing == faster].
      toLog.append(qt);
      if (qt != DBFunctionType.NEXT) {
        toLog.append(" END   ").append(qn).append(" ");
      }
      toLog.append(timerStr.righted(String.valueOf(millis))).append(" ms ");
      if (qt == DBFunctionType.UPDATE) {
        toLog.append(" returned ").append(retval);
      }
      if (regularLog) {
        dbg.WARNING(toLog.toString());
      }
      log(toLog.toString());
    } catch (Exception e) {
      dbg.Caught(e);
    }
  }

  private static void preLogQuery(int qt, QueryString query, boolean regularLog, long qn) {
    try {
      DBFunctionType qtype = DBFunctionType.class.getEnumConstants()[qt];
      String toLog = qtype.toString() + " BEGIN " + qn + " = " + query;
      if (regularLog) {
        dbg.WARNING(toLog);
      }
      log(toLog);
    } catch (Exception e) {
      dbg.Caught(e);
    }
  }

  public static String getStringFromRS(String fieldName, ResultSet myrs) {
    return getStringFromRS(NOT_A_COLUMN, fieldName, myrs);
  }

  public static String getStringFromRS(ColumnProfile field, ResultSet myrs) {
    return getStringFromRS(field.name(), myrs);
  }

  // presumed only done once by one thread
  public static boolean init() {
//    if (service == null) {
//      service = new DBMacrosService(new PayMateDBDispenser()); // setup the service
//      service.initLog();
//    }
//    pf = service.logFile.getPrintFork();
//    pf.println("inited OK");
    return true;
  }

  private static void log(String toLog) {
    if (pf != null) {
      try {
        pf.println(toLog);
      } catch (Exception e) {
        dbg.Caught(e);
      }
    }
  }

  /**
   * @returns sum of non-error-code int returns from executing a batch statement
   */
  public static int doBatch(Statement pst) throws SQLException {
    int[] qty = pst.executeBatch();
    int batchsum = 0;
    for (int one : qty) {
      if (one > 0) {
        batchsum += one;
      }
      //else negatives are error codes, we could sum them up separately ...
    }
    return batchsum;
  }

  public static String getStringFromRS(int column, ResultSet myrs) {
    return getStringFromRS(column, null, myrs);
  }

  private static String getStringFromRS(int column, String fieldName, ResultSet myrs) {
    String ret = null;
    try {
      if (myrs != null) {//valid non empty resultset
        if (column < 1) {//code that tells us to use the name field
          try (Finally pop = dbg.Push("ResultSet.findColumn()")) {
            column = myrs.findColumn(fieldName);//excepts if results set is empty!
          } catch (SQLException ex) {
            dbg.VERBOSE("Coding error: \n" + ErrorLogStream.whereAmI());
            dbg.VERBOSE(ex.getMessage() + " - see next line for args...");
          }
        }
        if (column < 1) {//happens when field is present but result set is empty.
          dbg.ERROR("getStringFromRS: column [{0}] (not included in query?): ", fieldName);
        } else {
          try (Finally pop = dbg.Push("ResultSet.getString()")) {
            ret = myrs.getString(column);
          }
        }
      }
    } catch (Exception t) {
      dbg.Caught(t, "getStringFromRS: Exception getting column [{0}/{1}] (not included in query?)", column, fieldName);
    }
    return StringX.TrivialDefault(ret, "").trim();
  }

  public static int getIntFromRS(int column, ResultSet myrs) {
    return StringX.parseInt(getStringFromRS(column, myrs));
  }

  public static int getIntFromRS(String column, ResultSet myrs) {
    return StringX.parseInt(getStringFromRS(column, myrs));
  }

  public static int getIntFromRS(ColumnProfile column, ResultSet myrs) {
    return getIntFromRS(column.name(), myrs);
  }

  public static long getLongFromRS(String column, ResultSet myrs) {
    return StringX.parseLong(getStringFromRS(column, myrs));
  }

  public static long getLongFromRS(ColumnProfile column, ResultSet myrs) {
    return getLongFromRS(column.name(), myrs);
  }

  public static boolean getBooleanFromRS(int column, ResultSet myrs) {
    return Bool.For(getStringFromRS(column, myrs));
  }

  public static boolean getBooleanFromRS(String column, ResultSet myrs) {
    return Bool.For(getStringFromRS(column, myrs));
  }

  public static boolean getBooleanFromRS(ColumnProfile column, ResultSet myrs) {
    return getBooleanFromRS(column.name(), myrs);
  }

  public static boolean next(ResultSet rs) {
    return next(rs, null);
  }

  // a timer is passed in so that it can be used,
  // in case the caller wants to know how long it took.
  // if the timer is null, we create one locally,
  // as we use that timer to add times to our accumulator for service reporting.
  public static boolean next(ResultSet rs, StopWatch swatch) {
    if (rs != null) {
      if (swatch == null) {
        swatch = new StopWatch(false);
      }
      swatch.Start(); // to be sure to start either passed-in or locally-created ones
      try {
//        dbg.VERBOSE("Calling ResultSet.next() ...");
        return rs.next();
      } catch (Exception e) {
        dbg.ERROR("next() excepted attempting to get the next row in the ResultSet ... ");
        dbg.Caught(e);
        return false;
      } finally {
        long dur = swatch.Stop();
        nextStats.add(dur);
//        dbg.VERBOSE("Done calling ResultSet.next().");
        if (dur > 0) {
          logQuery(DBFunctionType.NEXT, dur, -1, false, -1);
        }
      }
    } else {
      return false;
    }
  }

  /**
   * @returns result set from <b>already executed</b> @param stmt, or null with lots of diagnostic spew
   */
  public static ResultSet getResultSet(Statement stmt) {
    if (stmt != null) {
      try (Finally pop = dbg.Push("getResultSet()")) {
        return stmt.getResultSet();
      } catch (Exception e) {
        dbg.ERROR("attempting to get the ResultSet from the executed statement {1} got {0}", e, stmt.toString());
        dbg.Caught(e);
        return null;
      }
    } else {
      return null;
    }
  }

  public static void closeCon(Connection con) {
    if (con != null) {
      try (Finally pop = dbg.Push("closeCon()")) {
        con.close();
      } catch (Exception t) {
        dbg.WARNING("Exception closing connection.");
      }
    }
  }

  public static void closeStmt(Statement stmt) {
    if (stmt != null) {
      try (Finally pop = dbg.Push("Statement.close()")) {
        stmt.close();
      } catch (Exception t) {
        dbg.WARNING("closing statement gave {0}", t);
      }
    }
  }

  /**
   * ONLY use with resultsets that have no statement (like are returned by DatabaseMetadata functions)
   */
  public static void closeRS(ResultSet rs) {
    if (rs != null) {
      try (Finally pop = dbg.Push("ResultSet.close()")) {
        rs.close();
      } catch (Exception t) {
        dbg.WARNING("Exception closing result set: {0}", t);
      }
    }
  }

  public static Statement getStatement(ResultSet rs) {
    if (rs != null) {
      try (Finally pop = dbg.Push("ResultSet.getStatement()")) {
        return rs.getStatement();
      } catch (Exception t) {
        dbg.WARNING("Exception getting statement from resultset: {0}.", t);
      }
    }
    return null;
  }

  public static boolean isStringy(String image) {
    try {
      //noinspection ResultOfMethodCallIgnored
      Double.parseDouble(image);
      return false;//if we get here without an exception then the string is a number
    } catch (Exception any) {
      return true;
    }
  }
}

