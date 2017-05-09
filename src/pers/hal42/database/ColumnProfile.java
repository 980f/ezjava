package pers.hal42.database;
/**  Contains the Column information used by database profiling, etc.
 * This was created against a Postgres database, might need some fields dropped for mysql (or annotated as not relevant) */

import pers.hal42.lang.ObjectX;
import pers.hal42.lang.StringX;
import pers.hal42.logging.ErrorLogStream;

public class ColumnProfile implements Comparable {
  protected static final ErrorLogStream dbg = ErrorLogStream.getForClass(ColumnProfile.class);

  // database metadata stuff:
  public String tableCat = "";
  public String tableSchem = "";
  private TableProfile table = null;
  private String columnName = "";
  private ColumnTypes type;
  private String columnSize = "";
  private int size = -1; //todo:1 enforce restrictions?
  public String decimalDigits = "";
  public String numPrecRadix = "";
  public String nullAble = "";
  public String remarks = "";
  public String columnDef = "";
  public String charOctetLength = "";
  public String ordinalPosition = "";
  private String isNullable = "";
  private boolean nullable = false;

  private String displayName = ""; // for what goes at the top of a column when displaying via webpages, etc
  private boolean autoIncrement = false;

  //////////////////////////
  private ColumnProfile() {
    //use create
  }

  /**
   * returns correctenss of strucutre, not whether it is a member of the enclosing database.
   */
  public boolean isViable() {
    return StringX.NonTrivial(columnName); //&&type in typelist&&nullable is valid ...
  }

  public String name() {
    return columnName;
  }

  public String type() {
    return type.name();
  }

  public ColumnTypes numericType() {
    return type;
  }

  public boolean nullable() {
    return nullable;
  }

  public int size() {
    return size;
  }

  public String displayName() {
    return displayName;
  }

  public boolean autoIncrement() {
    return autoIncrement;
  }

  public TableProfile table() {
    return table;
  }

  public String fullName() {
    return ObjectX.NonTrivial(table) ? table.name() + '.' + name() : name();
  }

  public static final int     TEXTLEN = 0;//ObjectX.INVALIDINDEX;
  public static final int     INT4LEN = 4; // duh!
  public static final int     CHARLEN = 1; // special PG datatype that is one char
  public static final int     BOOLLEN = 1; // assume one byte

  public static final boolean ALLOWNULL=true;//strange that this was missing
  public static final boolean NOAUTO=false;

  private static int sizeForType(ColumnTypes type) {
    int ret = 0;
    switch(type) {
      case BOOL: {
        ret = BOOLLEN;
      } break;
      case TEXT: {
        ret = TEXTLEN;
      } break;
      case INT4: {
        ret = INT4LEN;
      } break;
      case CHAR: {
        ret = CHARLEN;
      } break;
    }
    return ret;
  }

  // creator for code-based profiles
  public static ColumnProfile create(TableProfile table, String name,
                                     ColumnTypes dbtype, int size,
                                     boolean nullable, String displayName,
                                     boolean autoIncrement,
                                     String columnDef) {
    name = StringX.TrivialDefault(name, "").toLowerCase();
    ColumnProfile cp = new ColumnProfile();
    cp.table = table;
    cp.columnName = name;
    cp.type = dbtype;
    dbg.VERBOSE("Just set column " + cp.fullName() + "'s type = " + cp.type.toString() + " for dbtype = " + dbtype);
    cp.size = size > 0 ? size : sizeForType(cp.type);
    cp.nullable = nullable;
    cp.displayName = StringX.OnTrivial(displayName, name);
    cp.autoIncrement = autoIncrement;
    // the following needs to happen in the code that creates the column in the db
    // and the code that checks the default to see if it has changed,
    // and NOT here!
    cp.columnDef = /*(cp.type.is(ColumnTypes.TEXT) ||
                    cp.type.is(ColumnTypes.CHAR)) ?
        StringX.singleQuoteEscape(columnDef) : // quote it*/
        columnDef; // otherwise don't
    return cp;
  }

  // creator for existing tables
  public static ColumnProfile create(TableProfile table, String name, String type, String size, String nullable) {
    return create(table, name, (ColumnTypes.valueOf(type)), StringX.parseInt(size), nullable.equalsIgnoreCase("YES"), null, false,"");
  }

  public int compareTo(Object o) {
    int i = 0;
    if (ObjectX.NonTrivial(o)) {
      try {
        ColumnProfile cp = (ColumnProfile) o;
        i = name().compareTo(cp.name());
        if (i == 0) {
          return table.compareTo(cp.table);
        }
      }
      catch (Exception e) {
        dbg.ERROR("Compared different types!");
      }
      return i;
    }
    else {
      return 1; // this is bigger than null
    }
  }

  public boolean is(ColumnProfile other) {
    return (this.compareTo(other) == 0);
  }

  public final String toString() {
    return name();
  }

  public String dbReadyColumnDef() {
    return dbReadyColumnDef(type, columnDef);
  }

  public static String dbReadyColumnDef(ColumnTypes type, String rawDefault) {
    rawDefault = StringX.TrivialDefault(rawDefault);
    return type.isTextlike() ? StringX.singleQuoteEscape(rawDefault) : rawDefault;
  }

  public void setDefaultFromDB(String defaultFromDB) {
    columnDef = dbUnReadyColumnDef(type, defaultFromDB);
  }

  public static String dbUnReadyColumnDef(ColumnTypes type, String defaultFromDB) {
    if(StringX.NonTrivial(defaultFromDB)) {
      if(type.isTextlike()) {
        return StringX.unSingleQuoteEscape(defaultFromDB);
      } else {
        return defaultFromDB;
      }
    } else {
      return "";
    }
  }

  // for them to have the same name, the names must match (sans case)
  public boolean sameNameAs(ColumnProfile other) {
    return StringX.equalStrings(name(), other.name(), true);
  }

  public boolean sameTypeAs(ColumnProfile other) {
    return this.type==other.type;
  }

  public boolean sameNullableAs(ColumnProfile other) {
    return nullable() == other.nullable();
  }

  public boolean sameDefaultAs(ColumnProfile other) {
    return StringX.equalStrings(columnDef, other.columnDef);
  }

  public boolean isProbablyId() {
    return StringX.equalStrings(StringX.right(name(), 2), "ID", /* ignore case */ true);
  }
}
//$Id: ColumnProfile.java,v 1.28 2003/08/20 18:22:29 mattm Exp $
