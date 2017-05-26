package pers.hal42.database;

import com.fedfis.RefconTableManager;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.transport.Storable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConn {
  @Storable.Stored
  public DBConnInfo connInfo;
  /** most recently created connection */
  public Connection dbConnection;
  public static ErrorLogStream dbg = ErrorLogStream.getForClass(DBConn.class);

  public Connection makeConnection() {
    String url=connInfo.fullUrl();
    if (dbConnection != null) {
      try {
        dbConnection.close();//don't trust finalizer
      } catch (SQLException e) {
        dbg.Caught(e, "Closing an abandoned database connection");
      }
      dbConnection = null;
    }
    try (AutoCloseable pop=dbg.Push("Connect to Database")){

      Properties loginProperties = new java.util.Properties();
      loginProperties.put("user", connInfo.username);
      loginProperties.put("password", connInfo.password);

      dbg.VERBOSE("Attempting connection to: {0}", url );
      dbConnection = DriverManager.getConnection(url, loginProperties);
      dbConnection.setAutoCommit(connInfo.autoCommit);
    } catch (Exception e) {
      dbConnection = null;
      dbg.ERROR("Couldn''t connect to datasource {0}  via user: {1} reason: {2}",  url,connInfo.username,e.getMessage() );
//      dbg.Caught(e);
    }
    return dbConnection;
  }

  public Connection makeConnection(DBConnInfo connInfo){
    this.connInfo = connInfo;
    return makeConnection();
  }
}
