package pers.hal42.database;

/**
 * Title:        $Source: /cvs/src/net/paymate/database/KeyProfile.java,v $
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:      PayMate.net
 * @author PayMate.net
 * @version $Revision: 1.1 $
 */

public abstract class KeyProfile extends Constraint {

  public abstract boolean isPrimary();

  public KeyProfile(String name, TableProfile table, ColumnProfile field) {
    super(name, table, field);
  }
}
