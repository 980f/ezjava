package pers.hal42.database;

/**
 * <p>Title: $Source: /cvs/src/net/paymate/database/ColumnVector.java,v $</p>
 * <p>Description: database validator uses this</p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: PayMate.net</p>
 * @author PayMate.net
 * @version $Revision: 1.1 $
 */

import java.util.Vector;

public class ColumnVector extends Vector {
  public ColumnProfile itemAt(int index) {
    return (ColumnProfile) elementAt(index);
  }
}
