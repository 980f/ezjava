package pers.hal42.database;

public enum ColumnTypes {
  DECIMAL      ,
  INT4         ,
  SMALLINT     ,
  BIGINT       ,
  DOUBLE       ,
  FLOAT        ,
  TINYINT      ,
  REAL         ,
  NUMERIC      ,
  BOOL         ,
  TIME         ,
  TIMESTAMP    ,
  DATE         ,
  DATETIME     ,
  BYTE         ,
  CHAR         ,
  SERIAL       ,
  TEXT         ,
  VARCHAR      ,
  ARRAY        ,
  BINARY       ,
  BIT          ,
  BLOB         ,
  CLOB         ,
  DISTINCT     ,
  JAVA         ,
  LONGVARBINARY,
  LONGVARCHAR  ,
  NULL         ,
  OTHER        ,
  REF          ,
  STRUCT       ,
  VARBINARY    ;

  /** frequently used query, @returns whether the associated datum is naturally human readable stuff */
  public boolean isTextlike(){
    return this==CHAR || this==TEXT;
  }

  public static ColumnTypes forName(String name){
    return  ColumnTypes.valueOf(name);
  }
}
