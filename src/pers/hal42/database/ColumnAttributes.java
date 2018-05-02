package pers.hal42.database;

import pers.hal42.ext.Extremer;
import pers.hal42.lang.ArrayIterator;
import pers.hal42.lang.ReflectX;
import pers.hal42.lang.StringX;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.text.SimpleCsvIterator;
import pers.hal42.text.StringIterator;

import java.util.Iterator;

import static java.text.MessageFormat.format;
import static pers.hal42.database.MysqlQuirks.isTicked;
import static pers.hal42.lang.Index.BadIndex;
import static pers.hal42.lang.StringX.NonTrivial;

/**
 * see ColumnProfile, which has too much legacy cruft
 */
public class ColumnAttributes {
  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(ColumnAttributes.class);
  //DDL phrase
  private final static String AUTOSTAMP = " ON UPDATE CURRENT_TIMESTAMP";
  //DDL phrase
  private final static String BENOTNULL = " NOT NULL";
  private final static String EmptyString = "''";
  public boolean nullable = true;
  public ColumnType dataType = ColumnType.VARCHAR;
  public int size = BadIndex;//
  /** position in 'insert or update' statements, skips over automatic columns */
  public int ordinal = BadIndex; //sql bad index
  /** which slot of pk this column is, if 0 it is provisionally part of the pk, if BadIndex then not part of pk */
  public int pkordinal = BadIndex;//0== part of pk but not assigned position yet
  public String name;//local name, exclusive of table and schema
  public String defawlt;
  /** for foreign keys this is the foreign table name */
  public String more;
  public boolean autoIncrement = false;

  public ColumnAttributes(boolean nullable, ColumnType dataType, int size, String name, String defawlt, boolean autoIncrement) {
    this.nullable = nullable;
    this.dataType = dataType;
    this.size = size;
    this.name = name;
    this.defawlt = defawlt;
    this.autoIncrement = autoIncrement;
  }

  /** used by ost of the factories */
  private ColumnAttributes() {
    //dbg.FATAL("ColumnAttibutes null constructor called");
  }

  public ColumnAttributes(String column_name, String data_type) {
    this(column_name, ColumnType.forName(data_type));
  }

  public ColumnAttributes(String column_name, ColumnType data_type) {
    this(true, data_type, BadIndex, column_name, null, false);
  }

  /**
   * @returns pkordinal or ordinal, with no validation
   */
  public int ord(boolean pk) {
    if (pk) {
      return pkordinal;
    } else {
      return ordinal;
    }
  }

  /**
   * if this is true it should be left out of inserts and updates, it will be automatically filled in by database engine
   */
  public boolean isAuto() {
    return autoIncrement;
  }

  public ColumnAttributes setAuto(boolean autoIncrement) {
    this.autoIncrement = autoIncrement;
    return this;
  }

  public boolean isPkish() {
    return pkordinal >= 0;
  }

  public boolean isUpdateable() {
    return !isAuto() && !isPkish();
  }

  public boolean isInsertable() {
    return !isAuto();
  }

  public ColumnAttributes setDefawlt(String defawlt) {
    this.defawlt = defawlt;
    return this;
  }

  /**
   * only use for finding items, not for comparing them
   */
  @Override
  public int hashCode() {
    return name.hashCode();
  }

