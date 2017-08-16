package pers.hal42.database;

import com.mysql.jdbc.exceptions.jdbc4.MySQLNonTransientConnectionException;
import pers.hal42.lang.Finally;
import pers.hal42.lang.Monitor;
import pers.hal42.lang.ReflectX;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.thread.Counter;

import java.sql.*;

public class GenericDB {

  private final Monitor connMonitor;
  /**
   * whether conn is not null and is likely usable. clear this if the non-null connection gave you trouble.
   */
  public boolean connectOk = false;
  /**
   * replaced pool with a regenerator
   */
  protected DBConn dbConnector;
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
  private DBConnInfo connInfo;
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

  public GenericDB(DBConnInfo connInfo, String threadname) {
    connMonitor = new Monitor(GenericDB.class.getName() + ".MetaData." + metaDataCounter.incr());
    try (AutoCloseable free = genericDBclassMonitor.getMonitor()) {
      this.myThreadName = threadname;
      this.connInfo = connInfo;
      dbg.VERBOSE("Preloading driver class {0}", connInfo.drivername);
      if (!ReflectX.preloadClass(connInfo.drivername)) {
        dbg.ERROR("Preloading driver class {0} failed", connInfo.drivername);
      }
//      cpool = list.get(connInfo);
      getConnection();
    } catch (Exception e) {
      // can't happen
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
        dbg.WARNING("Getting new connection for thread \"" + myThreadName + "\" !");
//        conn = cpool.checkOut();
        conn = dbConnector.makeConnection(connInfo);
      }
    }
    connectOk = conn != null;
    return conn;
  }

  public Statement makeStatement() throws SQLException {
    if (haveConnection()) {
      try {
        return conn.createStatement();
      } catch (SQLException e1) {
        if (e1 instanceof MySQLNonTransientConnectionException) {
          MySQLNonTransientConnectionException throwables = (MySQLNonTransientConnectionException) e1;
          connectOk = false;
        }
        throw e1;
      }
    } else {
      return null;
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

