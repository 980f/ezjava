package pers.hal42.database;

/**
 * Title:        $Source: /cvs/src/net/paymate/database/PrimaryKeyProfile.java,v $
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:      PayMate.net
 * @author PayMate.net
 * @version $Revision: 1.2 $
 */

public class PrimaryKeyProfile extends KeyProfile {

  public boolean isPrimary() {
    return true; // if it isn't primary, it is foreign
  }

  public PrimaryKeyProfile(String name, TableProfile table, ColumnProfile field) {
    super(name, table, field);
  }

}
