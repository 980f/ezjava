package pers.hal42.database;


import pers.hal42.lang.Monitor;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.thread.Counter;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

public class GenericDB {

  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(GenericDB.class);

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
  /** replaced pool with a regenerator */
  DBConn dbConnector;
  private static final Monitor genericDBclassMonitor = new Monitor(GenericDB.class.getName());

  private static final Counter metaDataCounter = new Counter();

  private DBConnInfo connInfo;
  private Connection conn;
  private String myThreadName = "NOTSETYET";

  // +++ eventually package the connDataSource, username, and password into a single object
  public GenericDB(DBConnInfo connInfo, String threadname) {
    genericDBclassMonitor.getMonitor();
    try {
      this.myThreadName = threadname;
      this.connInfo = connInfo;
      connMonitor = new Monitor(GenericDB.class.getName()+".MetaData."+metaDataCounter.incr());
//      cpool = list.get(connInfo);
      getCon();
    } finally {
      genericDBclassMonitor.freeMonitor();
    }
  }

  public void finalize() {
    releaseConn();
  }

  public void releaseConn() {
    try {
      if(conn != null) {
//        list.get(connInfo).checkIn(conn);
        conn = null;
        dbg.WARNING("releasing connection for thread \"" + myThreadName + "\" !");
      }
    } catch (Exception e) {
      dbg.Caught(e);
    }
  }

  private Monitor connMonitor = null;
  private DatabaseMetaData dbmd; // the metadata for the database
  public final DatabaseMetaData getDatabaseMetadata() {
    try {
      dbg.Enter("getDatabaseMetadata");
      Connection mycon = getCon();
      if(mycon != null) {
        connMonitor.getMonitor();
        try {
          dbg.VERBOSE("Calling Connection.getMetaData() ...");
          dbmd = mycon.getMetaData();
        } finally {
          connMonitor.freeMonitor();
          dbg.VERBOSE("Done calling Connection.getMetaData().");
        }
      }
    } catch (Exception t) {
      dbg.Caught(t);
      dbmd = null;
    } finally {
      dbg.Exit();
      return dbmd;
    }
  }

  // there is a mutex in this class, but it should be very fast.
  // +++ @@@ %%% should we put this in a loop so that it doesn't continue UNTIL it gets a connection?
  public final Connection getCon() {
    connMonitor.getMonitor();
    try {
      if(conn == null) {
        dbg.WARNING("conn is null, so getting new connection for thread \"" + myThreadName + "\" !");
//        conn = cpool.checkOut();
        conn = dbConnector.makeConnection(connInfo);
      }
    } catch (Exception ex) {
      dbg.Caught(ex);
    } finally {
      connMonitor.freeMonitor();
      return conn;
    }
  }

}

