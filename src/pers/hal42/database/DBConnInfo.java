package pers.hal42.database;

import pers.hal42.lang.StringX;
import pers.hal42.transport.Storable;

import java.util.Properties;

import static java.text.MessageFormat.format;

/**
 * database connection info, typically feeds jdbc connect string.
 * <p>
 * defaults are set for most recent contract ;)
 */
@Storable.Stored
public class DBConnInfo {

  public String drivername = "com.mysql.jdbc.Driver";
  public String urlFormat = "jdbc:mysql://{0}.fedfis.com";//{0} is filled with @see server
  public String server = "test";
  public String username = "admin";
  public String password = "";

  public boolean autoCommit = false;
  public boolean readOnly = true; //

  public Properties custom = new Properties();

  public int intervalsecs = 0;
  public String keepaliveSQL = "";

  public DBConnInfo() {
//    mysqlDefaults();//todo:1 push this to elsewhere
  }

  /**
   * @returns whether this connection is to the same db server as @param conninfo
   */
  public boolean is(DBConnInfo conninfo) {
    return (conninfo != null) && StringX.equalStrings(server, conninfo.server) && StringX.equalStrings(urlFormat, conninfo.urlFormat);
  }

  public String fullUrl() {
    return format(urlFormat, server);
  }

  public void mysqlDefaults() {
    custom.put("connectTimeout", 0);
    custom.put("socketTimeout", 0);
    custom.put("useCompression", true);
    custom.put("useCursorFetch", true);
    custom.put("useInformationSchema", true); //todo:1 see if this fixes useing metadata instead of direct access to infoschema.
  }

  @Override
  public String toString() {
    return format("DBConnInfo driver:{0}  url:{1} server:{2} username:{3} password:{4} ", drivername, urlFormat, server, username, StringX.NonTrivial(password) ? "<set>" : "<blank>");
  }
}

