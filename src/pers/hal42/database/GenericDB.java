package pers.hal42.database;


import pers.hal42.lang.Monitor;
import pers.hal42.lang.ReflectX;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.thread.Counter;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;

public class GenericDB {

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
  private Connection conn;
  private String myThreadName = "NOTSETYET";
  private final Monitor connMonitor;

  /** last fetched metadata */
  private DatabaseMetaData dbmd; // saved for debug

  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(GenericDB.class);
  private static final Monitor genericDBclassMonitor = new Monitor(GenericDB.class.getName());
  private static final Counter metaDataCounter = new Counter();

  public GenericDB(DBConnInfo connInfo, String threadname) {
    connMonitor = new Monitor(GenericDB.class.getName() + ".MetaData." + metaDataCounter.incr());
    try (AutoCloseable free = genericDBclassMonitor.getMonitor()) {
      this.myThreadName = threadname;
      this.connInfo = connInfo;
      dbg.VERBOSE("Preloading driver class {0}",connInfo.drivername);
      if(!ReflectX.preloadClass(connInfo.drivername)){
        dbg.ERROR("Preloading driver class {0} failed",connInfo.drivername);
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
//        list.get(connInfo).checkIn(conn);
        conn = null;
        dbg.WARNING("releasing connection for thread \"" + myThreadName + "\" !");
      }
    } catch (Exception e) {
      dbg.Caught(e);
    }
  }

  public final DatabaseMetaData getDatabaseMetadata() {
    try (AutoCloseable pop = dbg.Push("getDatabaseMetadata")) {
      Connection mycon = getConnection();
      if (mycon != null) {
        try (AutoCloseable monitor = connMonitor.getMonitor()) {
          dbg.VERBOSE("Calling Connection.getMetaData() ...");
          dbmd = mycon.getMetaData();
          return dbmd;
        }
      }
      return dbmd;//return a stale instance.
    } catch (Exception t) {
      dbg.Caught(t);
      return null;
    }
  }

  public final Connection getConnection() {

    try (AutoCloseable monitor = connMonitor.getMonitor()) {//mutex needed when we restore connection pooling
      if(dbConnector==null) {
        dbConnector = new DBConn();
      }
      if (conn == null) {
        dbg.WARNING("conn is null, so getting new connection for thread \"" + myThreadName + "\" !");
//        conn = cpool.checkOut();
        conn = dbConnector.makeConnection(connInfo);
      }
    } catch (Exception ex) {
      dbg.Caught(ex);
    } finally {
      return conn;
    }
  }

}

