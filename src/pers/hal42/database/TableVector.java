package pers.hal42.database;

/**
 * <p>Title: $Source: /cvs/src/net/paymate/database/TableVector.java,v $</p>
 * <p>Description: validator uses this</p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: PayMate.net</p>
 * @author PayMate.net
 * @version $Revision: 1.1 $
 */

import java.util.Vector;

public class TableVector extends Vector {
  public TableProfile itemAt(int index) {
    return (TableProfile) elementAt(index);
  }
}
