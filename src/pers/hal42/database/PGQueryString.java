package pers.hal42.database;


import pers.hal42.lang.Bool;
import pers.hal42.lang.ObjectX;
import pers.hal42.lang.StringX;

/* debris from temporarily abandoned TableProfile and UniqueId classes.
This was formerly part of querystring and has PostGres dependent stuff in it.
*
* */
public class PGQueryString extends QueryString {
  protected static final String TABLE = " TABLE ";
  protected static final String DROP = " DROP ";
  protected static final String DEFAULT = " DEFAULT ";
  protected static final String SEQ = "SEQ"; // PG:NO SPACES AROUND IT!
  private static final String ALTER = " ALTER ";
  protected static final String ALTERTABLE = ALTER + TABLE;
  private static final String RENAME = " RENAME ";
  private static final String DELETE = " DELETE ";
  private static final String DELETEFROM = DELETE + FROM;
  private static final String RENAMECOLUMN = RENAME + COLUMN;
  private static final String DROPCOLUMN = DROP + COLUMN;
  private static final String CREATE = " CREATE ";
  private static final String CREATETABLE = CREATE + TABLE;// + MAINSAIL;
  private static final String CONSTRAINT = " CONSTRAINT ";
  private static final String ADDCONSTRAINT = ADD + CONSTRAINT;
  private static final String KEY = " KEY ";
  private static final String PRIMARYKEY = " PRIMARY " + KEY;
  private static final String FOREIGNKEY = " FOREIGN " + KEY;
  private static final String REFERENCES = " REFERENCES ";
  private static final String INDEX = " INDEX ";
  private static final String CREATEINDEX = CREATE + INDEX;// + MAINSAIL;
  private static final String UNIQUE = " UNIQUE ";
  private static final String CREATEUNIQUEINDEX = CREATE + UNIQUE + INDEX;// + MAINSAIL;
  private static final String DROPTABLE = DROP + TABLE;
  private static final String DROPINDEX = DROP + INDEX;
  private static final String DROPCONSTRAINT = DROP + CONSTRAINT;
  private static final String OUTER = " OUTER ";
  private static final String VACUUM = " VACUUM ";
  private static final String ANALYZE = " ANALYZE ";
  private static final String FULL = " FULL ";
  private static final String VERBOSE = " VERBOSE ";
  private static final String EXPLAINANALYZE = EXPLAIN + ANALYZE; // PG only;

  public PGQueryString(String starter) {
    super(starter);
  }

  ///////////////////////////////

  //  public QueryString commaOuter(TableProfile tp){
//    return comma().cat(OUTER).cat(tp.name());
//  }

  public PGQueryString comma(UniqueId id) {
    if (UniqueId.isValid(id)) {
      comma().cat(id.value());
    } else {
      comma().cat(NULL);
    }
    return this;
  }

  public PGQueryString nvPair(ColumnProfile field, UniqueId value) {
    if (UniqueId.isValid(value)) {
      nvPair(field, value.value());
    } else {
      nvPairNull(field);
    }
    return this;
  }
//  public QueryString SetJust(ColumnProfile field, UniqueId id) {
//    return cat(SET).nvPair(field.name(), id); // do not change this until you read the above function !!!
//  }

//  public PGQueryString nvPair(String field, UniqueId value) {
//    return (PGQueryString) nvPair(field, value.value());
//  }
//
//  public static PGQueryString RenameColumn(String table, String from, String to) {
//    return (PGQueryString) new PGQueryString(ALTERTABLE).cat(table).
//      word(RENAMECOLUMN).word(from).cat(TO).word(to);
//  }
//
//  public static PGQueryString genAddField(ColumnProfile column) {
//    return AddColumn(column);
//  }

//  public static PGQueryString genCreateIndex(IndexProfile index) {
//    return PGQueryString.CreateIndex(index);
//  }
//  public static PGQueryString AddColumn(ColumnProfile column) {
//    return PGQueryString.AlterTable(column.table()).word(ADD).createFieldClause(column, true);
//  }
//
//
//  public static boolean isReadOnly(QueryStringBase qs) {
//    return qs == null || qs.isReadOnly();
//// read only since it cannot change the database if it is null!
//  }

  public PGQueryString all(TableProfile table) {
    return (PGQueryString) word(table.all());
  }

  public PGQueryString from(TableProfile first) {
    return (PGQueryString) word(FROM).word(first.name());
  }

