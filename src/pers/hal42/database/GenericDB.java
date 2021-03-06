package pers.hal42.database;

import com.mysql.jdbc.exceptions.jdbc4.MySQLNonTransientConnectionException;
import pers.hal42.lang.Finally;
import pers.hal42.lang.Monitor;
import pers.hal42.lang.ReflectX;
import pers.hal42.lang.StringX;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.thread.Counter;

import java.sql.*;

import static java.text.MessageFormat.format;
import static pers.hal42.database.QueryString.Clause;
import static pers.hal42.lang.Index.BadIndex;

public class GenericDB {
  protected Modeler modeler = new Modeler();

  /**
   * provider for DML.
   * Default class works for simple situations
   */
  static class Modeler {

    public QueryString genDropIndex(String indexname) {
      return null;
    }

    public QueryString genDropTable(String tablename) {
      return Clause(format("DROP TABLE IF EXISTS {0}", tablename));
    }

    public QueryString genAddField(ColumnProfile column) {
      return null;
    }

    public QueryString genChangeFieldNullable(ColumnProfile to) {
      return null;
    }

    public QueryString genChangeFieldDefault(ColumnProfile to) {
      return null;
    }

    public QueryString genCreateIndex(IndexProfile index) {
      return null;
    }

    public QueryString genAddPrimaryKeyConstraint(PrimaryKeyProfile primaryKey) {
      return null;
    }

    public QueryString genAddForeignKeyConstraint(ForeignKeyProfile foreignKey) {
      return null;
    }

    public QueryString genDropConstraint(TableProfile tempprof, Constraint constraint) {
      return null;
    }

    public QueryString genRenameColumn(String table, String oldname, String newname) {
      return null;
    }

    public QueryString genCreateTable(TableProfile tp) {
      return null;
    }

    public String extractFKJustName(String fkname) {    //!PG version
      if (StringX.NonTrivial(fkname)) {
        int loc = fkname.indexOf("\\000");
        if (loc != BadIndex) {
          String tmp = StringX.left(fkname, loc);
          dbg.VERBOSE("Extracted actual key name {0} from verbose PG key name {1}", tmp, fkname);
          fkname = tmp;
        }
      }
      return fkname;
    }
  }

  private final Monitor connMonitor;
  /**
   * whether conn is not null and is likely usable. clear this if the non-null connection gave you trouble.
   */
  public boolean connectOk = false;
  /**
   * replaced pool with a regenerator
   */
  protected DBConn dbConnector;

  public MysqlConnectionInfo getConnInfo() {
    return connInfo;
  }

  //  protected boolean validated() {
//    return (cpool != null) && cpool.validated;
//  }
//
//  protected void validated(boolean value) {
//    if(cpool != null) {
//      cpool.validated = value;
//    }
//  }
//
//  public static final EasyProperties getPoolProfile() {
//    return list.getProfile();
//  }
//
//  private static final ConnectionPoolList list = new ConnectionPoolList(); // list of ConnectionPools
//  public static final int connectionCount() {
//    int cnxns = list.connectionCount();
//    dbg.ERROR("cnxns returned " + cnxns);
//    return cnxns;
//  }
//  public static final int connectionsUsed() {
//    int cnxns = list.connectionsUsed();
//    dbg.ERROR("cnxns returned " + cnxns);
//    return cnxns;
//  }
//
//  protected static final void startCaretakers() {
//    list.startCaretakers();
//  }
//private ConnectionPool cpool;
  private MysqlConnectionInfo connInfo;
  /**
   * the live connection, see connectOk
   */
  private Connection conn;
  private String myThreadName = "NOTSETYET";
  /**
   * last fetched metadata
   */
  private DatabaseMetaData dbmd; // saved for debug
  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(GenericDB.class);
  private static final Monitor genericDBclassMonitor = new Monitor(GenericDB.class.getName());
  private static final Counter metaDataCounter = new Counter();

