package pers.hal42.database;

import org.jetbrains.annotations.NotNull;
import pers.hal42.lang.StringX;

import static java.text.MessageFormat.format;

/**
 extend name of a table, plus some other metadata.
 */

public class TableInfo implements Comparable<TableInfo> {
  //don't want to depend upon db connection features for these:
  public static String SharedCatalog;
  public static String SharedSchema;

  private String catalog;
  private String schema;

  public void setSchema(String schema) {
    this.schema = schema;
  }

  public void setName(String name) {
    this.name = name;
  }

  private String name;
  private String type;
  private String remark;

  public TableInfo(String catalog, String schema, String name, String type, String remark) {
    this.catalog = StringX.TrivialDefault(catalog, SharedCatalog);
    this.schema = StringX.TrivialDefault(schema, SharedSchema);
    this.name = StringX.TrivialDefault(name, "").toLowerCase(); // lowercase FOR PostGres
    this.type = StringX.TrivialDefault(type, "TABLE");//else view et al.
    this.remark = StringX.TrivialDefault(remark, "");
  }

  public TableInfo(String name) {
    this(null, null, name, null, null);
  }

  public String catalog() {
    return catalog;
  }

  public String schema() {
    return schema;
  }

  public String name() {
    return name;
  }

  public String fullName(){
    return format("{0}.{1}",schema,name);
  }
  public String type() {
    return type;
  }

  public String remark() {
    return remark;
  }

  @Override
  public int compareTo(@NotNull TableInfo o) {
    int diff=catalog.compareTo(o.catalog);
    if(diff==0){
      diff=schema.compareTo(o.schema);
      if(diff==0){
        diff=name.compareTo(o.name);
      }
    }
    return diff;
  }
}
