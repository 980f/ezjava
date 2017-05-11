/**
 * Title:        DatabaseProfile
 * Description:  Contains information for table(s) used by database profiling, etc.
 * Copyright:    Copyright (c) 2000
 * Company:      PayMate.net
 * @author       PayMate.net
 * @version      $Id: DatabaseProfile.java,v 1.7 2003/07/27 05:35:00 mattm Exp $
 */

package pers.hal42.database;
import pers.hal42.lang.StringX;

import java.util.Vector;

public class DatabaseProfile extends Vector<TableProfile> {
  private String name = null;
  public TableProfile itemAt(int index) {
    return (TableProfile)elementAt(index);
  }
  public DatabaseProfile(String name) {
    this.name = name;
  }
  public String name() {
    return name;
  }
  public TableProfile tableFromName(String tablename) {
    for(TableProfile tp:this){
      if((tp != null) && StringX.equalStrings(tp.name(), tablename, true/*ignoreCase*/)) {
        return tp;
      }
    }
    return null;
  }
}
