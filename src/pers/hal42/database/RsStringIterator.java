package pers.hal42.database;

import pers.hal42.text.StringIterator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * for a resultSet that only supplies one (meaningful) value, this lets you iterate over it.
 * this class must wholly own the result set as it manipulates the rs' state.
 * it closes the resultSet when iteration completes.
 * If you need multiple fields you must extract them in parallel.
 */
public class RsStringIterator implements StringIterator {
  public interface onExhaustion {
    @SuppressWarnings("MethodNameSameAsClassName")
    void onExhaustion(RsStringIterator si);
  }

  public onExhaustion notifyWhenDone;
  /** we must hold on to the statement in order to keep the resultset open */
  private Statement st;

  ResultSet rs;
  int col;
  /** a diagnostic added when resultset was getting closed by some outside agency. */
  private int count = 0;
  //must lookahead
  private boolean haveNext;


  /** you already know which column is of interest */
  public RsStringIterator(ResultSet rs, int whichColumn) {

    try {
      this.rs = rs;
      st = rs.getStatement();
      col = whichColumn;
      if (col > 0) {
        bump();
      } else {
        haveNext = false;
      }
    } catch (SQLException e) {
      DBMacros.dbg.Caught(e);
    }

  }

  /** single column resultset */
  public RsStringIterator(ResultSet rs) {
    this(rs, 1);
  }

  /** column by name, but see class doc for why use of this is likely to be rare */
  public RsStringIterator(ResultSet rs, String cname) {

    try {
      this.rs = rs;
      st = rs.getStatement();
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
      if (haveNext) {
        ++count;
      } else {
        if (notifyWhenDone != null) {
          notifyWhenDone.onExhaustion(this);
        } else {  //at least try to release resources in a timely fashion.
          //noinspection FinalizeCalledExplicitly
          finalize();
        }

      }
    } catch (SQLException e) {
      DBMacros.dbg.Caught(e);
      haveNext = false;
    } catch (Throwable throwable) {
      //ignore
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
    unlink();
    //but don't close the connection, in case we aren't the only user.
  }

  public void unlink() {
    try {
      rs.close(); //we own the result set.
      st.close(); //and its statement.
      notifyWhenDone = null;
    } catch (SQLException ignored) {
      //
    }

  }
}
