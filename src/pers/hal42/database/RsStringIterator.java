package pers.hal42.database;

import pers.hal42.text.StringIterator;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * for a resultSet that only supplies one (meaningful) value, this lets you iterate over it.
 * this class must wholly own the result set as it manipulates the rs' state.
 * it closes the resultSet when iteration completes.
 * If you need multiple fields you must extract them in parallel.
 */
public class RsStringIterator implements StringIterator {
  ResultSet rs;
  int col;
  //must lookahead
  private boolean haveNext;

  /** you already know which column is of interest */
  public RsStringIterator(ResultSet rs, int whichColumn) {
    this.rs = rs;
    col = whichColumn;
    if (col > 0) {
      bump();
    } else {
      haveNext = false;
    }
  }

  /** single column resultset */
  public RsStringIterator(ResultSet rs) {
    this(rs, 1);
  }

  /** column by name, but see class doc for why use of this is likely to be rare */
  public RsStringIterator(ResultSet rs, String cname) {
    this.rs = rs;
    try {
      col = rs.findColumn(cname);
      bump();
    } catch (SQLException e) {
      DBMacros.dbg.Caught(e);
      col = 0;//an illegal value for sql
      haveNext = false;
    }
  }

  private void bump() {
    try {
      haveNext = rs.next();
      if (!haveNext) {
        rs.close();
      }
    } catch (SQLException e) {
      DBMacros.dbg.Caught(e);
      haveNext = false;
    }
  }

  @Override
  public boolean hasNext() {
    return haveNext;
  }

  /**
   * @returns null if hasNext() returned false;
   */
  @Override
  public String next() {
    try {
      return rs.getString(col);
    } catch (SQLException e) {
      DBMacros.dbg.Caught(e);
      return null;
    } finally {
      bump();
    }
  }

  /** just in case someone abandons an iterator */
  @Override
  protected void finalize() throws Throwable {
    rs.close();
  }
}
