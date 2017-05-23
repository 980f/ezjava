package pers.hal42.database;

import pers.hal42.lang.Bool;
import pers.hal42.lang.Monitor;
import pers.hal42.lang.ObjectX;
import pers.hal42.lang.StringX;
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
import java.util.Vector;

/*
 * functions for performing database queries and updates
* usage:  class MyDatabase extends DBMacros implements Database, ConfigurationManager, HardwareCfgMgr , BusinessCfgMgr
  * */

enum DBFunctionType {
  UPDATE, QUERY, NEXT;

  public static final String[] myTexts = {
    "Update: ",
    "Query:  ",
    "Next(): ",
  };

  public int numValues() {
    return 3;
  }

  protected final String[] getMyText() {
    return myTexts;
  }

}

public class DBMacros extends GenericDB {
  Counter queryCounter = new Counter(); // used to number queries for loggin purposes
  public static final String ALLTABLES = null;
  public static final int FAILED = -1; //where #of rows is expected
  protected static final Tracer dbv = new Tracer(DBMacros.class, "Validator");
  protected static final int ONLYCOLUMN = 1;
  // +++ eventually an enumeration?
  protected static final int DONE = 0;
  protected static final int ALREADY = 1;
  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(DBMacros.class);
  private static final Fstring timerStr = new Fstring(7, ' ');
  private static final int NOT_A_COLUMN = ObjectX.INVALIDINDEX;
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

