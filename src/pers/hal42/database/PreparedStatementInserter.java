package pers.hal42.database;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/** to simplify sequential setting of prepared statement values */
public class PreparedStatementInserter {
  PreparedStatement st;
  int sti;

  public PreparedStatementInserter(PreparedStatement st) {
    this.st = st;
    sti = 0;
  }

  public void rewind() throws SQLException {
    st.clearParameters();//application,type_id,id,period_end,map_type ,parent_id);  status,result,note
    sti = 0;
  }

  /** @returns index of item just applied to statement */
  public int setNext(Object item) throws SQLException {
    if (item instanceof Enum) {
      st.setObject(++sti, item.toString());//mysql indicated these were ** STREAM DATA **, also we want to ensure any interceptions of ours are invoked.
    } else {
      st.setObject(++sti, item);
    }
    return sti;
  }

  /** add to batch.  @returns the number of params inserted */
  public void batch() throws SQLException {
    st.addBatch();
    rewind();
  }
}
