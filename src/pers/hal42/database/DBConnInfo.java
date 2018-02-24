package pers.hal42.database;

import pers.hal42.lang.StringX;
import pers.hal42.transport.Storable;

import java.util.Properties;

import static java.text.MessageFormat.format;

@Storable.Stored
public class DBConnInfo {
  /** jdbc needs a class name */
  public String drivername;
  /** database url format, {0} is filled with @see server */
  public String urlFormat;
  /** part of url likely to change often */
  public String server;
  /** jdbc login */
  public String username;
  /** jdbc login */
  public String password;
  /** whether to disable transaction logic, by commiting things as they execute. */
  public boolean autoCommit=true; //we don't need no stinking transactions ...
  /** helps protect the engineer from entering the wrong target for debug work */
  public boolean readOnly=true; //better safe ...
  /** keepalive isn't well automated yet, a few experiments are underway */
  public int intervalsecs = 0;
  /** sql for this engine that is executed periodically to keep a connection alive.*/
  public String keepaliveSQL = "";
  /**
   * whether ddl should be deferred through writing a file.
   */
  public boolean spoolDDL = false;
  /**
   * filename for spoolDDL feature
   */
  public String ddlSpoolFilename = "ddlspool.sql";

  public DBConnInfo() {
    //#nada
  }

  public void addOptions(Properties props) {
    //#hook
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
    return format("{0}\t{1} X :{2} u:{3} pass:{4} ", drivername, urlFormat, server, username, StringX.NonTrivial(password) ? "<set>" : "<blank>");
  }
}
