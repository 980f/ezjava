package pers.hal42.database;

import pers.hal42.lang.StringX;
import pers.hal42.transport.Storable;

/**
 * database connection info, typically feeds jdbc connect string.
 */
public class DBConnInfo {
  @Storable.Stored
  public String drivername = "";

  @Storable.Stored
  public String connDatasource;
  @Storable.Stored
  public String connUser = "";
  @Storable.Stored
  public String connPass = "";

  @Storable.Stored
  public boolean autoCommit = false;
  @Storable.Stored
  public int oversize = 0;
  @Storable.Stored
  public int intervalsecs = 10;
  @Storable.Stored
  public String keepaliveSQL = "";

  public DBConnInfo(String connDatasource, String connUser, String connPass, String drivername, int oversize, int intervalsecs, String keepaliveSQL) {
    this.connDatasource = connDatasource;
    this.connUser = connUser;
    this.connPass = connPass;
    this.drivername = drivername;
    this.oversize = oversize;
    this.intervalsecs = intervalsecs;
    this.keepaliveSQL = keepaliveSQL;
  }

  public DBConnInfo(String connDatasource, String connUser, String connPass, String drivername, int oversize, int intervalsecs) {
    this(connDatasource, connUser, connPass, drivername, oversize, intervalsecs, "");
  }

  public DBConnInfo(String connDatasource, String connUser, String connPass, String drivername, int oversize) {
    this(connDatasource, connUser, connPass, drivername, oversize, 10);
  }

  public DBConnInfo(String connDatasource, String connUser, String connPass, String drivername) {
    this(connDatasource, connUser, connPass, drivername, 0);
  }

  public DBConnInfo(String connDatasource, String connUser, String connPass) {
    this(connDatasource, connUser, connPass, "jdbc:mysql");
  }

  /**
   * @returns whether this connection is to the same db as @param conninfo
   */
  public boolean is(DBConnInfo conninfo) {
    return (conninfo != null) && StringX.equalStrings(connDatasource, conninfo.connDatasource);
  }

  public String toString() {
    return "connDatasource=" + connDatasource + ", connUser=" + connUser + ", connPass=" + connPass + ", drivername=" + drivername + ", oversize=" + oversize + ", intervalsecs=" + intervalsecs;
  }

}

