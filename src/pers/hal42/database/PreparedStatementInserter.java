package pers.hal42.database;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/** to simplify sequential setting of prepared statement values */
public class PreparedStatementInserter {
  final public boolean legit;
  /** diagnostic counter */
  public int counter = 0;
  public int grandCounter = 0;
  public int grandSum = 0;

  private PreparedStatement st;
  private int sti;

  public PreparedStatementInserter(PreparedStatement st) {
    legit = st != null;
    this.st = st;
    sti = 0;
  }

  public void rewind() throws SQLException {
    if (legit) {
      st.clearParameters();//application,type_id,id,period_end,map_type ,parent_id);  status,result,note
      sti = 0;
    }
    //but leave grand alone
  }

  /** @returns index of item just applied to statement */
  public int setNext(Object item) throws SQLException {
    if (legit) {
      if (item instanceof Enum) {
        st.setObject(++sti, item.toString());//mysql indicated these were ** STREAM DATA **, also we want to ensure any interceptions of ours are invoked.
      } else {
        st.setObject(++sti, item);
      }
    }
    return sti;
  }

  /** add to batch.  @returns the number of params inserted */
  public void batch() throws SQLException {
    if (legit) {
      st.addBatch();
      ++counter;
      rewind();
    }
  }

  /** for diagnostics call this when batch has been run */
  public void onBatchRun(int oneSum) {
    grandSum += oneSum;
    grandCounter += counter;
    counter = 0;
  }
}
