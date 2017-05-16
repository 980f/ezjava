package pers.hal42.database;

/**
 * reference to a column in a different table
 */

public class ForeignKeyProfile extends KeyProfile {
  public TableProfile referenceTable = null;

  public ForeignKeyProfile(String name, TableProfile table, ColumnProfile field, TableProfile referenceTable) {
    super(name, table, field);
    this.referenceTable = referenceTable;
  }

  public boolean isPrimary() {
    return false; // if it isn't primary, it is foreign
  }
}
