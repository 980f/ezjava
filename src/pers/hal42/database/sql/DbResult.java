package pers.hal42.database.sql;
import java.util.*;
import java.sql.*;
/**
 * Currently this class is not used.
 * @author Chris Bitmead
 */
public class DbResult {
  ResultSet rs;
  DbResult(ResultSet rs) {
    this.rs = rs;
  }
}