  // +++ should do this by passing in both tables and making sure they both have that field?
  // LEFT OUTER JOIN ASSOCIATE USING (associateid)
  // this function extracts the table name from the column profile !!!
  public PGQueryString leftOuterJoinUsing(ColumnProfile column) {
    return (PGQueryString) cat(LEFT).cat(OUTER).cat(JOIN).cat(column.table().name()).using(column);
  }

  //  public QueryString fromOuter(TableProfile left, TableProfile right) {
//    return word(FROM).word(left.name()).word(OUTER).word(JOIN).word(right.name());
//  }

  public final PGQueryString DropNotNull() {
    return (PGQueryString) cat(DROP).not().cat(NULL);
  }

  public final PGQueryString SetNotNull() {
    return (PGQueryString) cat(SET).not().cat(NULL);
  }

  public final PGQueryString DropDefault() {
    return (PGQueryString) cat(DROP).cat(DEFAULT);
  }

  public final PGQueryString SetDefault(String def) {
    return (PGQueryString) cat(SET).cat(DEFAULT).cat(def); // +-+ test this
  }

  //  public static final QueryString StatisticsAndCleanup() {
//    return Vacuum(true, true, false);
//  }

//  public static PGQueryString SelectAllFrom(TableProfile table) {
//    return SelectAll().from(table);
//  }

//  public static PGQueryString Insert(TableProfile table) {
//    return new PGQueryString(INSERTINTO).cat(table.name());
//  }

//  public static PGQueryString Insert(TableProfile table, EasyProperties toInsert, UniqueId serialValue) {
//    return Insert(table, toInsert, serialValue, false /*valuesFromDefaults*/);
//  }

//  // PG only
//  // the format of the query to do so is: SELECT nextval('tablename_fieldname_seq')
//  public static PGQueryString SelectNextVal(ColumnProfile field) {
//    return Select().word(NEXTVAL).Open().value(field.table().name() + "_" + field.name() + "_" + SEQ).Close();
//  }
//
//  // Note that you have to get a serial field's next value to use to insert the record
//  static QueryString genSelectNextVal(ColumnProfile cp) {
//    return QueryString.SelectNextVal(cp);
//  }

//  static QueryString genMaxId(ColumnProfile pk) {
//    return PGQueryString.genid(pk.table()).orderbydesc(pk).limit(1);
//  }

//  public static PGQueryString Insert(TableProfile table, EasyProperties toInsert, UniqueId serialValue, boolean valuesFromDefaults) {
//    PGQueryString names = PGQueryString.Insert(table).Open();
//    QueryString values = QueryString.Clause().Close().Values();
//    QueryString end = QueryString.Clause().Close();
//    if (toInsert != null) {
//      // be sure that the primary key is included in the list of properties, if valid
//      ColumnProfile omitSerial;
//      if (table.primaryKey != null) { // HOWEVER, all tables need to have a primary key !!!
//        omitSerial = table.primaryKey.field;
//        if (UniqueId.isValid(serialValue)) {
//          // be sure that the field for the serial value is in the fieldlist
//          toInsert.setInt(omitSerial.name(), serialValue.value());
//        }
//      } else {
//        dbg.WARNING("Table " + table.name() + " does not have a primary key!  (bad implementation)");
//      }
//      // validate the property names with the fields to ensure insertion of at least SOME data?
//      boolean first = true;
//      for (Enumeration ennum = toInsert.propertyNames(); ennum.hasMoreElements(); ) {
//        String name = (String) ennum.nextElement();
//        ColumnProfile column = table.column(name);
//        if (column == null) {
//          dbg.ERROR("Insert(): field " + name + " not found in table " + table.name() + "!");
//        } else {
//          // prefix with commas if needed
//          if (first) {
//            first = false;
//          } else {
//            names.comma();
//            values.comma();
//          }
//          // add strings
//          names.cat(name);
//          String value = toInsert.getProperty(name);
//          dbg.VERBOSE("Property scan: " + name + "=" + value);
//          values.cat(valueByColumnType(column, value, !valuesFromDefaults /*forceQuotes*/));
//        }
//      }
//    }
//    return names.cat(values).cat(end);
//  }

//  private static PGQueryString Update(TableProfile table) {
//    return new PGQueryString(UPDATE).word(table.name()).cat(SET);
//  }
//
//  // call this, then .where()... to add a where clause.
//  // Just BE SURE to add the clause, or you will change ALL RECORDS!!!
//  public static PGQueryString Update(TableProfile table, EasyProperties toUpdate) {
//    PGQueryString qs = Update(table); // this makes UPDATE TABLE SET
//    // loop through the properties and set them
//    // validate the property names with the fields to ensure insertion of at least SOME data?
//    boolean first = true;
//    for (Enumeration ennum = toUpdate.propertyNames(); ennum.hasMoreElements(); ) {
//      String name = (String) ennum.nextElement();
//      ColumnProfile column = table.column(name);
//      if (column == null) {
//        dbg.ERROR("Update(): field " + name + " not found in table " + table.name() + "!");
//      } else {
//        // prefix with commas if needed
//        if (first) {
//          first = false;
//        } else {
//          qs.comma();
//        }
//        // find the value
//        String value = toUpdate.getProperty(name);
//        dbg.VERBOSE("Property scan: " + name + "=" + value);
//        // tack on the name=value
//        // NOTE! Do not use column.fullname!  *Might* cause problems with PG.
//        qs.cat(column.name()).cat(EQUALS).cat(valueByColumnType(column, value));
//      }
//    }
//    return qs;
//  }

