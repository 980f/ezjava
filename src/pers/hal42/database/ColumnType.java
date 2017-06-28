package pers.hal42.database;

import java.text.MessageFormat;

public enum ColumnType {
  DECIMAL(false),   //money
  BIGINT(false), INTEGER(false),  //int4 seems to have been postgres specific.
  SMALLINT(false), TINYINT(false), NUMERIC(false), BOOL(false), TIME(false),       //externally supplied time
  TIMESTAMP(false),  //database generated time value
  DATE(false), DATETIME(false), //  BYTE(false),
  CHAR(false), SERIAL(false),     //mysql: "SERIAL is an alias for BIGINT UNSIGNED NOT NULL AUTO_INCREMENT UNIQUE"
  TEXT(true),       //human readable tag, use varchar for content
  VARCHAR(true), LONGVARCHAR(true),

  BIT(false), BLOB(false), CLOB(false),

  BINARY(false), VARBINARY(true), LONGVARBINARY(true),

  NULL(false), OTHER(false), REF(false),;//end of list.
  /**
   * whether the column specification requires a size value
   */
  boolean requiresLength;

  ColumnType(boolean requiresLength) {
    this.requiresLength = requiresLength;
  }

  /**
   * @returns string suitable for a create table statement
   */
  public String DeclarationText(int size) {
    if (requiresLength) {
      return MessageFormat.format("{0}({1})", toString(), size);
    } else {
      return toString();//will work for most, add exceptions for the few.
    }
  }

  /**
   * @returns whether the associated datum is naturally human readable stuff
   */
  public boolean isTextlike() {
    return this == CHAR || this == TEXT;
  }

  /**
   * @returns best match type, or null. Does NOT throw exceptions.
   */
  public static ColumnType forName(String name) {
    try {
      return ColumnType.valueOf(name.toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
