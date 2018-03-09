package pers.hal42.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.function.Function;

import static pers.hal42.database.DBMacros.dbg;

/** to simplify sequential setting of prepared statement values */
public class PreparedStatementInserter implements AutoCloseable {
  /** whether constuctor had sane args. At a minium st!=null */
  final public boolean legit;
  /** batches stored since last batch execution */
  public int counter = 0;
  /** batches since object created */
  public int grandCounter = 0;
  /** sum of returned values, probably somewhat related to number of records modified. */
  public int grandSum = 0;

  /** how much batch to accumulate before sending. defaults to send all right away. */
  public int batchSize = 0;
  //feel free to subvert this item, this class is an assistant not a manager
  /** the statement that we are managing setting values on. */
  public PreparedStatement st;
  //feel free to subvert this item, this class is an assistant not a manager
  /** preincrementing pointer to next field to insert */
  public int sti;

  public PreparedStatementInserter(PreparedStatement st) {
    legit = st != null;
    this.st = st;
    sti = 0;
  }

  public boolean rewind() {
    if (legit) {
      try {
        st.clearParameters();//application,type_id,id,period_end,map_type ,parent_id);  status,result,note
        return true;
      } catch (SQLException e) {
        dbg.Caught(e, "clearing parameters, highly unlikely");
        return false;
      } finally {
        sti = 0;
      }
    } else {
      return true;
    }
    //but leave grand alone
  }

  public int nextItem() {
    return ++sti;
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

  /**
   * add to batch, run the batch if batchSize entries have been added.
   *
   * @returns whether a query was made
   */
  public boolean batch() throws SQLException {
    return legit && addbatch();
  }

  /** @param stringify is whether to convert items to string before setting on statement. This ia advisable for char being sent to a varchar and ditto for enum. mysql/j does a horrible job of passing such items about. */
  public void batchSet(Iterator it, boolean stringify) throws SQLException {
    if (legit) {
      ++sti;//just once, and the iterated arg has to be the final one
      while (it.hasNext()) {
        final Object next = it.next();
        st.setObject(sti, stringify ? String.valueOf(next) : next);
        addbatch();
      }
    }
  }

  /** @returns whether a query was made */
  private boolean addbatch() throws SQLException {
    try {
      st.addBatch();
      if (++counter >= batchSize) {
        onBatchRun(DBMacros.doBatch(st));
        return true;
      }
      return false;
    } finally {
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
  public ResultSet executeInsert(/*int[] autokeys*/) throws SQLException {
    if (st.execute()) {
      return st.getGeneratedKeys();
    } else {
      return null;
    }
  }

  /** executes statement and @returns its ResultSet, possibly null. rewinds pointer for next use. */
  public ResultSet execute() throws SQLException {
    try {
      if (legit && st.execute()) {//executeQuery() got "Can not issue data manipulation statements with executeQUery();
        return st.getResultSet();
      } else {
        return null;
      }
    } finally {
      rewind();
    }
  }

  /** for debug */
  @Override
  public String toString() {
    return legit ? st.toString() : "(null)";
  }
}
