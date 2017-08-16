package pers.hal42.database;

import pers.hal42.lang.Finally;
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
    try (Finally pop = dbg.Push("Connect to Database")) {
      Properties loginProperties = new java.util.Properties();
      loginProperties.put("user", connInfo.username);
      loginProperties.put("password", connInfo.password);
      //mysql specials:      //todo:1 source from properties node on DBConnInfo
//NPE in a stupid place in driver:      loginProperties.putAll(connInfo.custom);
      loginProperties.put("connectTimeout", String.valueOf(0));
      loginProperties.put("socketTimeout", String.valueOf(0));
      loginProperties.put("useCompression", String.valueOf(true));
      loginProperties.put("useCursorFetch", String.valueOf(true));
      loginProperties.put("useInformationSchema", String.valueOf(true)); //todo:1 see if this fixes useing metadata instead of direct access to infoschema.


      dbg.VERBOSE("Attempting connection to: {0}", url );
      dbConnection = DriverManager.getConnection(url, loginProperties);
      dbConnection.setAutoCommit(connInfo.autoCommit);
      dbConnection.setReadOnly(connInfo.readOnly);
    } catch (SQLException e) {
      dbConnection = null;
      dbg.ERROR("Couldn''t connect to datasource {0}  via user: {1} reason: {2}",  url,connInfo.username,e.getMessage() );
    }
    return dbConnection;
  }

  public Connection makeConnection(DBConnInfo connInfo){
    this.connInfo = connInfo;
    return makeConnection();
  }
}
