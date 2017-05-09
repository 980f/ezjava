
package pers.hal42.database;

import pers.hal42.logging.ErrorLogStream;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConn {
  public static ErrorLogStream dbg=ErrorLogStream.getForClass(DBConn.class);

  DBConnInfo connInfo;
  Connection dbConnection;
  public Connection makeConnection(DBConnInfo connInfo){
    if(dbConnection!=null){
      try {
        dbConnection.close();//don't trust finalizer
      } catch (SQLException e) {
        dbg.Caught("Closing an abandoned database connection",e);
      }
      dbConnection=null;
    }
    try {
      dbg.Enter("checkOut()");
      this.connInfo = connInfo;
      Properties lProp = new java.util.Properties();
      lProp.put("user", connInfo.connUser);
      lProp.put("password", connInfo.connPass);
      dbg.VERBOSE("Attempting connection to: '" + connInfo.connDatasource + "'.");
      dbConnection = DriverManager.getConnection(connInfo.connDatasource, lProp);
      dbConnection.setAutoCommit(true);
      //todo: restore some ConnectionPool methods:
//      logAutocommit(dbConnection);
//      setEnableSeqScanOff(dbConnection);
    } catch (Exception e) {
      dbConnection = null;
//      boolean wasSql = (e.getClass() == SQLException.class);
      dbg.ERROR("Couldn't connect to datasource '" + connInfo.connDatasource + "' via user: '" + connInfo.connUser + "'\n");
      dbg.Caught(e);
    } finally {
      dbg.VERBOSE("Done attempting connection.");
      dbg.Exit();
    }
    return dbConnection;
  }
}
