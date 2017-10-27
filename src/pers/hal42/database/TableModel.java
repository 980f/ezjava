package pers.hal42.database;

import pers.hal42.lang.VectorX;

import java.util.Vector;

import static pers.hal42.lang.Index.BadIndex;

/**
 * lazy init cache of just the ones the related class defines.
 * attempt to init on construction ran afoul of wrapped class not being fully constructed when this constructor is invoked.
 */
public class TableModel {
  public final TableInfo ti;//shared object
  public final Vector<ColumnAttributes> columns = new Vector<>();
  /** an index built by scanning columns marked 'i'm a pk' */
  public final Index pkIndex = new Index();
  public final Vector<Index> indexes = new Vector<>();

  public TableModel(TableInfo ti) {
    this.ti = ti;
  }

  public ColumnAttributes getColByName(String name) {
    int which = VectorX.indexOfLast(columns, ca -> ca.name.equalsIgnoreCase(name));
    if (which != BadIndex) {
      return columns.get(which);
    } else {
      return null;
    }
  }

  public Index getIndexByName(String name) {
    int which = VectorX.indexOfLast(indexes, idx -> idx.my.name.equalsIgnoreCase(name));
    if (which != BadIndex) {
      return indexes.get(which);
    } else {
      return null;
    }
  }
}
