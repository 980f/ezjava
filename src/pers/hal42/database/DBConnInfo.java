package pers.hal42.database;

import pers.hal42.lang.StringX;

/**
 * database connection info, typically feeds jdbc connect string.
 */
public class DBConnInfo {

  public String connDatasource = "";
  public int oversize = DEFAULTOVERSIZE;
  public int intervalsecs = DEFAULTINTERVALSECS;
  public String connUser = "";
  public String connPass = "";
  public String drivername = "";
  public String keepaliveSQL = DEFAULTKEEPALIVESQL;

  public static final int DEFAULTOVERSIZE = 0;
  public static final int DEFAULTINTERVALSECS = 10;
  public static final String DEFAULTKEEPALIVESQL = "select 1;";
  public static final String DEFAULTDRIVER = "jdbc:postgresql:mainsail";

  public DBConnInfo() {
  }

  public DBConnInfo(String connDatasource, String connUser, String connPass, String drivername, int oversize, int intervalsecs, String keepaliveSQL) {
    this.connDatasource = connDatasource;
    this.connUser = connUser;
    this.connPass = connPass;
    this.drivername = StringX.TrivialDefault(drivername, DEFAULTDRIVER);
    this.oversize = oversize;
    this.intervalsecs = intervalsecs;
    this.keepaliveSQL = keepaliveSQL;
  }

  public DBConnInfo(String connDatasource, String connUser, String connPass, String drivername) {
    this(connDatasource, connUser, connPass, drivername, DEFAULTOVERSIZE, DEFAULTINTERVALSECS, DEFAULTKEEPALIVESQL);
  }

  public DBConnInfo(String connDatasource, String connUser, String connPass) {
    this(connDatasource, connUser, connPass, null, DEFAULTOVERSIZE, DEFAULTINTERVALSECS, DEFAULTKEEPALIVESQL);
  }

  public boolean is(DBConnInfo conninfo) {
    return (conninfo != null) && StringX.equalStrings(connDatasource, conninfo.connDatasource);
  }

  public String toString() {
    return "connDatasource=" + connDatasource +
      ", connUser=" + connUser +
      ", connPass=" + connPass +
      ", drivername=" + drivername +
      ", oversize=" + oversize +
      ", intervalsecs=" + intervalsecs;
  }

}

