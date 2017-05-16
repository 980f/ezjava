package pers.hal42.database.sql;

import pers.hal42.logging.ErrorLogStream;

/**
 * Generates unique values for use as keys. Orignally crafted for postgres Sequence feature, wrapped so that we can implement something like it for different db's.
 */

public class DbSequence {

  String name;
  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(DbSequence.class);

  DbSequence(String name) {
    this.name = name;
  }

  public int next() {
//      String sql = "";//props.getProperty(db.getProperty("vendor") + ".nextSequenceSql");
//      sql = pers.hal42.lang.StringX.replace(sql, "${name}", name);
//      dbg.ERROR("DbSequence.next: "+ sql);
//      PreparedStatement ps = con.prepareStatement(sql);
//      ResultSet rs = ps.executeQuery();
//      if (!rs.next()) {
//        throw new Exception("Can't read " + name + " sequence");
//      }
//      int rtn = rs.getInt(1);
//      ps.close();
//      return rtn;
    return -1;
  }
}