  private static String valueByColumnType(ColumnProfile column, String prevalue) {
    return valueByColumnType(column, prevalue, true /*forceQuotes*/);
  }

  private static String valueByColumnType(ColumnProfile column, String prevalue, boolean forceQuotes) {
    if (prevalue == null) { // do NOT use !NonTrivial() here!  NULL is the check we are doing.
      return NULL; // do not quote a null, but instead use the word null
    } else {
      ColumnType type = column.getType();
      dbg.VERBOSE("ColumnTypes for " + column.name() + " is " + type);
      switch (type) {
      case SERIAL:
      case INTEGER: { // do not quote integer or serial !
        String postvalue = "";
        if (!StringX.NonTrivial(prevalue)) {
          // then we WANT null!
          if (!column.nullable()) {
            dbg.ERROR("PANIC! valueByColumnType(INT4) passed empty string, but NULL not allowed for column " + column.fullName() + "!");
            // then go ahead and use the value passed to us!
          }
          return NULL; // and pray that the field can handle a null!
        } else {
          int integer = StringX.parseInt(prevalue);
          if ((integer == ObjectX.INVALIDINDEX) && column.isProbablyId()) { // but can't really check integrity any better than this
            // --- if this column doesn't allow null, this will except, HOWEVER, we can't *guess* at a value!
            // so, in that case, we should send a panic!
            if (!column.nullable()) {
              dbg.ERROR("PANIC! valueByColumnType(INT4) passed '" + prevalue + "', but NULL not allowed for column " + column.fullName() + ", so using value verbatim");
              // then go ahead and use the value passed to us!
              return String.valueOf(integer);
            } else {
              return NULL; // and pray that the field can handle a null!
            }
          } else {
            // DO NOT let the database engine parse it.  It may not be as forgiving as our parser!
            return String.valueOf(integer);
          }
        }
      }
      // +++ put serial8 and int8 in here !!!!
      case BOOL: {
        return Bool.toString(Bool.For(prevalue)); // convert a short or long text to a long text; also converts "" to false
      }
      default: // using a quoted string for an unknown type is usually converted fine by the DBMS
//      case TEXT:
      case CHAR: { // but we shouldn't have any CHARs, except for "char", single char values
        if ("null".equalsIgnoreCase(prevalue.trim())) {
          // +++ what if null isn't allowed?  Let's have it put "" in that case
          if (column.nullable()) {
            return NULL;
          } else {
            return Quoted(""); // for cases where null is not valid
          }
        } else {
          if (!forceQuotes && prevalue.startsWith("'") && prevalue.endsWith("'")) { // don't add extra quotes if not needed
            return prevalue;
          }
          return Quoted(prevalue); // quote char or unknown type fields
        }
      }
      }
    }
  }

  // postgresql only
  public static PGQueryString Vacuum(TableProfile tp, boolean verbose, boolean analyze, boolean full) {
    return VacuumAnalyze(tp, verbose, analyze, full, true);
  }

