package pers.hal42.database;

/**
 * <p>Title: $Source: /cvs/src/net/paymate/database/IsNullIndexProfile.java,v $</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: PayMate.net</p>
 * @author PayMate.net
 * @version $Revision: 1.1 $
 */

public class IsNullIndexProfile extends IndexProfile {
  public IsNullIndexProfile(String name, TableProfile table, ColumnProfile column) {
    super(name, table, column, QueryString.Clause().where().isNull(column));
  }

}
