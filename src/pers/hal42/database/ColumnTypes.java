package pers.hal42.database;

import java.text.MessageFormat;

public enum ColumnTypes {
  DECIMAL(false),   //money
  INT4(false),
  SMALLINT(false),
  BIGINT(false),
  //only care about 64 bit hosts, remove distinction between float and  DOUBLE,
//let NUMERIC cover numbers which don't have a precision requirement  FLOAT,
  TINYINT(false),
  //get rid of this synonym, we aren't doing physics  REAL,
  NUMERIC(false),
  BOOL(false),
  TIME(false),       //externally supplied time
  TIMESTAMP(false),  //databse generated time value
  DATE(false),
  DATETIME(false),
  BYTE(false),
  CHAR(false),
  SERIAL(false),     //as in serial number
  TEXT(true),       //human readable tag, use varchar for content
  VARCHAR(true),
  ARRAY(false),
  BINARY(false),
  BIT(false),
  BLOB(false),
  CLOB(false),
  DISTINCT(false),
  JAVA(false),
  LONGVARBINARY(true),
  LONGVARCHAR(true),
  NULL(false),
  OTHER(false),
  REF(false),
  STRUCT(false),
  VARBINARY(true);

  boolean requiresLength;

  ColumnTypes(boolean requiresLength) {
    this.requiresLength = requiresLength;
  }

  public String DeclarationText(int size) {
    if (requiresLength) {
      return MessageFormat.format("{0}({1})", toString(), size);
    } else {
      return toString();//will work for most, add exceptions for the few.
    }
  }

  /**
   * frequently used query, @returns whether the associated datum is naturally human readable stuff
   */
  public boolean isTextlike() {
    return this == CHAR || this == TEXT;
  }

  public static ColumnTypes forName(String name) {
    return ColumnTypes.valueOf(name);
  }
}
