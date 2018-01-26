package pers.hal42.database;

import org.jetbrains.annotations.NotNull;
import pers.hal42.lang.SimpleEntry;
import pers.hal42.lang.VectorSet;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static pers.hal42.database.DBMacros.dbg;

public class ResultSetMap implements Map<String, Object> {
  public final ResultSet rs;
  /** lazy init and might fail to init */
  private ResultSetMetaData rsmd = null;

  public ResultSetMap(ResultSet rs) {
    this.rs = rs;
    //defer rsmd in case we don't need it.
  }

  /** @returns the metadata, if it can be acquired, else null */
  public ResultSetMetaData getMeta() {
    if (rs == null) {
      return null;
    }
    if (rsmd == null) {
      try {
        rsmd = rs.getMetaData();
        return rsmd;
      } catch (SQLException e) {
        dbg.Caught(e);
        return null;
      }
    } else {
      return rsmd;
    }
  }

  @Override
  /** @returns the number of columns/ result items, 0 if none, -1 if exception occured.*/
  public int size() {
    if (getMeta() != null) {
      try {
        return rsmd.getColumnCount();
      } catch (SQLException e) {
        dbg.Caught(e);
        return -1;
      }
    } else {
      return 0;
    }
  }

  /** @returns whether there are any columns. This is expensive. */
  @Override
  public boolean isEmpty() {
    return size() <= 0;
  }

  /** @returns whether there is a column whose name is case insensitively the same as String.valueOf(@param key) */
  @Override
  public boolean containsKey(Object key) {
    if (getMeta() != null) {
      //noinspection RedundantIfStatement
      if (indexFor(key) > 0) {
        return true;
      }
      return false;
    } else {
      //todo:1 perhaps throw exception
      return false;
    }
  }

  @Override
  public boolean containsValue(Object value) {
    //todo:1 perhaps throw exception
    return false;
  }

  @Override
  public Object get(Object key) {
    int index = indexFor(key);
    if (index > 0) {
      try {
        return rs.getObject(index);
      } catch (SQLException e) {
        dbg.Caught(e);
        return null;  //getobject failed.
      }
    } else {
      return null;
    }
  }

  @Override
  public Object put(String key, Object value) {
    try {
      int index = indexFor(key);
      if (index > 0) {
        rs.updateObject(index, value);
      }
    } catch (SQLException e) {
      dbg.Caught(e);
    }
    return null;
  }

  @Override
  public Object remove(Object key) {
    //todo:1 perhaps throw exception
    return null;
  }

  @Override
  public void putAll(@NotNull Map<? extends String, ?> m) {
    //todo:1 perhaps throw exception
  }

  @Override
  public void clear() {
    //todo:1 perhaps throw exception
  }

  @NotNull
  @Override
  public Set<String> keySet() {
    if (getMeta() != null) {
      try {
        final int size = rsmd.getColumnCount();
        VectorSet<String> set = new VectorSet<>(size);
        for (int ci = size; ci > 0; --ci) {//1-based value
          set.add(ci - 1, rsmd.getColumnName(ci));
        }
        return set;
      } catch (SQLException e) {
        dbg.Caught(e);
        return new VectorSet<>(0);
      }
    } else {
      return new VectorSet<>(0);
    }
  }

  @NotNull
  @Override
  public Collection<Object> values() {
    if (getMeta() != null) {
      try {
        final int size = rsmd.getColumnCount();
        VectorSet<Object> set = new VectorSet<>(size);
        for (int ci = size; ci > 0; --ci) {//1-based value
          set.add(ci - 1, rs.getObject(ci));
        }
        return set;
      } catch (SQLException e) {
        dbg.Caught(e);
        return new VectorSet<>(0);
      }
    } else {
      return new VectorSet<>(0);
    }
  }

  @NotNull
  @Override
  public Set<Entry<String, Object>> entrySet() {
    VectorSet<Entry<String, Object>> set = new VectorSet<>(0);
    if (getMeta() != null) {
      try {
        final int size = rsmd.getColumnCount();
        set.ensureCapacity(size);
        for (int ci = size; ci > 0; --ci) {//1-based value
          set.add(ci - 1, new SimpleEntry<>(rsmd.getColumnName(ci), rs.getObject(ci)));
        }
        return set;
      } catch (SQLException e) {
        dbg.Caught(e);
        return set;
      }
    } else {
      return set;
    }
  }

  /**
   * @returns the numbered position of the named entity, 0 if not a known name, -1 on sql exception
   * since the map interface doesn't know our key is a String we can actually feed something else since we use String.valueOf here.
   */
  public int indexFor(Object key) {
    try {
      for (int ci = rsmd.getColumnCount(); ci > 0; --ci) {//1-based value
        String cname = rsmd.getColumnName(ci);
        if (cname.equalsIgnoreCase(String.valueOf(key))) {
          return ci;
        }
      }
    } catch (SQLException e) {
      dbg.Caught(e);
      return -1;
    }
    return 0;
  }
}
