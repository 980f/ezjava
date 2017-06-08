package pers.hal42.database;

public enum ColumnTypes {
  DECIMAL,   //money
  INT4,
  SMALLINT,
  BIGINT,
//only care about 64 bit hosts, remove distinction between float and  DOUBLE,
//let NUMERIC cover numbers which don't have a precision requirement  FLOAT,
  TINYINT,
//get rid of this synonym, we aren't doing physics  REAL,
  NUMERIC,
  BOOL,
  TIME,       //externally supplied time
  TIMESTAMP,  //databse generated time value
  DATE,
  DATETIME,
  BYTE,
  CHAR,
  SERIAL,     //as in serial number
  TEXT,       //human readable tag, use varchar for content
  VARCHAR,
  ARRAY,
  BINARY,
  BIT,
  BLOB,
  CLOB,
  DISTINCT,
  JAVA,
  LONGVARBINARY,
  LONGVARCHAR,
  NULL,
  OTHER,
  REF,
  STRUCT,
  VARBINARY;

  public String DeclarationText(){
    return this.toString();//will work for most, add exceptions for the few.
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
