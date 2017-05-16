package pers.hal42.database;

import java.util.Vector;

public class ColumnVector extends Vector<ColumnProfile> {
  public ColumnProfile itemAt(int index) {
    return elementAt(index);
  }
}
