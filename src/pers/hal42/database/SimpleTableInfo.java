package pers.hal42.database;

import pers.hal42.lang.StringX;

/**
 * extend name of a table, plus some other metadata.
 */

public class SimpleTableInfo implements TableInfo {
  //don't want to depend upon db connection features for these:
  public static String SharedCatalog;
  public static String SharedSchema;
  protected String schema;
  protected String name;
  private String catalog;
  private String type = "";
  private String remark = "";

  public SimpleTableInfo(String catalog, String schema, String name, String type, String remark) {
    this.catalog = StringX.TrivialDefault(catalog, SharedCatalog);
    this.schema = StringX.TrivialDefault(schema, SharedSchema);
    this.name = StringX.TrivialDefault(name, "").toLowerCase(); // lowercase FOR PostGres
    this.type = StringX.TrivialDefault(type, "TABLE");//else view et al.
    this.remark = StringX.TrivialDefault(remark, "");
  }

  /**
   * know name and schema
   */
  public SimpleTableInfo(String schema, String name) {
    this(null, schema, name, null, null);
  }

  /**
   * just know table name
   */
  public SimpleTableInfo(String name) {
    this(null, null, name, null, null);
  }

  public void setSchema(String schema) {
    this.schema = schema;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String catalog() {
    return catalog;
  }

  @Override
  public String schema() {
    return schema;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  /** debug */
  public String toString() {
    return fullName();
  }
}