  public static PGQueryString VacuumAnalyze(TableProfile tp, boolean verbose, boolean analyze, boolean full, boolean vacuum) {
    PGQueryString qs = new PGQueryString("");
    if (vacuum) {
      qs.cat(VACUUM);
      if (full) {
        qs.cat(FULL);
      }
    }
    if (analyze) {
      qs.cat(ANALYZE);
    }
    if ((vacuum || analyze) && verbose) {
      qs.cat(VERBOSE);
    }
    if ((tp != null) && StringX.NonTrivial(tp.name())) {
      qs.cat(tp.name());
    }
    return qs;
  }

//  public static PGQueryString AlterTable(TableProfile table) {
//    return new PGQueryString(ALTERTABLE).cat(table.name());
//  }
//
//  public static PGQueryString DropTable(TableProfile table) {
//    return new PGQueryString(DROPTABLE).cat(table.name());
//  }
//
//  public static PGQueryString DropIndex(IndexProfile index) {
//    return new PGQueryString(DROPINDEX).cat(index.name);
//  }
//
//  public static PGQueryString DropConstraint(TableProfile table, Constraint constr) {
//    return AlterTable(table).word(DROPCONSTRAINT).word(constr.name);
//  }
//
//  public static PGQueryString DeleteFrom(TableProfile table) {
//    PGQueryString qs = new PGQueryString(DELETEFROM);
//    return qs.cat(table.name()).cat(SPACE);
//  }
//
//  // testing these new things ...
//  public static PGQueryString generateTableCreate(TableProfile tp) {
//    PGQueryString qs = new PGQueryString(CREATETABLE).cat(tp.name());
//    if (tp.numColumns() > 1) {
//      qs.Open();
//    }
//    for (int i = 0; i < tp.numColumns(); i++) {
//      qs.createFieldClause(tp.column(i), false /* not an existing table */);
//      // on all but the last one, add a comma
//      if (!(i == (tp.numColumns() - 1))) {
//        qs.comma();
//      }
//    }
//    if (tp.numColumns() > 1) {
//      qs.Close();
//    }
//    return qs;
//  }
//
//  // PG:  ALTER TABLE TXN ADD CONSTRAINT txnpk PRIMARY KEY(TXNID)
//  public static PGQueryString addKeyConstraint(KeyProfile key) {
//    PGQueryString qs = AlterTable(key.table).cat(ADDCONSTRAINT);
//    String keytype = key.isPrimary() ? PRIMARYKEY : FOREIGNKEY;
//    qs.word(key.name).cat(keytype).parenth(key.field.name());
//    if (!key.isPrimary()) {
//      ForeignKeyProfile fk = (ForeignKeyProfile) key;
//      qs.cat(REFERENCES).cat(fk.referenceTable.name());
//    }
//    return qs;
//  }
//
//  public static PGQueryString CreateIndex(IndexProfile index) {
//    PGQueryString ret = index.unique ? new PGQueryString(CREATEUNIQUEINDEX) : new PGQueryString(CREATEINDEX);
//    ret.cat(index.name).cat(ON).cat(index.table.name()).parenth(index.columnNamesCommad());
//    if (index.whereclause != null) {
//      ret.cat(index.whereclause);
//    }
//    return ret;
//  }
//
//  public static PGQueryString ExplainAnalyze() {
//    return PGQueryString.Clause().cat(EXPLAINANALYZE);
//  }
//
//  public static PGQueryString ShowParameter(String paramname) {
//    return PGQueryString.Clause().cat(SHOW).cat(paramname);
//  }
//
//  public static PGQueryString TableStats(TableProfile table) {
//    return PGQueryString.Clause().cat("select a.relpages,a.reltuples," + "b.n_tup_ins,b.n_tup_upd,b.n_tup_del " + "from pg_class a, pg_stat_user_tables b where a.relfilenode=b.relid " + "and a.relname='" + table.name() + "'");
//  }
//
//  public static PGQueryString TablePages(TableProfile table) {
//    return PGQueryString.Select().cat("reltuples,relpages " + "from pg_class where relname='" + table.name() + "'");
//  }
//
//  public static PGQueryString DatabaseAge(String databasename) {
//    return PGQueryString.Select().cat("age(datfrozenxid) from pg_database where datname='" + databasename + "'");
//  }

//  public static PGQueryString genAddPrimaryKeyConstraint(PrimaryKeyProfile primaryKey) {
//    return PGQueryString.addKeyConstraint(primaryKey);
//  }
//
//  public static PGQueryString genAddForeignKeyConstraint(ForeignKeyProfile foreignKey) {
//    return PGQueryString.addKeyConstraint(foreignKey);
//  }
//
//  public static PGQueryString genDropConstraint(TableProfile table, Constraint constr) {
//    return PGQueryString.DropConstraint(table, constr);
//  }
//  /////////////////////
//  public static PGQueryString genDropField(ColumnProfile column) {
//    return PGQueryString.AlterTable(column.table()).dropColumn(column);
//  }
//
//  public static PGQueryString genDropIndex(String indexname) {
//    return PGQueryString.DropIndex(new IndexProfile(indexname, null, new ColumnProfile[0]));
//  }
//
//  public static PGQueryString genDropTable(String tablename) {
//    return PGQueryString.DropTable(TableProfile.create(new TableInfo(tablename), null, null));
//  }
//
//  public static PGQueryString genRenameColumn(String table, String oldname, String newname) {
//    return RenameColumn(table, oldname, newname);
//  }
//
//  public static PGQueryString genCreateTable(TableProfile tp) {
//    return PGQueryString.generateTableCreate(tp);
//  }
//
//  public final PGQueryString AlterColumn(ColumnProfile column) {
//    return cat(ALTER).word(column.name());
//  }
//
//  // column points to its table
//  public final PGQueryString dropColumn(ColumnProfile column) {
//    return word(DROPCOLUMN).cat(column.name());
//  }
//
//  static PGQueryString genSelectRecord(TableProfile tp, UniqueId id) {
//    return PGQueryString.SelectAllFrom(tp).where().nvPair(tp.primaryKey.field, id);
//  }
//
//  static PGQueryString genUpdateRecord(TableProfile tp, UniqueId id, EasyProperties ezp) {
//    return PGQueryString.Update(tp, ezp).where().nvPair(tp.primaryKey.field, id);
//  }
//
//  // fragment that can be used for all genid()s
//  public static PGQueryString genid(TableProfile tp) {
//    return PGQueryString.Select(tp.primaryKey.field).from(tp);
//  }
//
//  private static PGQueryString idDistinct(TableProfile tp) {
//    return PGQueryString.SelectDistinct(tp.primaryKey.field).from(tp);
//  }
//  public PGQueryString createFieldClause(ColumnProfile cp, boolean existingTable) {
//    PGQueryString qs = this;
//    qs.word(cp.name());
//    ColumnType dbt = ColumnType.valueOf(cp.type().toUpperCase());
//    if (cp.autoIncrement()) {
//      //noinspection StatementWithEmptyBody
//      if (dbt == ColumnType.SERIAL) {
//        // this is fine!
//      } else {
//        if (dbt == ColumnType.INTEGER || dbt == ColumnType.SMALLINT || dbt == ColumnType.TINYINT) {
//          dbt = ColumnType.SERIAL;
//        } else {
//          dbg.ERROR("Don't know how to autoincrement the datatype " + dbt + "!");
//        }
//      }
//    }
//    switch (dbt) {
//    default: {
//      dbg.ERROR("Data type not considered in createFieldClause(): " + dbt.toString() + "!");
//    }
//    case CHAR: {
//      if (cp.size() < 2) {
//        qs.cat("\"char\""); // the PG "char" type, which implements char(1) in the main table (faster, smaller)
//        break;
//      } // else  do like all the rest
//    }
//    case BOOL:
////    case DATETIME:
//    case SERIAL:
////    case TEXT:
//      // +++ DO MORE OF THESE!!!
//    case INTEGER: {
//      qs.cat(dbt.name());
//    }
//    break;
//    }
//    // default goes here
//    //noinspection StatementWithEmptyBody
//    if (!existingTable && StringX.NonTrivial(cp.columnDef)) {
//      qs.word(DEFAULT).word(cp.dbReadyColumnDef());
//    } else {
//      // do nothing
//      // can't add a default clause to an 'alter table add field' statement in PG
//      // also, if the default isn't defined, don't add a default clause
//    }
//
//    return qs;
//  }
//
//  public static PGQueryString genChangeFieldNullable(ColumnProfile field) {
//    PGQueryString qs = PGQueryString.AlterTable(field.table()).AlterColumn(field);
//    if (field.nullable()) {
//      qs.DropNotNull();
//    } else {
//      qs.SetNotNull();
//    }
//    return qs;
//  }
//
//  public static PGQueryString genChangeFieldDefault(ColumnProfile field) {
//    PGQueryString qs = PGQueryString.AlterTable(field.table()).AlterColumn(field);
//    if (StringX.NonTrivial(field.columnDef)) {
//      qs.SetDefault(field.dbReadyColumnDef()); // dbReadyColumnDef() quotes things that need to be quoted
//    } else {
//      qs.DropDefault();
//    }
//    return qs;
//  }
}
