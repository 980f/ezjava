package pers.hal42.database;

import org.jetbrains.annotations.NotNull;
import pers.hal42.lang.ObjectX;
import pers.hal42.lang.StringX;
import pers.hal42.logging.ErrorLogStream;

/**
 * Contains the Column information used by database profiling, etc.
 * This was created against a Postgres database, might need some fields dropped for mysql (or annotated as not relevant)
 */
public class ColumnProfile implements Comparable<ColumnProfile> {
  // database metadata stuff:
  public String tableCat = "";
  public String tableSchem = "";
  public String decimalDigits = "";
  public String numPrecRadix = "";
  public String nullAble = "";
  public String remarks = "";
  public String columnDef = "";
  public String charOctetLength = "";
  public String ordinalPosition = "";
  private TableProfile table = null;
  private String columnName = "";
  private ColumnType type;
  private String columnSize = "";
  private int size = -1; //todo:1 enforce restrictions?
  private String isNullable = "";
  private boolean nullable = false;
  private String displayName = ""; // for what goes at the top of a column when displaying via webpages, etc
  private boolean autoIncrement = false;
  public static final int TEXTLEN = 0;//ObjectX.INVALIDINDEX;
  public static final int INT4LEN = 4; // duh!
  public static final int CHARLEN = 1; // special PG datatype that is one char
  public static final int BOOLLEN = 1; // assume one byte
  public static final boolean ALLOWNULL = true;//strange that this was missing
  public static final boolean NOAUTO = false;
  protected static final ErrorLogStream dbg = ErrorLogStream.getForClass(ColumnProfile.class);

  //////////////////////////
  private ColumnProfile() {
    //use create
  }

  public TableInfo tq() {
    return table.tq();
  }

  /**
   * returns correctness of structure, not whether it is a member of the enclosing database.
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

  public ColumnType numericType() {
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

  public int compareTo(@NotNull ColumnProfile o) {
    if (ObjectX.NonTrivial(o)) {
      try {
        int i = name().compareTo(o.name());
        if (i == 0) {
          return table.compareTo(o.table);
        }
        return i;
      } catch (Exception e) {
        dbg.ERROR("Compared different types!");
        return 0;//not our problem to deal with bad code. 0 is the only value that meets commutative requirement
      }
    } else {
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

  public void setDefaultFromDB(String defaultFromDB) {
    columnDef = dbUnReadyColumnDef(type, defaultFromDB);
  }

  // for them to have the same name, the names must match (sans case)
  public boolean sameNameAs(ColumnProfile other) {
    return StringX.equalStrings(name(), other.name(), true);
  }

  public boolean sameTypeAs(ColumnProfile other) {
    return this.type == other.type;
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

  private static int sizeForType(ColumnType type) {//todo:1 push into the enum.
    switch (type) {
      case BOOL:
        return BOOLLEN;
      case TEXT:
        return TEXTLEN;
    case INTEGER:
        return INT4LEN;
      case CHAR:
        return CHARLEN;
      default:
        return 0;
    }
  }

  // creator for code-based profiles
  public static ColumnProfile create(TableProfile table, String name, ColumnType dbtype, int size, boolean nullable, String displayName, boolean autoIncrement, String columnDef) {
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
    return create(table, name, (ColumnType.valueOf(type)), StringX.parseInt(size), nullable.equalsIgnoreCase("YES"), null, false, "");
  }

  public static String dbReadyColumnDef(ColumnType type, String rawDefault) {
    rawDefault = StringX.TrivialDefault(rawDefault);
    return type.isTextlike() ? StringX.singleQuoteEscape(rawDefault) : rawDefault;
  }

  public static String dbUnReadyColumnDef(ColumnType type, String defaultFromDB) {
    if (StringX.NonTrivial(defaultFromDB)) {
      if (type.isTextlike()) {
        return StringX.unSingleQuoteEscape(defaultFromDB);
      } else {
        return defaultFromDB;
      }
    } else {
      return "";
    }
  }
}