  /**
   * for comparing desired and actual attribugtes, ignore ordinal
   */
  @SuppressWarnings("SimplifiableIfStatement") //leave it stretched out so we can breakpoint on particular miscompares.
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    if (!ColumnAttributes.class.isAssignableFrom(o.getClass())) {
      return false;
    }
    ColumnAttributes that = (ColumnAttributes) o;
    if (!nullable && that.nullable) {
      dbg.ERROR("Column <{0}> should be not null but existing is nullable", name);
      //mysql apparently ignores our request for nullable if there is a default
    }
    if (size >= 0 && that.size >= 0 && size > that.size) {
      return false;
    }
    if (autoIncrement != that.autoIncrement) {
      return false;
    }
    if (!dataType.isLike(that.dataType)) {
      return false;
    }
    if (!name.equals(that.name)) {
      return false;
    }
    //don't compare null defaults, allow null to match anything
    //noinspection RedundantIfStatement
    if (defawlt == null || that.defawlt == null || defawlt.equals(that.defawlt)) {
      return true;
    } else {
      if (that.defawlt.equals("NULL")) {//parsing mysql DDL sometimes gives us an explicit reference to NULL.
        return !NonTrivial(defawlt);
      }
      return false;
    }
  }

  @Override
  public String toString() {
    return String.valueOf(name);
  }

  /**
   * @returns sql fragment for create table
   */
  public StringBuilder declaration() {
    StringBuilder piece = new StringBuilder(100);
    piece.append(format("{0} {1}", name, dataType.DeclarationText(size)));
    if (!nullable) {
      piece.append(BENOTNULL);
    }
    if (dataType == ColumnType.TIMESTAMP) {//enforce it being a true timestamp
      //todo:1 mysql coerced a nullable timestamp to not null default 0000
      if (autoIncrement) {//abuse of this flag
        piece.append(AUTOSTAMP);
      }
      if (autoIncrement || !nullable) {//legacy code, might remove if we improve Timestamp() factory.
        defawlt = "CURRENT_TIMESTAMP";
      }
    } else {
      if (autoIncrement) {
        piece.append(" AUTO_INCREMENT");
      }
    }
    if (defawlt != null) {//#trivial strings are a useful default
      piece.append(" DEFAULT ");
      piece.append(defawlt.length() == 0 ? EmptyString : defawlt);//caller must put in quotes when needed.
    }
    return piece;
  }

  /**
   * if not already set then set name from fieldname of this within @param parent
   *
   * @returns whether name is nonTrivial, not whether it was altered herein.
   */
  public boolean eponomize(Object parent) {
    if (NonTrivial(name)) {
      return true;
    }
    String myName = ReflectX.fieldName(parent, this);
    if (NonTrivial(myName)) {
      name = myName;
      return true;
    }
    return false;
  }

  /** @deprecated use an Index object instead of this. will delete soon. */
  public String makeIndex() {
    return format("INDEX {0}_idx ({0})", name);//INDEX optindexname opttype (namelist)
  }

  public ColumnAttributes bePk() {
    if (pkordinal < 0) {//guard against setting it again after ordination.
      pkordinal = 0;
    }
    return this;
  }

  public void setTrivialDefault() {
    if (!NonTrivial(defawlt)) {
      defawlt = dataType.isTextlike() ? "" : "0";
    }
  }

  /** a varchar big enough to contain the widest string associated with the @param given enum */
  public static ColumnAttributes Enumeration(Class<? extends Enum> eclass, boolean nullable) {
    Extremer<Integer> maxWidth = new Extremer<>();
    maxWidth.inspectArray(eclass.getEnumConstants(), symbol -> symbol.toString().length());
    return ColumnAttributes.Stringy(maxWidth.getExtremum(10), nullable);
    //not using Mysql enum mechanism, we are not ready to deal with changes to the enumeration.
  }

  public static ColumnAttributes fromDDL(String line) {
    if (line.endsWith(",")) {
      line = line.substring(0, line.length() - 1);
    }
    StringIterator words = new SimpleCsvIterator(false, ' ', line);
    String word = words.next();
    if (isTicked(word)) {//then it is a column definition
      ColumnAttributes noob = new ColumnAttributes();
      noob.name = StringX.removeParens(word);
      String typeinfo = words.next();
      int cutpoint = typeinfo.indexOf('(');
      if (cutpoint > 0) { // a size indicator is present
        noob.dataType = ColumnType.forName(typeinfo.substring(0, cutpoint));
        noob.size = StringX.parseInt(typeinfo.substring(1 + cutpoint));
      } else {
        noob.dataType = ColumnType.forName(typeinfo);
      }
      while (words.hasNext()) {
        word = words.next();
        switch (word.toUpperCase()) {//mysql is damnably inconsistent in its casing! guarding against them changing it in the future
        case "UNSIGNED":
          break;
        case "DEFAULT":
          StringBuilder next = new StringBuilder(words.next());
          if (next.toString().startsWith("'")) {
            while (words.hasNext() && !next.toString().endsWith("'")) {
              next.append(" ").append(words.next());
            }
          }
          noob.defawlt = MysqlQuirks.unTick(next.toString());
          //todo: enable csviterator quoting logicremerge quoted words
          break;
        case "AUTO_INCREMENT":
          noob.autoIncrement = true;
          break;
        case "NOT":
          word = words.next();
          if (word.equals("NULL")) {
            noob.nullable = false;
          } else {
            dbg.FATAL("NOT {0} not understood", word);
          }
          break;
        case "NULL":
          noob.nullable = true;
          break;
        case "ON": //
          word = words.next();
          if (word.equals("UPDATE")) {
            word = words.next();
            if (word.equals("CURRENT_TIMESTAMP")) {
              noob.autoIncrement = true; //house rule: mark 'on update' fields as auto.
            } else {
              dbg.FATAL("ON UPDATE {0} not understood in {1}", word, line);
            }
          } else {
            dbg.FATAL("ON {0} not understood in {1}", word, line);
          }
          break;
        default:
          dbg.FATAL("ddl {0} not understood in {1}", word, line);
          break;
        }
      }
      return noob;
    } else {
      return null;
    }
  }

  public static ColumnAttributes Stringy(int size, boolean nullable) {
    ColumnAttributes noob = new ColumnAttributes();
    noob.size = size;
    noob.nullable = nullable;
    if (!nullable) {
      noob.defawlt = "";//empty is not the same as null
    }
    return noob;
  }

  /**
   * a numerical value of uncertain precision/range.
   */
  public static ColumnAttributes Number(boolean nullable) {
    ColumnAttributes noob = new ColumnAttributes();
    noob.dataType = ColumnType.DOUBLE;//fedfis house rule, need to analyze each and make them a decimal(,) declaration.
    noob.nullable = nullable;
    if (!nullable) {
      noob.defawlt = "0";
    }
    return noob;
  }

  public static ColumnAttributes Boolean(boolean nullable) {
    ColumnAttributes noob = new ColumnAttributes();
    noob.dataType = ColumnType.BOOL;
    noob.nullable = nullable;
    return noob;
  }

  /**
   * a proper 'internal' pk that allows user init.
   */
  public static ColumnAttributes SimplePK() {
    ColumnAttributes noob = new ColumnAttributes();
    noob.dataType = ColumnType.REF;
    noob.nullable = false;
    noob.bePk();
    return noob;
  }

  /**
   * a proper 'internal' pk that is auto increment only
   */
  public static ColumnAttributes StrictPK() {
    ColumnAttributes noob = new ColumnAttributes();
    noob.dataType = ColumnType.REF;
    noob.nullable = false;
    noob.autoIncrement = true;
    noob.bePk();
    return noob;
  }

  /**
   * an integer, such as for a quantity
   */
  public static ColumnAttributes ForeignKey(String foreignTableName, boolean nullable) {
    ColumnAttributes noob = new ColumnAttributes();
    noob.dataType = ColumnType.REF;  //INT UNSIGNED
    noob.nullable = nullable;
    noob.more = foreignTableName;
    return noob;
  }

  /**
   * an integer, such as for a quantity
   */
  public static ColumnAttributes Cardinal(boolean nullable) {
    ColumnAttributes noob = new ColumnAttributes();
    noob.dataType = ColumnType.INTEGER;
    noob.nullable = nullable;
    return noob;
  }

  /**
   * a nonnegative integer, such as for an enum, one expects the data to be dense.
   */
  public static ColumnAttributes Ordinal(boolean nullable) {
    ColumnAttributes noob = new ColumnAttributes();
    noob.dataType = ColumnType.SMALLINT;
    noob.nullable = nullable;
    return noob;
  }

  public static ColumnAttributes SingleLetterEnum(boolean nullable) {
    ColumnAttributes noob = new ColumnAttributes();
    noob.dataType = ColumnType.CHAR;
    noob.nullable = nullable;
    return noob;
  }

  public static ColumnAttributes Blob() {
    ColumnAttributes noob = new ColumnAttributes();
    noob.dataType = ColumnType.BLOB;
    noob.nullable = true;//house rule: never require blobs to exist, it gets in the way of changing our mind to keep them outside the tables.
    return noob;
  }

  /**
   * @param track is whether to alter this field with every operation else it is a creation date.
   */
  public static ColumnAttributes Timestamp(boolean track) {
    ColumnAttributes noob = new ColumnAttributes();
    noob.dataType = ColumnType.TIMESTAMP;
    noob.nullable = !track;
    noob.autoIncrement = track;//field is misnamed, this makes sense.
    return noob;
  }

  public ColumnAttributes setName(String name) {
    this.name = name;
    return this;
  }

  public static class Ordinator {
    int ordinator = 0;

    /**
     * add index of where column will appear in standard queries @returns whether column appears in standard select and insert/update lists
     */
    public boolean ordinate(ColumnAttributes col) {
      if (col.isAuto()) {
        col.ordinal = 0;
        return false;
      } else {
        col.ordinal = ++ordinator;
        return true;
      }
    }

    /**
     * re-enumerate pk @returns whether col is a pk entry
     */
    public boolean pkOrd(ColumnAttributes col) {
      if (col.pkordinal >= 0) {//0 marks it is a pk, but that its position is unknown, this will be ~0 for non-pk columns
        col.pkordinal = ++ordinator;
        return true;
      } else {
        return false;
      }
    }
  }

  /** provides a String iterator wrapper for columnattributes */
  public static class NameIterator implements Iterator<String> {
    Iterator<ColumnAttributes> cols;

    public NameIterator(Iterator<ColumnAttributes> cols) {
      this.cols = cols;
    }

    public NameIterator(ColumnAttributes[] cols) {
      this(new ArrayIterator<>(cols));
    }

    @Override
    public boolean hasNext() {
      return cols.hasNext();
    }

    @Override
    public String next() {
      return cols.next().name;
    }
  }
}