  public DBMacros(DBConnInfo connInfo, String threadname) {
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

//  public TextList getTextListColumnFromRS(ResultSet rs) {
//    return getTextListColumnFromRS(rs, ONLYCOLUMN);
//  }
//
//  public TextList getTextListColumnFromRS(ResultSet rs, int col) {
//    TextList tl = new TextList(50, 50);
//    if(col < 1) {
//      col = 1;
//    }
//    while(next(rs)) {
//      String tmp = getStringFromRS(col, rs);
//      dbg.WARNING("adding "+tmp);
//      tl.add(tmp);
//    }
//    dbg.WARNING("returning: " + tl);
//    return tl;
//  }

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

  public String getStringFromQuery(QueryString queryStr, int field) {
    String result = null;
    String[] results = getStringsFromQuery(queryStr);
    if ((results != null) && (field < results.length)) {
      result = results[field].trim();
    }
    return result;
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
      try {
        dbg.VERBOSE("Calling ResultSet.findColumn() ...");
        col = rs.findColumn(cp.name());
      } catch (Exception e) {
        dbg.Caught(e);
      } finally {
        dbg.VERBOSE("Done calling ResultSet.findColumn().");
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

  public int getIntFromQuery(QueryString queryStr, ColumnProfile field) {
    return getIntFromQuery(queryStr, field.name());
  }

  public int getIntFromQuery(QueryString queryStr, int field) {
    return StringX.parseInt(StringX.TrivialDefault(getStringFromQuery(queryStr, field), "-1"));
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

  public double getDoubleFromQuery(QueryString queryStr, int field) {
    return StringX.parseDouble(getStringFromQuery(queryStr, field));
  }

  public double getDoubleFromRS(int column, ResultSet myrs) {
    return StringX.parseDouble(getStringFromRS(column, myrs));
  }

  public double getDoubleFromRS(String column, ResultSet myrs) {
    return StringX.parseDouble(getStringFromRS(column, myrs));
  }

  public Statement query(QueryString queryStr, boolean throwException) throws Exception {
    return query(queryStr, throwException, true /*canReattempt*/);
  }

  private Statement query(QueryString queryStr, boolean throwException, boolean canReattempt) throws SQLException {
//    checkDBthreadSelect();
    Statement stmt = null;
    SQLException mye = null;
    StopWatch swatch = new StopWatch();
    Connection mycon = null;
    boolean needsReattempt = false;
    long qn = queryCounter.incr();
    try {
      dbg.Push("query");
      mycon = getCon();
      if (mycon != null) {
        dbg.VERBOSE("Calling Connection.createStatement() ...");
        // +++ deal with other possible parameters to this statement ...
        stmt = mycon.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY /* +++ do we really want to say read only ? */);
        dbg.VERBOSE("Done calling Connection.createStatement().");
        String qs = String.valueOf(queryStr);
        dbg.VERBOSE("Calling Statement.executeQuery() ...");
//        preLogQuery(DBFunctionType.QUERY, queryStr, true, qn);
        if (stmt.executeQuery(qs) == null) { // +++ DOES THIS QUALIFY FOR GETTING A NEW CONNECTION?
          dbg.ERROR("queryStmt() generates a null statement!");
        }
        dbg.VERBOSE("Done calling Statement.executeQuery().");
      } else {
        dbg.ERROR("Cannot call queryStmt() unless connection exists!");
      }
    } catch (SQLException t) { // catch throwables (out of memory errors)? +++
      recycleConnection(t, mycon, queryStr);
      needsReattempt = true;
      if (throwException) {
        mye = t;
      } else {
        String toLog = "Exception performing query: " + swatch.millis() + " ms = " + queryStr + "; [recycled] " + t;
        dbg.Caught(t, toLog);
        log(toLog);
      }
    } finally {
      queryStats.add(swatch.millis());
      logQuery(DBFunctionType.QUERY, swatch.millis(), -1, true, qn);
      printWarnings(mycon);
      dbg.Exit();
      if (canReattempt && needsReattempt) { // +++ check the time it took last time?
        return query(queryStr, throwException, false /*canReattempt*/);
      } else {
        if (mye != null) {
          throw mye;
        }
        return stmt;
      }
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
        mycon.clearWarnings();
      } catch (Exception ex) {
        dbg.Caught(ex);
      }
    }
  }

  // +++ add this to the catch of next() ?
  private boolean recycleConnection(Exception t, Connection mycon, QueryString originalQuery) {
    closeCon(mycon); // pray that nobody else is using it!  probably is hosed, either way
    String msg = "DB.CONNCLOSED";
//    service.PANIC(msg);
    dbg.Caught(t, msg + ": " + originalQuery + "\n");
    return true;
  }

  public Statement query(QueryString queryStr) {
    Statement stmt = null;
    try {
      stmt = query(queryStr, false);
    } catch (Exception e) {
      dbg.Caught(e);
    } finally {
      return stmt;
    }
  }

  public int update(QueryString queryStr, boolean throwException) throws SQLException {
    return update(queryStr, throwException, /*canReattempt*/ true);
  }

  //todo: replace 'throwException' with an exception member on the QueryString and never throw from herein, instead return an error code.
  private int update(QueryString queryStr, boolean throwException, boolean canReattempt) throws SQLException {
//    checkDBthreadSelect();
    int retval = FAILED; // the error code
    SQLException tothrow = null;
    String qs = String.valueOf(queryStr);
    StopWatch swatch = new StopWatch();
    Connection mycon = null;
    boolean needsReattempt = false;
    long qn = queryCounter.incr();
    try {
      dbg.Push("update");
      Statement stmt = null;
      // do the query
      try {
        dbg.Push("update" + (throwException ? "(RAW)" : ""));
        mycon = getCon();
        if (mycon != null) {
          dbg.VERBOSE("Calling Connection.createStatement() ...");
          stmt = mycon.createStatement();
          dbg.VERBOSE("Done calling Connection.createStatement().");
          if (stmt != null) {
            // insert it into the table
            dbg.VERBOSE("Calling Connection.executeUpdate() ...");
//            preLogQuery(DBFunctionType.UPDATE, queryStr, true, qn);
            retval = stmt.executeUpdate(qs); // this is where sql exceptions will most likely occur
            dbg.VERBOSE("Done calling Connection.executeUpdate().");
          } else {
            dbg.VERBOSE("attempted to execute null statement");
          }
        } else {
          dbg.ERROR("Cannot call update() unless connection exists!");
        }
      } catch (SQLException e) {
        if (throwException) {
          tothrow = e;
        } else {
          dbg.Caught(e, "Exception performing update [recycled]: " + queryStr);
          if (mycon == null) {
            dbg.WARNING("mycon == null");
          }
          if (stmt == null) {
            dbg.WARNING("stmt == null");
          } else {
            try {
              SQLWarning warn = stmt.getWarnings();
              while (warn != null) {
                dbg.WARNING("warn: " + warn);
                warn = warn.getNextWarning();
              }
            } catch (Exception trash) {
              // who cares
            }
          }
          if (queryStr == null) {
            dbg.WARNING("queryStr == null");
          }
        }
        recycleConnection(e, mycon, queryStr); // +++ testing this !!!
        needsReattempt = true;
      } finally {
        try {
          dbg.VERBOSE("Calling Connection.getAutoCommit() ...");
          if ((mycon != null) && !mycon.getAutoCommit()) {
            mycon.commit();
          }
        } catch (Exception e) {
          dbg.Caught(e);
        } finally {
          dbg.VERBOSE("Done calling Connection.getAutoCommit().");
          closeStmt(stmt);
        }
        dbg.Exit();
      }
    } finally {
      try {
        updateStats.add(swatch.millis());
        logQuery(DBFunctionType.UPDATE, swatch.millis(), retval, true, qn);
      } catch (Exception e2) {
        dbg.Caught(e2);
      } finally {
        printWarnings(mycon);
        dbg.Exit();
        if (canReattempt && needsReattempt) { // +++ check the time it took last time?
          return update(queryStr, throwException, false /*canReattempt*/);
        } else {
          if (tothrow != null) {
            dbg.WARNING("Throwing " + tothrow);
            throw tothrow;
          } else {
            return retval;
          }
        }
      }
    }
  }

  public int update(QueryString queryStr) {
    int retval = FAILED; // changed from 0 recently.  should have?
    try {
      retval = update(queryStr, false);
    } catch (Exception e) {
      dbg.Caught(e);
    } finally {
      return retval;
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
  private ResultSet getTables() {
    ResultSet rs = null;
    try {
      String types[] = {
        "TABLE",
        "VIEW",
//        "SYSTEM TABLE",
//        "GLOBAL TEMPORARY",
//        "LOCAL TEMPORARY",
//        "ALIAS",
//        "SYNONYM",
      };
      dbg.VERBOSE("Calling DatabaseMetadata.getTables() ...");
      rs = getDatabaseMetadata().getTables(null, null, null, types);
    } catch (Exception e) {
      dbg.Caught(e);
    } finally {
      dbg.VERBOSE("Done calling DatabaseMetadata.getTables().");
    }
    return rs;
  }

  public TableInfoList getTableList() {
    // get the list of all tables in the new database
    TableInfoList tables = new TableInfoList();
    ResultSet rs = getTables();
    if (rs != null) {
      boolean cont = true;
      while (cont) {
        if (cont = next(rs)) {
          String name = getTableInfo(rs, "TABLE_NAME");
          String catalog = getTableInfo(rs, "TABLE_CAT");
          String schema = getTableInfo(rs, "TABLE_SCHEM");
          String type = getTableInfo(rs, "TABLE_TYPE");
          String remarks = getTableInfo(rs, "REMARKS");
          dbg.VERBOSE("Found table: CAT=" + catalog + ", SCHEM=" + schema + ", NAME=" + name + ", TYPE=" + type + ", REMARKS=" + remarks);
          if ((name != null) && (name.indexOf("SYS") != 0)) { // +++ We MUST make a rule to NOT start our table names with SYS!
            TableInfo ti = new TableInfo(catalog, schema, name, type, remarks);
            tables.add(ti);
          }
        }
      }
      closeStmt(getStatement(rs));
    }
    return tables.sortByName();
  }

  private String getTableInfo(ResultSet rs, String info) {
    String ret = "";
    try {
      dbg.Push("getTableInfo");
      dbg.VERBOSE("Calling ResultSet.getString() regarding: " + info + "...");
      ret = rs.getString(info);
    } catch (Exception e) {
      dbg.ERROR("Excepted trying to get field " + info + " from result set.");
    } finally {
      dbg.VERBOSE("Done calling ResultSet.getString().");
      dbg.Exit();
    }
    return ret;
  }

  /**
   * +++ Make this create TableProfile objects with the right names, then fill those objects with their data instead of passing String names around.
   */
  public DatabaseProfile profileDatabase(String databaseName, String tablename, boolean sort) {
    dbg.VERBOSE("Profiling database: " + databaseName + " ...");
    DatabaseProfile tables = new DatabaseProfile(databaseName);
    //noinspection StringEquality
    if (tablename != ALLTABLES) {
      TableProfile tp = profileTable(tablename);
      if (sort) {
        tp.sort();
      }
      tables.add(tp);
    } else { // Profile ALLTABLES
      TableInfoList tablelist = getTableList();
      for (int i = 0; i < tablelist.size(); i++) {
        TableProfile tp = profileTable(tablelist.itemAt(i).name());
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
    return profileTable(table.name());
  }

  private TableProfile profileTable(String tableName) {
    ResultSet cpmd = null;
    Vector<ColumnProfile> cols = new Vector<>(100);
    TableProfile tp = TableProfile.create(new TableInfo(tableName), null, new ColumnProfile[0]);
    try {
      dbg.VERBOSE("profiling table '" + tableName + "'");
      try {
        dbg.VERBOSE("Calling getDatabaseMetadata.getColumns() ...");
        cpmd = getDatabaseMetadata().getColumns(null, null, tableName.toLowerCase(), null);
      } finally {
        dbg.VERBOSE("Done calling getDatabaseMetadata.getColumns().");
      }
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
        try {
          cpmd.close(); // don't do this anywhere else, please
        } catch (Exception e) {
          dbg.Caught(e);
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

  public boolean tableExists(String tableName) {
    boolean ret = false;
    if (StringX.NonTrivial(tableName)) {
      TableInfoList tablelist = getTableList();
      for (int i = tablelist.size(); i-- > 0; ) {
        if (tablelist.itemAt(i).name().equalsIgnoreCase(tableName)) {
          ret = true;
          break;
        }
      }
    }
    return ret;
  }

  public boolean fieldExists(String tableName, String fieldName) {
    TableProfile tp = profileTable(tableName);
    // skip through and look for the fieldname
    return tp.fieldExists(fieldName);
  }

  public boolean primaryKeyExists(PrimaryKeyProfile primaryKey) {
    boolean ret = false;
    // +++ put this into the table profiler ???
    ResultSet rs = null;
    try {
      dbg.VERBOSE("Calling DatabaseMetadata.getPrimaryKeys() ...");
      String tablename = primaryKey.table.name().toLowerCase(); // MUST BE LOWER for PG
      rs = getDatabaseMetadata().getPrimaryKeys(null/*catalog*/, null/*schema*/, tablename);
    } catch (Exception e) {
      dbg.Caught(e);
    } finally {
      dbg.VERBOSE("Done calling DatabaseMetadata.getPrimaryKeys().");
    }
    if (rs != null) {
      try {
        boolean emptyresultset = true;
        while (next(rs)) {
          emptyresultset = false;
          String cname = getStringFromRS("PK_NAME", rs);
          if (primaryKey.name.equalsIgnoreCase(cname)) {
            ret = true;
            break;
          } else {
            dbg.ERROR("Primary key test: '" + cname + "' is not the same as '" + primaryKey.name + "'.");
          }
        }
        if (emptyresultset) {
          dbg.WARNING("Primary key test: resultset is empty!");
        }
      } catch (Exception e) {
        dbg.Caught(e);
      } finally {
        closeRS(rs);
      }
    } else {
      dbg.ERROR("ResultSet = NULL !!!");
    }
    String disp = primaryKey.table.name() + " primary key constraint " + primaryKey.name + " does " + (ret ? "" : "NOT ") + "exist.";
    if (ret) {
      dbg.VERBOSE(disp);
    } else {
      dbg.WARNING(disp);
    }
    return ret;
  }

  // due to a bug in the PG driver ...
  protected String extractFKJustName(String fkname) {
    if (StringX.NonTrivial(fkname)) {
      int loc = fkname.indexOf("\\000");
      if (loc > ObjectX.INVALIDINDEX) {
        String tmp = StringX.left(fkname, loc);
        dbg.VERBOSE("Extracted actual key name '" + tmp +
          "' from verbose PG key name '" + fkname + "'.");
        fkname = tmp;
      }
    }
    return fkname;
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
        String cname = extractFKJustName(getStringFromRS("FK_NAME", rs));
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
    boolean ret = false;
    try {
      dbg.Push("dropField");
      if (fieldExists(column.table().name(), column.name())) {
        boolean supports = false;
        try {
          dbg.VERBOSE("Running DatabaseMetadata.getIndexInfo() ...");
          supports = getDatabaseMetadata().supportsAlterTableWithDropColumn();
        } finally {
          dbg.VERBOSE("Done running DatabaseMetadata.getIndexInfo().");
        }
        if (!supports) {
          dbg.ERROR("dropField " + column.fullName() + ": was not able to run since the DBMS does not support it!");
          ret = true;
        } else {
          dbg.ERROR("dropField " + column.fullName() + ": returned " +
            update(QueryString.genDropField(column)));
          ret = !fieldExists(column.table().name(), column.name());
        }
      } else {
        dbg.ERROR("dropField " + column.fullName() + ": already dropped.");
        ret = true;
      }
    } catch (Exception e) {
      dbg.Caught(e);
    } finally {
      dbg.Exit();
      return ret;
    }
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

  /**
   * Returns true if the index was properly dropped
   */
  protected final boolean dropIndex(String indexname, String tablename) {
    int i = -1;
    try {
      dbg.Push("dropIndex");
      if (indexExists(indexname, tablename)) {
        i = update(QueryString.genDropIndex(indexname));
      } else {
        dbg.ERROR("dropIndex " + indexname + ": index doesn't exist; can't drop.");
      }
    } catch (Exception e) {
      dbg.Caught(e);
    } finally {
      dbg.ERROR("dropIndex " + indexname + ": returned " + i);
      dbg.Exit();
      return (i > -1) || !indexExists(indexname, tablename);
    }
  }

  /**
   * Returns true if the table was properly dropped
   */
  protected final boolean dropTable(String tablename) {
    boolean ret = false;
    try {
      dbg.Push("dropTable");
      if (tableExists(tablename)) {
        dbg.ERROR("dropTable" + tablename + " returned " +
          update(QueryString.genDropTable(tablename)));
        ret = !tableExists(tablename);
      } else {
        ret = true;
      }
    } catch (Exception e) {
      dbg.Caught(e);
    } finally {
      dbg.Exit();
      return ret;
    }
  }

  // +++ use dbmd's getMaxColumnNameLength to see if the name is too long
  // +++ combine this and prev function
  protected final boolean addField(ColumnProfile column) {
    boolean ret = false;
    try {
      dbg.Push("addField");
      if (!fieldExists(column.table().name(), column.name())) {
        dbg.ERROR("addField" + column.fullName() + " returned " + update(/* +++ use: db.generateColumnAdd(tp, cp) (or something similar) instead! */
          QueryString.genAddField(column)));
        ret = fieldExists(column.table().name(), column.name());
      } else {
        ret = true;
      }
    } catch (Exception e) {
      dbg.Caught(e);
    } finally {
      dbg.Exit();
      return ret;
    }
  }

  protected final boolean changeFieldType(ColumnProfile from, ColumnProfile to) {
    try {
      dbg.Push("changeFieldType");
//      dbg.ERROR("changeFieldType " + to.fullName() + " returned " + update(QueryString.genChangeFieldType(to)));
      dbg.ERROR("changeFieldType is not yet supported.  Write code in the content validator to handle this!");
      return false;//fieldExists(to.table().name(), to.name()); // +++ instead, need to do the things you do to check that a column is correct, not just check to see if it is added!
    } catch (Exception e) {
      dbg.Caught(e);
      return false;
    } finally {
      dbg.Exit();
    }
  }

  protected final boolean changeFieldNullable(ColumnProfile to) {
    boolean ret = false;
    try {
      dbg.Push("changeFieldNullable");
      dbg.ERROR("changeFieldNullable " + to.fullName() + " returned " + update(QueryString.genChangeFieldNullable(to)));
      TableProfile afterTable = profileTable(to.table().name());
      ColumnProfile aftercolumn = afterTable.column(to.name());
      ret = to.sameNullableAs(aftercolumn);
    } catch (Exception e) {
      dbg.Caught(e);
    } finally {
      dbg.Exit();
      return ret;
    }
  }

  protected final boolean changeFieldDefault(ColumnProfile to) {
    boolean ret = false;
    try {
      dbg.Push("changeFieldDefault");
      dbg.ERROR("changeFieldDefault " + to.fullName() + " returned " + update(QueryString.genChangeFieldDefault(to)));
      TableProfile afterTable = profileTable(to.table().name());
      ColumnProfile aftercolumn = afterTable.column(to.name());
      ret = to.sameDefaultAs(aftercolumn);
    } catch (Exception e) {
      dbg.Caught(e);
    } finally {
      dbg.Exit();
      return ret;
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

  protected final int validateAddIndex(IndexProfile index) {
    String functionName = "validateAddIndex";
    int success = FAILED;

    String indexName = index.name;
    String tableName = index.table.name();
    String fieldExpression = index.columnNamesCommad();

    try {
      dbv.Push(functionName);//#gc
      dbv.mark("Add Index " + indexName + " for " + tableName + ":" + fieldExpression);
      if (!indexExists(index)) {
        QueryString qs = QueryString.genCreateIndex(index);
        if (update(qs) != -1) {
          success = DONE;
          dbv.ERROR(functionName + ": SUCCEEDED!");
        } else {
          dbv.ERROR(functionName + ": ERROR [" + qs + "]!");
        }
      } else {
        success = ALREADY;
        dbv.ERROR("Index " + indexName + " already exists!");
      }
    } catch (Exception e) {
      dbv.Caught(e);
    } finally {
      if (success == FAILED) {
        dbv.ERROR(functionName + ": FAILED!");
      }
      dbv.mark("");
      dbv.Exit();//#gc
      return success;
    }
  }

  protected final int validateAddField(ColumnProfile column) {
    String functionName = "validateAddField(fromProfile)";
    int success = FAILED;
    try {
      dbv.Push(functionName);//#gc
      dbv.mark("Add field " + column.fullName());
      if (!fieldExists(column.table().name(), column.name())) {
        boolean did = addField(column);
        success = (did ? DONE : FAILED);
        dbv.ERROR((did ? "Added" : "!! COULD NOT ADD") + " field " + column.fullName());
      } else {
        success = ALREADY;
        dbv.ERROR("Field " + column.fullName() + " already added.");
      }
    } catch (Exception e) {
      dbv.Caught(e);
    } finally {
      if (success == FAILED) {
        dbv.ERROR(functionName + ":" + " FAILED!");
      }
      dbv.mark("");
      dbv.Exit();//#gc
      return success;
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

  // +++ generalize internal parts between validate functions!
  protected final int validateAddPrimaryKey(PrimaryKeyProfile primaryKey) {
    String functionName = "validateAddPrimaryKey";
    int success = FAILED;
    try {
      dbv.Push(functionName);//#gc
      String blurb = "Primary Key " + primaryKey.name + " for " + primaryKey.table.name() + "." + primaryKey.field.name();
      dbv.mark("Add " + blurb);
      if (!primaryKeyExists(primaryKey)) {
        QueryString qs = QueryString.genAddPrimaryKeyConstraint(primaryKey);
        if (update(qs) != -1) {
          success = DONE;
          dbv.ERROR(functionName + ": SUCCEEDED!");
        }
      } else {
        success = ALREADY;
        dbv.ERROR(blurb + " already added.");
      }
    } catch (Exception e) {
      dbv.Caught(e);
    } finally {
      if (success == FAILED) {
        dbv.ERROR(functionName + ": FAILED!");
      }
      dbv.mark("");
      dbv.Exit();//#gc
      return success;
    }
  }

  // +++ generalize internal parts and move to DBMacros!
  protected final int validateAddForeignKey(ForeignKeyProfile foreignKey) {
    String functionName = "validateAddForeignKey";
    int success = FAILED;
    try {
      dbv.Push(functionName);//#gc
      String blurb = "Foreign Key " + foreignKey.name + " for " + foreignKey.table.name() + "." + foreignKey.field.name() + " against " + foreignKey.referenceTable.name();
      dbv.mark("Add " + blurb);
      if (!foreignKeyExists(foreignKey)) {
        QueryString qs = QueryString.genAddForeignKeyConstraint(foreignKey);
        if (update(qs) != -1) {
          success = DONE;
          dbv.ERROR(functionName + ": SUCCEEDED!");
        }
      } else {
        success = ALREADY;
        dbv.ERROR(blurb + " already added.");
      }
    } catch (Exception e) {
      dbv.Caught(e);
    } finally {
      if (success == FAILED) {
        dbv.ERROR(functionName + ": FAILED!");
      }
      dbv.mark("");
      dbv.Exit();//#gc
      return success;
    }
  }

  protected final int validateAddTable(TableProfile table) {
    String functionName = "validateAddTable";
    int success = FAILED;
    try {
      dbv.Push(functionName);//#gc
      dbv.mark("Add table " + table.name());
      if (!tableExists(table.name())) {
        boolean did = createTable(table);
        success = (did ? DONE : FAILED);
        dbv.ERROR((did ? "Added" : "!! COULD NOT ADD") + " table " + table.name());
      } else {
        success = ALREADY;
        dbv.ERROR("Table " + table.name() + " already added.");
      }
    } catch (Exception e) {
      dbv.Caught(e);
    } finally {
      if (success == FAILED) {
        dbv.ERROR(functionName + ":" + " FAILED!");
      }
      dbv.mark("");
      dbv.Exit();//#gc
      return success;
    }
  }

  // +++ use dbmd's getMaxColumnNameLength to see if the name is too long
  // +++ generalize and move into DBMacros!
  protected final boolean dropTableConstraint(String tablename, String constraintname) {
    String functionName = "dropTableConstraint";
    int success = FAILED;
    String toDrop = "drop constraint " + tablename + "." + constraintname;
    try {
      dbv.Push(functionName);//#gc
      dbv.mark(toDrop);
      TableProfile tempprof = TableProfile.create(new TableInfo(tablename), null, null);
      success = (tableExists(tablename)) ?
        update(QueryString.genDropConstraint(tempprof, new Constraint(constraintname, tempprof, null))) :
        ALREADY;
    } catch (Exception e) {
      // muffle
      //dbv.Caught(e);
    } finally {
      dbv.ERROR(functionName + ": '" + toDrop +
        ((success == ALREADY) ?
          " Can't perform since table doesn't exist." :
          ((success == DONE) ?
            "' Succeeded!" :
            "' FAILED!  Constraint probably didn't exist.")));
      dbv.mark("");
      dbv.Exit();//#gc
      return (success != FAILED);
    }
  }

  protected final boolean renameField(String table, String oldname, String newname) {
    int count = 0;
    if (fieldExists(table, oldname)) {
      count = update(QueryString.genRenameColumn(table, oldname, newname));
    }
    return (count == 0);
  }

  // +++ use dbmd's getMaxTableNameLength to see if the name is too long
  protected final boolean createTable(TableProfile tp) {
    boolean ret = false;
    try {
      dbg.Push("createTable");
      if (!tableExists(tp.name())) {
        dbg.ERROR("createTable " + tp.name() + " returned " + update(QueryString.genCreateTable(tp)));
        ret = tableExists(tp.name());
      } else {
        ret = true;
      }
    } catch (Exception e) {
      dbg.Caught(e);
    } finally {
      dbg.Exit();
      return ret;
    }
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
            ColumnTypes dbt = ColumnTypes.valueOf(rsmd.getColumnTypeName(col));
            if (dbt == ColumnTypes.BOOL) {
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
    String toLog = "exception attempting to log the query";
    try (AutoCloseable free = timerStrMon.getMonitor()) {
      // create a pool of DBFunctionTypes and Fstrings that you can reuse [no mutexing == faster].
      toLog = "'can'this figure out what javac doesn't like about the following complex message, ie replace with Message.Format ";
//        qt.toString()
//        + (qt==DBFunctionType.NEXT )? "" : " END   " + qn + " "
//        +  timerStr.righted(String.valueOf(millis)) + " ms " +
//          (qt==DBFunctionType.UPDATE) ? " returned " + retval : "";
      if (regularLog) {
        dbg.WARNING(toLog);
      }
      log(toLog);
    } catch (Exception e) {
      dbg.Caught(e);
    }
  }

  // VALIDATOR STUFF

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

  // VALIDATOR STUFF

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

  public static String getStringFromRS(int column, ResultSet myrs) {
    return getStringFromRS(column, null, myrs);
  }

  private static String getStringFromRS(int column, String fieldName, ResultSet myrs) {
    String ret = null;
    try {
      if (myrs != null) {//valid non empty resultset
        if (column < 1) {//code that tells us to use the name field
          try {
            dbg.VERBOSE("Calling ResultSet.findColumn() ...");
            column = myrs.findColumn(fieldName);//excepts if results set is empty!
          } catch (SQLException ex) {
            dbg.VERBOSE("Coding error: \n" + ErrorLogStream.whereAmI());
            dbg.VERBOSE(ex.getMessage() + " - see next line for args...");
          } finally {
            dbg.VERBOSE("Done calling ResultSet.findColumn().");
          }
        }
        if (column < 1) {//happens when field is present but result set is empty.
          dbg.ERROR("getStringFromRS: column [" + fieldName + "] (not included in query?): ");
        } else {
          try {
            dbg.VERBOSE("Calling ResultSet.getString() ...");
            ret = myrs.getString(column);
            //} catch (Exception ex) {
          } finally {
            dbg.VERBOSE("Done calling ResultSet.getString().");
          }
        }
      }
    } catch (Exception t) {
      dbg.ERROR("getStringFromRS: Exception getting column [" + column + "/" + fieldName + "] (not included in query?): ");
      dbg.Caught(t);
    }
    // --------------- NEXT LINE COULD BE CAUSING PROBLEMS,
    // but try to fix the problems elsewhere so that we can preserve empty strings here.
    // return StringX.OnTrivial(StringX.TrivialDefault(ret, "").trim(), " "); // Why are we doing this ?!?!?!?  I think because leaving it out makes empty (raised) boxes in the html tables, but we should fix those in the html tables, and not here.
    return StringX.TrivialDefault(ret, "").trim();
  }

  public static int getIntFromRS(int column, ResultSet myrs) {
    return StringX.parseInt("0" + getStringFromRS(column, myrs));//--- parseInt can deal with null and non-decimal returns
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
    String boolstr = getStringFromRS(column, myrs);
    boolean ret = Bool.For(boolstr);
    dbg.VERBOSE("Returning " + ret + " for string " + boolstr);
    return ret;
  }

  public static boolean getBooleanFromRS(ColumnProfile column, ResultSet myrs) {
    return getBooleanFromRS(column.name(), myrs);
  }

  /**
   * TODO:
   * +++ Improve the pagination of records [only for search screen, drawers listing, etc.]:
   * boolean isBeforeFirst() throws SQLException;
   * boolean isAfterLast() throws SQLException;
   * boolean isFirst() throws SQLException;
   * boolean isLast() throws SQLException;
   * void beforeFirst() throws SQLException;
   * void afterLast() throws SQLException;
   * boolean first() throws SQLException;
   * boolean last() throws SQLException;
   * int getRow() throws SQLException;
   * boolean absolute( int row ) throws SQLException;
   * boolean relative( int rows ) throws SQLException;
   * boolean previous() throws SQLException;
   */
  public static boolean next(ResultSet rs) {
    return next(rs, null);
  }

  // a timer is passed in so that it can be used,
  // in case the caller wants to know how long it took.
  // if the timer is null, we create one locally,
  // as we use that timer to add times to our accumulator for service reporting.
  public static boolean next(ResultSet rs, StopWatch swatch) {
    if (swatch == null) {
      swatch = new StopWatch(false);
    }
    swatch.Start(); // to be sure to start either passed-in or locally-created ones
    boolean ret = false;
    try {
      if (rs != null) {
        dbg.VERBOSE("Calling ResultSet.next() ...");
        ret = rs.next();
      }
    } catch (Exception e) {
      dbg.ERROR("next() excepted attempting to get the next row in the ResultSet ... ");
      dbg.Caught(e);
    } finally {
      if (rs != null) {
        dbg.VERBOSE("Done calling ResultSet.next().");
      }
      long dur = swatch.Stop();
      nextStats.add(dur);
      if (dur > 0) {
        logQuery(DBFunctionType.NEXT, dur, -1, false, -1);
      }
      return ret;
    }
  }

  public static ResultSet getResultSet(Statement stmt) {
    ResultSet ret = null;
    try {
      if (stmt != null) {
        dbg.VERBOSE("Calling Statement.getResultSet() ...");
        ret = stmt.getResultSet();
      }
    } catch (Exception e) {
      dbg.ERROR("getRS excepted attempting to get the ResultSet from the executed statement");
      dbg.Caught(e);
    } finally {
      if (stmt != null) {
        dbg.VERBOSE("Done calling Statement.getResultSet().");
      }
      return ret;
    }
  }

  public static void closeCon(Connection con) {
    if (con != null) {
      try {
        dbg.VERBOSE("Calling Connection.close() ...");
        con.close();
      } catch (Exception t) {
        dbg.WARNING("Exception closing connection.");
      } finally {
        dbg.VERBOSE("Done calling Connection.close().");
      }
    }
  }

  public static void closeStmt(Statement stmt) {
    if (stmt != null) {
      try {
        dbg.VERBOSE("Calling Statement.close() ...");
        stmt.close();
      } catch (Exception t) {
        dbg.WARNING("Exception closing statement.");
      } finally {
        dbg.VERBOSE("Done calling Statement.close().");
      }
    }
  }

  /**
   * ONLY use with resultsets that have no statement (like are returned by DatabaseMetadata functions)
   */
  public static void closeRS(ResultSet rs) {
    if (rs != null) {
      try {
        dbg.VERBOSE("Calling ResultSet.close() ...");
        rs.close();
      } catch (Exception t) {
        dbg.WARNING("Exception closing result set.");
      } finally {
        dbg.VERBOSE("Done calling ResultSet.close().");
      }
    }
  }

  public static Statement getStatement(ResultSet rs) {
    try {
      if (rs != null) {
        dbg.VERBOSE("Calling ResultSet.getStatement() ...");
        return rs.getStatement();
      }
    } catch (Exception t) {
      dbg.WARNING("Exception getting statement from resultset.");
    } finally {
      if (rs != null) {
        dbg.VERBOSE("Done calling ResultSet.getStatement().");
      }
    }
    return null;
  }

}

class NamedStatementList extends Vector<NamedStatement> {
  private ErrorLogStream dbg = null;

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

class NamedStatement {
  public Statement stmt = null;
  public String name = "uninited"; // name is really the full query

  public NamedStatement(Statement stmt, String name) {
    this.stmt = stmt;
    this.name = Thread.currentThread().getName() + ";" + name;
  }
}