  public GenericDB(MysqlConnectionInfo connInfo, String threadname) {
    connMonitor = new Monitor(GenericDB.class.getName() + ".MetaData." + metaDataCounter.incr());
    try (Monitor free = genericDBclassMonitor.getMonitor()) {
      this.myThreadName = threadname;
      this.connInfo = connInfo;
      dbg.VERBOSE("Preloading driver class {0}", connInfo.drivername);
      if (!ReflectX.preloadClass(connInfo.drivername)) {
        dbg.ERROR("Preloading driver class {0} failed", connInfo.drivername);
      }
//      cpool = list.get(connInfo);
      getConnection();
    }
  }

  public void finalize() {
    releaseConn();
  }

  public void releaseConn() {
    try {
      if (conn != null) {
        dbg.VERBOSE("releasing connection for thread {0}", myThreadName);
//        list.get(connInfo).checkIn(conn);
        try {
          conn.close();
        } catch (SQLException e) {
          dbg.VERBOSE("Closing discarded connection");
        } finally {
          connectOk = false;//forget we ahve one
          conn = null;
        }
      }
    } catch (Exception e) {
      dbg.Caught(e);
    }
  }

  public final DatabaseMetaData getDatabaseMetadata() {
    try (Finally pop = dbg.Push("getDatabaseMetadata")) {
      Connection mycon = getConnection();
      if (mycon != null) {
        try (Monitor freer = connMonitor.getMonitor()) {
          dbg.VERBOSE("Calling Connection.getMetaData() ...");
          dbmd = mycon.getMetaData();
          return dbmd;
        } catch (SQLException e) {
          dbg.Caught(e);
          return null;
        }
      }
      return dbmd;//can return a stale instance.
    }
  }

  public boolean haveConnection() {
    if (conn == null) {
      getConnection();
    } else {
      try {
        if (conn.isClosed() || !conn.isValid(1)) {//getting 'connection is closed' apparently due to autoclose on timeout.
          connectOk = false;
          getConnection();
        }
      } catch (SQLException e) {
        releaseConn();//on any error kick back hard
        getConnection();
      }
    }
    return connectOk;
  }

  public final Connection getConnection() {
    try (Monitor freer = connMonitor.getMonitor()) {//mutex needed when we restore connection pooling
      if (!connectOk && conn != null) {
        releaseConn();//nulls conn.
      }
      if (dbConnector == null) {
        dbConnector = new DBConn();
      }
      if (conn == null) {
        dbg.WARNING("Getting new connection for thread <{0}>", myThreadName);
//        conn = cpool.checkOut();
        conn = dbConnector.makeConnection(connInfo);
      }
    }
    connectOk = conn != null;
    if (connectOk) {
      try (Statement statement = makeStatement()) {//todo:1 abstract to a list of statements to execute upon making a connection. Needs DbFlavor concept to be implemented, which would return an interator.
        statement.execute("SET GLOBAL max_allowed_packet=1024*1024*50");
        statement.execute("SET SESSION group_concat_max_len = 1024*1024*50");
      } catch (SQLException e) {
        dbg.Caught(e);
      }
    }
    return conn;
  }

  /** @returns a usable statement else throws an exception */
  public Statement makeStatement() throws SQLException {
    if (haveConnection()) {
      try {
        return conn.createStatement();
      } catch (SQLException e1) {
        if (e1 instanceof MySQLNonTransientConnectionException) {
          MySQLNonTransientConnectionException throwables = (MySQLNonTransientConnectionException) e1; //expose details for the debugger
          connectOk = false;
        }
        throw e1;
      }
    } else {
      throw new SQLException("No connection");
    }
  }

  public PreparedStatement makePreparedStatement(String sql) throws SQLException {
    if (haveConnection()) {
      return conn.prepareStatement(sql);
    } else {
      return null;
    }
  }

  public void keepAlive(double seconds) {
    if (connInfo.intervalsecs > 0 && seconds >= connInfo.intervalsecs) {
      try (Statement st = makeStatement()) {
        st.execute(connInfo.keepaliveSQL);
      } catch (SQLException e) {
        dbg.Caught(e);//may spew exceptions if configuration is bad.
      }
    }
  }

  public boolean wakeup() {
    return haveConnection();//devolved into this.
  }

  /** @returns whether we asked for the connection to not be read-only */
  public boolean isWritable() {
    return !dbConnector.connInfo.readOnly;
  }
}

