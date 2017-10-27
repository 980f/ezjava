package pers.hal42.database;

import pers.hal42.lang.Eponym;
import pers.hal42.lang.StringX;

import java.util.Vector;

public class Index {
  public Eponym my = new Eponym();
  public Vector<ColumnAttributes> cols = new Vector<>();

  /** create index, canonicalizing the name */
  public Index(String name, ColumnAttributes... indexed) {
    my.name = name;
    cols.ensureCapacity(indexed.length);
    for (ColumnAttributes col : indexed) {
      cols.addElement(col);
    }
  }

  public Index(ColumnAttributes... indexed) {
    this(null, indexed);
  }

  /** @returns whether you can naively make an index declaration from this. */
  public boolean isValid() {
    return cols.size() > 0 && StringX.NonTrivial(my.name);
  }
}
