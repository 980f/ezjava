package pers.hal42.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.function.Function;

import static pers.hal42.database.DBMacros.dbg;

/** to simplify sequential setting of prepared statement values */
public class PreparedStatementInserter implements AutoCloseable {
  final public boolean legit;
  /** diagnostic counter */
  public int counter = 0;
  public int grandCounter = 0;
  public int grandSum = 0;

  /** how much batch to accumulate before sending. defaults to send all right away. */
  public int batchSize = 0;

  //feel free to subvert this class, it is an assistant not a manager
  public PreparedStatement st;
  //feel free to subvert this class, it is an assistant not a manager
  public int sti;

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
        try {
          st.setObject(++sti, item);
        } catch (SQLException e) {
          dbg.Caught(e, "setNext[{1}]:{0} value ", item, sti);
          throw e;
        }
      }
    }
    return sti;
  }

  /** random access setter */
  public <T> void set(ColumnAttributes col, T value) throws SQLException {
    if (legit) {
      st.setObject(col.ordinal, value);
    }
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
  public int setMany(Object... objs) throws SQLException {
    for (Object item : objs) {
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

  /** @returns new keys from running what is presumed to be an insert. */
  public ResultSet executeInsert(int[] autokeys) throws SQLException {
    if (st.execute()) {
      return st.getGeneratedKeys();
    } else {
      return null;
    }
  }

  /** executes statement and @returns its ResultSet, possibly null */
  public ResultSet execute() throws SQLException {
    if (st.execute()) {//executeQuery() got "Can not issue data manipulation statements with executeQUery();
      return st.getResultSet();
    } else {
      return null;
    }
  }
}
