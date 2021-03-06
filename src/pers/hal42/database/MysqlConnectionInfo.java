package pers.hal42.database;

import pers.hal42.transport.JsonOptions;
import pers.hal42.transport.Storable;

import java.util.Properties;

/**
 * database connection info, typically feeds jdbc connect string.
 * <p>
 * defaults are set for fedfis
 */
@Storable.Stored
public class MysqlConnectionInfo extends DBConnInfo {

  public Options opts = new Options();

  public MysqlConnectionInfo() {
    readOnly = true;
    urlFormat = "jdbc:mysql://{0}.fedfis.com";
    server = "";
    autoCommit = true;
    password = "";
    username = "admin";
    drivername = "com.mysql.jdbc.Driver";
  }

  public MysqlConnectionInfo(Storable node) {

    Storable.Nodal<MysqlConnectionInfo> loader = new Storable.Nodal<>(node, this);
  }

  /** add options to login properties object */
  @Override
  public void addOptions(Properties props) {
    opts.putInto(props, true);//NEW
  }

  public static class Options extends JsonOptions {
    public int connectTimeout = 0;
    public int socketTimeout = 0;

    public boolean useCompression = false;
    public boolean useCursorFetch = false;
    public boolean useInformationSchema = false; //todo:1 see if this fixes using metadata instead of direct access to infoschema.

    public Options() {
      updateDOM();//todo:1 figure out how to get base class to do this.
    }
  }
}

