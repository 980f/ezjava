package pers.hal42.database;

import pers.hal42.lang.StringX;
import pers.hal42.transport.Storable;

import java.util.Properties;

import static java.text.MessageFormat.format;

@Storable.Stored
public class DBConnInfo {
  public String drivername;
  public String urlFormat;//{0} is filled with @see server
  public String server;
  public String username;
  public String password;
  public boolean autoCommit;
  public boolean readOnly; //
  public int intervalsecs = 0;
  public String keepaliveSQL = "";

  public DBConnInfo() {

  }

  public void addOptions(Properties props) {
  }

  /**
   * @returns whether this connection is to the same db server as @param conninfo
   */
  public boolean is(MysqlConnectionInfo conninfo) {
    return (conninfo != null) && StringX.equalStrings(server, conninfo.server) && StringX.equalStrings(urlFormat, conninfo.urlFormat);
  }

  public String fullUrl() {
    return format(urlFormat, server);
  }

  @Override
  public String toString() {
    return format("DBConnInfo driver:{0}  url:{1} server:{2} username:{3} password:{4} ", drivername, urlFormat, server, username, StringX.NonTrivial(password) ? "<set>" : "<blank>");
  }
}
