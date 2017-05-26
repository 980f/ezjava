package pers.hal42.database;

import pers.hal42.lang.StringX;
import pers.hal42.transport.Storable;

import java.text.MessageFormat;

/**
 * database connection info, typically feeds jdbc connect string.
 *
 * defaults are set for most recent contract ;)
 */
public class DBConnInfo {
  @Storable.Stored
  public String drivername = "";

  @Storable.Stored(legacy="connDatasource")
  public String urlFormat ="jdbc:mysql://{0}.fedfis.com";
  @Storable.Stored
  public String server ="test";
  @Storable.Stored(legacy="connUser")
  public String username = "";
  @Storable.Stored(legacy="connPass")
  public String password = "";

  @Storable.Stored
  public boolean autoCommit = false;
  @Storable.Stored
  public int oversize = 0;
  @Storable.Stored
  public int intervalsecs = 100;
  @Storable.Stored
  public String keepaliveSQL = "";

  public DBConnInfo() {

  }

  /**
   * @returns whether this connection is to the same db server as @param conninfo
   */
  public boolean is(DBConnInfo conninfo) {
    return (conninfo != null) && StringX.equalStrings(server, conninfo.server) &&  StringX.equalStrings(urlFormat, conninfo.urlFormat);
  }

  public String fullUrl(){
    return MessageFormat.format(urlFormat,server);
  }

  @Override
  public String toString() {
    return "DBConnInfo{" + "drivername='" + drivername + '\'' + ", urlFormat='" + urlFormat + '\'' + ", server='" + server + '\'' + ", username='" + username + '\'' + ", password='" + password + '\'' + ", autoCommit=" + autoCommit + ", oversize=" + oversize + ", intervalsecs=" + intervalsecs + ", keepaliveSQL='" + keepaliveSQL + '\'' + '}';
  }

}

