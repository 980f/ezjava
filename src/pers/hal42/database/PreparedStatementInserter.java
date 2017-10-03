package pers.hal42.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Predicate;

/** to simplify sequential setting of prepared statement values */
public class PreparedStatementInserter implements AutoCloseable {
  final public boolean legit;
  /** diagnostic counter */
  public int counter = 0;
  public int grandCounter = 0;
  public int grandSum = 0;

  /** how much batch to accumulate before sending. defaults to send all right away. */
  public int batchSize=0;

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

  /** setNext on each value from a list, with optional (when not null) mapping function e.g. Iterator<Quarter> it, Quarter.toymd() */
  public <T> int setList(Iterator<T> it, Function<T, Object> transformer) throws SQLException {
    while (it.hasNext()) {//#forEach stifles exceptions
      T item = it.next();
      setNext(transformer != null ? transformer.apply(item) : item);
    }
    return sti;
  }

  /** call @see setNext on each item in order */
  public int setMany(Object ... objs) throws SQLException {
    for(Object item:objs){
      setNext(item);
    }
    return sti;
  }

   /** add to batch, run the batch if batchSize entries have been added */
  public void batch() throws SQLException {
    if (legit) {
      st.addBatch();
      if (++counter >= batchSize) {
        onBatchRun(DBMacros.doBatch(st));
      }
      rewind();
    }
  }

  /** for diagnostics call this when batch has been run if you run it outside of the provided batch() method. */
  public void onBatchRun(int oneSum) {
    grandSum += oneSum;
    grandCounter += counter;
    counter = 0;
  }

  /** execute pending batch */
  public int flush() throws SQLException {
    if (counter > 0) {
      onBatchRun(DBMacros.doBatch(st));
    }
    return grandSum;
  }

  /** if you don't care about the value returned by flush you can use a try-with-resources and let this take care of the final partial batch. */
  @Override
  public void close() throws SQLException {
    flush();
  }

  public ResultSet execute() throws SQLException {
    return st.executeQuery();
  }
}
