package pers.hal42.database;
import  pers.hal42.util.*;
import pers.hal42.lang.StringX;
/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2000
 * Company:      PayMate.net
 * @author PayMate.net
 * @version $Id: TableInfo.java,v 1.8 2003/08/27 02:20:11 mattm Exp $
 */

public class TableInfo implements Comparable {

  private String catalog;
  private String schema;
  private String name;
  private String type;
  private String remark;

  public TableInfo(String catalog, String schema, String name, String type, String remark) {
    this.catalog=StringX.TrivialDefault(catalog, "");
    this.schema=StringX.TrivialDefault(schema, "");
    this.name=StringX.TrivialDefault(name, "").toLowerCase(); // FOR PG!
    this.type=StringX.TrivialDefault(type, "");
    this.remark=StringX.TrivialDefault(remark, "");
  }

  public TableInfo(String name) {
    this("", "", name, "", "");
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
  public String type() {
    return type;
  }
  public String remark() {
    return remark;
  }

  public int compareTo(Object o) {
    TableInfo ti = (TableInfo)o;
    return name().compareTo(ti.name());
  }
}
