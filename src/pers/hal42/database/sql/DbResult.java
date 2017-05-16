package pers.hal42.database.sql;

import java.sql.ResultSet;

/**
 * Currently this class is not used.
 *
 * @author Chris Bitmead
 */
public class DbResult {
  ResultSet rs;

  DbResult(ResultSet rs) {
    this.rs = rs;
  }
}
