package pers.hal42.database;

import java.sql.Types;
import java.text.MessageFormat;

public enum ColumnType {
  DECIMAL(Types.DECIMAL, false),   //good for money
  BIGINT(Types.BIGINT, false), //
  INTEGER(Types.INTEGER, false),  //int4 seems to have been postgres specific.
  SMALLINT(Types.SMALLINT, false),  //
  TINYINT(Types.TINYINT, false), //
  NUMERIC(Types.NUMERIC, false), //
  BOOL(Types.BOOLEAN, false),   //
  TIME(Types.TIME, false),       //externally supplied time
  TIMESTAMP(Types.TIMESTAMP, false),  //database generated time value
  DATE(Types.DATE, false), //  BYTE(Types.,false),
  CHAR(Types.CHAR, false), //
  SERIAL(Types.OTHER, false),     //mysql: "SERIAL is an alias for BIGINT UNSIGNED NOT NULL AUTO_INCREMENT UNIQUE"
  TEXT(Types.VARCHAR, true),       //human readable tag, use varchar for content
  VARCHAR(Types.VARCHAR, true),  //
  LONGVARCHAR(Types.LONGVARCHAR, true),

  BIT(Types.BIT, false), //
  BLOB(Types.BLOB, false),  //
  CLOB(Types.CLOB, false),

  BINARY(Types.BINARY, false), //
  VARBINARY(Types.VARBINARY, true), //
  LONGVARBINARY(Types.LONGVARBINARY, true),//
  //
  NULL(Types.NULL, false), //
  REF(Types.REF, false)
  //end of list
  ;
  /**
   * whether the column specification requires a size value
   */
  boolean requiresLength;
  int jsqltype;

  ColumnType(int jsqltype, boolean requiresLength) {
    this.jsqltype = jsqltype;
    this.requiresLength = requiresLength;
  }

  /**
   * @returns string suitable for a create table statement
   */
  public String DeclarationText(int size) {
    if (requiresLength) {
      return MessageFormat.format("{0}({1})", toString(), size);
    } else {
      if (this == REF) {//mysql doesn't implement this part of the java specification
        return "INT UNSIGNED";
      }
      return toString();//will work for most, add exceptions for the few.
    }
  }

  /**
   * @returns whether the associated datum is naturally human readable stuff
   */
  public boolean isTextlike() {
    return this == CHAR || this == VARCHAR || this == LONGVARCHAR;
  }

  public boolean isLike(ColumnType other) {
    if (this == other) {
      return true;
    }
    switch (this) {
    case NUMERIC: //mysql: numeric in , decimal out.
      if (other == DECIMAL) {
        return true;
      }
      break;
    case INTEGER:
      return other == DECIMAL || other == TINYINT;//adding cases for fedfis optimization of column widths
    case REF: //keys are integers, perhaps need to allow 64 bit or any size integers as well as 32
      return other == INTEGER;
    }
    return false;
  }

  /**
   * @returns best match type, or null. Does NOT throw exceptions.
   */
  public static ColumnType forName(String name) {
    try {
      return ColumnType.valueOf(name.toUpperCase());
    } catch (IllegalArgumentException e) {
      switch (name.toLowerCase()) {//mysql synonyms
      case "int":
        return INTEGER;
      case "longtext":
        return TEXT;
      case "datetime":
        return TIMESTAMP;
      case "double":
        return NUMERIC;
      case "float":
        return NUMERIC;
      case "longblob":
        return BLOB;
      case "enum":
        return TEXT;
      case "mediumtext":
        return TEXT;
      case "set":
        return BLOB;
      case "year":
        return INTEGER;
      }
      return null;
    }
  }

  /**
   * metadata to our enum
   */
  public static ColumnType map(int javasqlTypeCode) {
    //you thinked they'd spec an enum  by now.
    for (ColumnType ct : values()) {
      if (ct.jsqltype == javasqlTypeCode) {
        return ct;
      }
    }
    return null;
  }
}

/*
* mysql madness:
* select distinct(data_type)
from
INFORMATION_SCHEMA.COLUMNS
"data_type"
"varchar"
"bigint"
"longtext"
"datetime"
"int"
"tinyint"
"decimal"
"double"
"char"
"timestamp"
"date"
"float"
"binary"
"longblob"
"blob"
"text"
"enum"
"smallint"
"mediumtext"
"set"
"year"
"time"

*
* */
