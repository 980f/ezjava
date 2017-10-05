package pers.hal42.database;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.function.Function;

/** step through fields of a row with default values for nulls */
public class ResultSetFieldIterator {
  public ResultSet rs;
  public final int columnCount;
  int ordinator = 0;//we don't count auto's
  private final ResultSetMetaData metaData;

  public ResultSetFieldIterator(ResultSet rs) throws SQLException {
    this.rs = rs;
    metaData = rs.getMetaData();
    columnCount = metaData.getColumnCount();
  }

  public boolean hasNext() {
    return ordinator < columnCount;
  }

  public int get(int defawlt) {
    if (ordinator < columnCount) {
      try {
        return rs.getInt(ordinator++);
      } catch (SQLException e) {
        return defawlt;
      }
    }
    return defawlt;
  }

  public String get(String defawlt) {
    if (ordinator < columnCount) {
      try {
        return rs.getString(ordinator++);
      } catch (SQLException e) {
        return defawlt;
      }
    }
    return defawlt;
  }

  /** fabricate a thing from a resultset object, usually just a type verifier returning null if incompatible */
  public <T> T getObject(Function<Object, T> factory) {
    if (ordinator < columnCount && factory != null) {
      try {
        final Object object = rs.getObject(ordinator++);
        return factory.apply(object);
      } catch (SQLException e) {
        return null;
      }
    }
    return null;
  }

  /** fabricate a thing from a resultset object, usually just a type verifier returning null if incompatible */
  public <T> T getThing(Function<ResultSetFieldIterator, T> factory) {
    if (ordinator < columnCount && factory != null) {
      return factory.apply(this);
    }
    return null;
  }

  public ResultSetFieldIterator skip(int howmany) {
    ordinator += howmany;
    if (ordinator < 0) {
      ordinator = 0;
    } else if (ordinator > columnCount) {
      ordinator = columnCount;//less distracting for debug, and gives us a place to set a breakpoint.
    }
    return this;
  }
}
