package pers.hal42.database;

import pers.hal42.text.TextList;

import java.util.Vector;

/**
 * List of Table Information structures, for database backup, creation, and copying
 */
public class TableInfoList extends Vector<TableInfo> {

  // Since this extends Vector, synchronizing makes sense

  public synchronized TableInfo itemAt(int i) {
    TableInfo ti = null;
    if (i < size() && i > -1) {
      ti = elementAt(i);
    }
    return ti;
  }

  public synchronized int indexOf(String name) {
    int ret = -1;
    for (int i = size(); i-- > 0; ) {
      if (itemAt(i).name().equals(name)) {
        ret = i;
        break;
      }
    }
    return ret;
  }

  public synchronized TextList justNames() {
    TextList tl = new TextList(size());
    for (int i = 0; i < size(); i++) {
      tl.add(itemAt(i).name());
    }
    return tl;
  }

  public synchronized TableInfoList sortByName() {
    final Object[] theList = toArray();
    java.util.Arrays.sort(theList);
    this.setSize(0);
    for (int i = theList.length; i-- > 0; ) {
      this.insertElementAt((TableInfo) theList[i], 0); // put in in reverse order, but at the front, which makes it the CORRECT order
    }
    return this;
  }
}

