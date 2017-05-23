package pers.hal42.database;

import pers.hal42.logging.ErrorLogStream;
import pers.hal42.transport.Storable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConn {
  @Storable.Stored
  public DBConnInfo connInfo;
  public Connection dbConnection;
  public static ErrorLogStream dbg = ErrorLogStream.getForClass(DBConn.class);

  public Connection makeConnection(DBConnInfo connInfo) {
    if (dbConnection != null) {
      try {
        dbConnection.close();//don't trust finalizer
      } catch (SQLException e) {
        dbg.Caught(e, "Closing an abandoned database connection");
      }
      dbConnection = null;
    }
    try (AutoCloseable pop=dbg.Push("Connect to Database")){
      this.connInfo = connInfo;
      Properties lProp = new java.util.Properties();
      lProp.put("user", connInfo.connUser);
      lProp.put("password", connInfo.connPass);
      dbg.VERBOSE("Attempting connection to: '" + connInfo.connDatasource + "'.");
      dbConnection = DriverManager.getConnection(connInfo.connDatasource, lProp);
      dbConnection.setAutoCommit(connInfo.autoCommit);
    } catch (Exception e) {
      dbConnection = null;
//      boolean wasSql = (e.getClass() == SQLException.class);
      dbg.ERROR("Couldn't connect to datasource '" + connInfo.connDatasource + "' via user: '" + connInfo.connUser + "'\n");
      dbg.Caught(e);
    }
    return dbConnection;
  }
}
