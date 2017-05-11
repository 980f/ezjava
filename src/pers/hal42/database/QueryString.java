package pers.hal42.database;


import pers.hal42.data.ObjectRange;
import pers.hal42.lang.Bool;
import pers.hal42.lang.ObjectX;
import pers.hal42.lang.StringX;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.text.TextList;
import pers.hal42.transport.EasyProperties;

import java.util.Enumeration;

/* If your database needs special syntax then you will have to implement an extended class AND make sure you use that class instead of this base.
*
* */
public class QueryString {
  public static final ErrorLogStream dbg = ErrorLogStream.getForClass(QueryString.class);

  ///////////////////////////////////////
  // Keywords:
  private static final String SELECT = " SELECT ";
  private static final String AND = " AND ";
  private static final String NOT = " NOT ";
  private static final String WHERE = " WHERE ";
  private static final String FROM = " FROM ";
  private static final String UPDATE = " UPDATE ";
  private static final String SET = " SET ";
  private static final String ORDERBY = " ORDER BY ";
  private static final String GROUPBY = " GROUP BY ";
  private static final String ASC = " ASC ";
  private static final String DESC = " DESC ";
  private static final String UNION = " UNION ";
  private static final String VALUES = " VALUES ";
  private static final String INSERTINTO = " INSERT INTO ";
  private static final String DELETE = " DELETE ";
  private static final String DELETEFROM = DELETE + FROM;
  private static final String IN = " IN ";
  private static final String ON = " ON ";
  private static final String ALLFIELDS = " * ";
  private static final String SELECTALL = SELECT + ALLFIELDS;
  private static final String OPENPAREN = " (";
  private static final String CLOSEPAREN = ") ";
  private static final String OR = " OR ";
  private static final String CREATE = " CREATE ";
  private static final String TABLE = " TABLE ";
  private static final String CREATETABLE = CREATE +  TABLE;// + MAINSAIL;
  private static final String COMMA = " , ";
  private static final String SPACE = " ";
  private static final String EQUALS = " = ";
  private static final String GTEQ = " >= ";
  private static final String GT = " > ";
  private static final String LTEQ = " <= ";
  private static final String LT = " < ";
  private static final String ADD = " ADD ";
  private static final String CONSTRAINT = " CONSTRAINT ";
  private static final String ADDCONSTRAINT = ADD+CONSTRAINT;
  private static final String KEY = " KEY ";
  private static final String PRIMARYKEY = " PRIMARY " + KEY;
  private static final String FOREIGNKEY = " FOREIGN " + KEY;
  private static final String REFERENCES = " REFERENCES ";
  private static final String EMPTY = "";
  private static final String INDEX = " INDEX ";
  private static final String CREATEINDEX = CREATE + INDEX;// + MAINSAIL;
  private static final String UNIQUE = " UNIQUE ";
  private static final String CREATEUNIQUEINDEX = CREATE + UNIQUE + INDEX;// + MAINSAIL;
  private static final String IS = " IS ";
  private static final String NULL = " null ";
  private static final String ISNULL = IS + NULL; // no space since each has a space before and after
  private static final String DISTINCT = " DISTINCT ";
  private static final String ALTER = " ALTER ";
  private static final String ALTERTABLE = ALTER + TABLE;
  private static final String RENAME = " RENAME ";
  private static final String COLUMN = " COLUMN ";
  private static final String RENAMECOLUMN = RENAME + COLUMN;
  private static final String DROP = " DROP ";
  private static final String DROPTABLE = DROP + TABLE;
  private static final String DROPINDEX = DROP + INDEX;
  private static final String DROPCOLUMN = DROP + COLUMN;
  private static final String DROPCONSTRAINT = DROP + CONSTRAINT;
  private static final String SUM = " SUM ";
  private static final String COUNT = " COUNT ";
  private static final String MAX = " MAX ";
  private static final String MIN = " MIN ";
  private static final String AS = " AS ";
  private static final String HAVING = " HAVING ";
  private static final String DEFAULT = " DEFAULT ";
  private static final String OUTER = " OUTER ";
//  private static final String COMMAOUTER = COMMA + OUTER;
  private static final String SUBSTRING = " SUBSTRING ";
  private static final String TO = " TO ";
  private static final String FOR = " FOR ";
  private static final String CASE = " CASE ";
  private static final String END = " END ";
  private static final String ELSE = " ELSE ";
  private static final String THEN = " THEN ";
  private static final String WHEN = " WHEN ";
  private static final String JOIN = " JOIN ";
  private static final String LEFT = " LEFT ";
  private static final String USING = " USING ";
  private static final String CAST = " CAST ";
  private static final String BIGINT = " BIGINT ";
//added for postgres:
  private static final String LIMIT = " LIMIT ";
  private static final String VACUUM = " VACUUM ";
  private static final String ANALYZE = " ANALYZE ";
  private static final String FULL = " FULL ";
  private static final String VERBOSE = " VERBOSE ";
  private static final String NEXTVAL = " NEXTVAL ";
  private static final String SEQ = "SEQ"; // PG:NO SPACES AROUND IT!
  private static final String SHOW = " SHOW ";
  private static final String EXPLAIN = " EXPLAIN ";
  private static final String EXPLAINANALYZE = EXPLAIN + ANALYZE; // PG only;


  protected StringBuffer guts= new StringBuffer(200);//most queries are big

  public String toString(){//works with String +
    return String.valueOf(guts);
  }

  public QueryString cat(QueryString s){
    cat(SPACE).cat(String.valueOf(s));
    return this;
  }

  public QueryString cat(String s){
    guts.append(s);
    return this;
  }

  public QueryString cat(char c){
    guts.append(c);
    return this;
  }

  public QueryString cat(int s){
    return cat((long)s);
  }

  public QueryString cat(long s){
    guts.append(s);
    return this;
  }

  public QueryString where(){
    return cat(WHERE);
  }

  public QueryString and(){
    return cat(AND);
  }

  public QueryString or(){
    return cat(OR);
  }

  public QueryString not(){
    return cat(NOT);
  }

  private QueryString in(String list){
    return word(IN).Open().cat(list).Close();
  }
  public QueryString inQuoted(TextList list){
    // have to do this list manually, as quoting has some special rules ...
    // create a new list with the quoted strings in it, then cat them into a StringBuffer and output as a string.
    // This would be faster if it was directly into a StringBuffer, but I am in a hurry right now.
    TextList list2 = new TextList(list.size());
    for(int i = 0; i < list.size(); i++) {
      list2.add(Quoted(list.itemAt(i)));
    }
    return inUnquoted(list2);
  }

  public QueryString inUnquoted(TextList list){
    return in(list.asParagraph(","));
  }

  public QueryString inUnquoted(ColumnProfile cp, TextList list){
    return cat(cp.fullName()).inUnquoted(list);
  }

  // apparently only used in genDupCheck()
  public QueryString substring(ColumnProfile cp, int from, int For) {
    return cat(SUBSTRING).Open(cp).cat(FROM).cat(from).cat(FOR).cat(For).Close();
  }

  public QueryString sum(ColumnProfile cp) {
    return sum(cp.fullName());
  }
  public QueryString sum(QueryString qs) {
    return sum(String.valueOf(qs));
  }
  private QueryString sum(String str) {
    return word(SUM).Open().cat(str).Close();
  }

  public QueryString count(ColumnProfile cp) {
    return count(cp.fullName());
  }

  public QueryString count(String it) {
    return word(COUNT).Open().cat(it).Close();
  }

  public QueryString max(ColumnProfile cp) {
    return max(cp.fullName());
  }

  public QueryString max(String of) {
    return word(MAX).Open().word(of).Close();
  }

  public QueryString min(ColumnProfile cp) {
    return min(cp.fullName());
  }

  public QueryString min(String of) {
    return word(MIN).Open().word(of).Close();
  }

  public QueryString as(String newname) {
    return word(AS).word(newname);
  }

//  CASE a WHEN 1 THEN 'one'
//         WHEN 2 THEN 'two'
//         ELSE 'other'
//  END
  public QueryString Decode(String field, String comparedTo, String ifEquals, String ifNot) {
//    return word(DECODE).Open().cat(field).comma().cat(comparedTo).comma().cat(ifEquals).comma().cat(ifNot).Close();
    return word(CASE).cat(field).word(WHEN).cat(comparedTo).word(THEN).cat(ifEquals).word(ELSE).cat(ifNot).word(END);
  }

  /**
   * "AND (fi
   */
  public QueryString AndInList(ColumnProfile field,TextList options){
    if((options!=null)&&(field!=null)){
      switch (options.size()) {
        case 0://make no changes whatsoever
          break;
        case 1://a simple compare
          return nvPair(field,options.itemAt(0));
        default://2 or more must be or'd together or use "IN" syntax %%%

          break;
      }
    }
    return this;
  }

  private QueryString SetJust(ColumnProfile field, String val){
    // do NOT use nvPair(field,val) here, as it puts a table. prefix on the fieldname
    // eg: table.field=value.  This FAILS in PG!
    // instead, use the following:
    return cat(SET).nvPair(field.name(), val);
  }

  private QueryString SetJust(ColumnProfile field, UniqueId id){
    return cat(SET).nvPair(field.name(),id); // do not change this until you read the above function !!!
  }

  private QueryString SetJust(ColumnProfile field, int val){//wishing for templates....
    return cat(SET).nvPair(field.name(), val); // do not change this until you read the above function !!!
  }

  private QueryString SetJust(ColumnProfile field, Long val){//wishing for templates....
    return cat(SET).nvPair(field.name(), val); // do not change this until you read the above function !!!
  }

  public QueryString Values(String first){
    return Values().value(first);
  }

  public QueryString Values(){
    return cat(VALUES+OPENPAREN);
  }

  public QueryString Open(){
    return cat(OPENPAREN);
  }

  public QueryString Open(ColumnProfile cp){
    return Open().cat(cp.fullName());
  }

  public QueryString Close(){
    return cat(CLOSEPAREN);
  }

  public QueryString comma(){
    return cat(COMMA);
  }

  public QueryString comma(ColumnProfile cp){
    return comma().cat(cp.fullName());
  }

  public QueryString comma(TableProfile tp){
    return comma().cat(tp.name());
  }
//  public QueryString commaOuter(TableProfile tp){
//    return comma().cat(OUTER).cat(tp.name());
//  }

  public QueryString comma(QueryString qs){
    return comma().cat(qs);
  }

  public QueryString comma(int s){
    return comma().cat(s);
  }

  public QueryString comma(UniqueId id){
    if(UniqueId.isValid(id)) {
      return comma().cat(id.value());
    } else {
      return comma().cat(NULL);
    }
  }

  private QueryString word(String s){
    return cat(SPACE).cat(s).cat(SPACE);
  }

  public QueryString parenth(String s){
    return cat(SPACE).Open().cat(s).Close().cat(SPACE);
  }

  public QueryString parenth(QueryString s){
    return parenth(String.valueOf(s));
  }

  public QueryString value(String s){
    return cat(Quoted(s));
  }

  public QueryString value(char c){
    return cat(Quoted(c));
  }

  public QueryString value(long ell){
    return cat(/*Quoted(*/ell/*)*/); // DO NOT quote this; it can cause the database txn to be very slow, as it has to convert its type before EACH comparison!
  }

/*
   Rules:
   1) If it contains any single quotes, escape them with another single quote
   2) Wrap it in single quotes,
*/
  public static String Quoted(String s){
    String ret = StringX.singleQuoteEscape(s);
    dbg.VERBOSE("Quoted():" +s+ "->" +ret);
    return ret;
  }
  /**
  * SQL-92 requires that all strings are inside single quotes.
  * Double quotation marks delimit special identifiers referred to in SQL-92
  * as 'delimited identifiers'.  Single quotation marks delimit character strings.
  * Within a character string, to represent a single quotation mark or
  * apostrophe, use two single quotation marks. To represent a double
  * quotation mark, use a double quotation mark
  * (which requires a backslash escape character within a Java program).
  * For example, the string
  *   "this " is a double quote"
  * is not valid.  This one is:
  *   'this " is a double quote'
  * And so is this one:
  *   "this "" is a double quote"
  * And this one:
  *   'this '' is a single quote'
  */


  public static String Quoted(char c){
    return Quoted(String.valueOf(c));
  }
  public static String Quoted(long ell){
    return Quoted(String.valueOf(ell));
  }

  public QueryString quoted(char c){
    return cat(SPACE).value(c);
  }

  public QueryString commaQuoted(String s){
    return cat(COMMA).value(s);
  }

  public QueryString commaQuoted(char c){
    return cat(COMMA).value(c);
  }

  public QueryString orderbyasc(ColumnProfile first){
    return cat(ORDERBY).cat(first.fullName()).cat(ASC); // --- possible cause of bugs
  }

  public QueryString orderbydesc(ColumnProfile first){
    return cat(ORDERBY).cat(first.fullName()).cat(DESC); // --- possible cause of bugs
  }

  public QueryString orderbydesc(int columnFirst){
    return cat(ORDERBY).cat(String.valueOf(columnFirst)).cat(DESC);
  }

  public QueryString orderbyasc(int columnFirst){
    return cat(ORDERBY).cat(String.valueOf(columnFirst)).cat(ASC);
  }

  public QueryString commaAsc(ColumnProfile next) {
    return cat(COMMA).cat(next.fullName()).cat(ASC); // --- possible cause of bugs
  }

  public QueryString commaDesc(ColumnProfile next) {
    return cat(COMMA).cat(next.fullName()).cat(DESC); // --- possible cause of bugs
  }

  public QueryString groupby(ColumnProfile first) {
    return cat(GROUPBY).cat(first.fullName()); // --- possible cause of bugs
  }

  public QueryString having() {
    return word(HAVING);
  }

  /**
   * Use one function that takes an object and does intelligent things with it instead of all of these ???
   */
  public QueryString nvPairNull(ColumnProfile field){
      return word(field.fullName()).cat(EQUALS).cat(NULL);
  }

  public QueryString nvPair(ColumnProfile field, String value){
      return nvPair(field.fullName(), value);
  }

  public QueryString nvPair(ColumnProfile field, int value){
      return nvPair(field.fullName(), value);
  }

  public QueryString nvPair(ColumnProfile field, long value){
      return nvPair(field.fullName(), value);
  }

  public QueryString nvPair(ColumnProfile field, Long value){
      return nvPair(field.fullName(), value); // --- just made this use the full name so that won't get the "ambiguous column" bug
  }

  public QueryString nvPair(String field, String value){
    return word(field).cat(EQUALS).value(value);
  }

  public QueryString nvPair(ColumnProfile field, UniqueId value){
    if(UniqueId.isValid(value)) {
      return nvPair(field, value.value());
    } else {
      return nvPairNull(field);
    }
  }

  public QueryString nvPair(String field, UniqueId value){
    return nvPair(field, value.value());
  }

  public QueryString nvPair(String field, int value){
      return nvPair(field, (long) value);
  }

  public QueryString nvPair(String field, long value){
      return word(field).cat(EQUALS).value(value);
  }

  public QueryString nvPair(String field, Long value){
      return word(field).cat(EQUALS).cat((value == null) ? NULL : String.valueOf(value));
  }

////////////////////////////
// compare field to value.

  public QueryString nvcompare(ColumnProfile field,String cmpop, String value){
    return word(field.fullName()).cat(cmpop).value(value);
  }
  public QueryString nvcompare(ColumnProfile field,String cmpop, long value){
    return word(field.fullName()).cat(cmpop).value(value);
  }

  public QueryString nGTEQv(ColumnProfile field, String value){
    return nvcompare(field,GTEQ,value);
  }
  public QueryString nGTEQv(ColumnProfile field, long value){
    return nvcompare(field,GTEQ,value);
  }

  public QueryString nLTEQv(ColumnProfile field, String value){
    return nvcompare(field,LTEQ,value);
  }
  public QueryString nLTEQv(ColumnProfile field, long value){
    return nvcompare(field,LTEQ,value);
  }

  public QueryString nLTv(ColumnProfile field, String value){
    return nvcompare(field,LT,value);
  }
  public QueryString nLTv(ColumnProfile field, long value){
    return nvcompare(field,LT,value);
  }

  public QueryString nGTv(ColumnProfile field, String value){
    return nvcompare(field,GT,value);
  }
  public QueryString nGTv(ColumnProfile field, long value){
    return nvcompare(field,GT,value);
  }
  //end binary compares
  /////////////////////////////

  public QueryString isNull(ColumnProfile field) {
    return isNull(field.fullName());
  }

  public QueryString isNull(String field) {
    return word(field).cat(ISNULL);
  }

  public QueryString isTrue(ColumnProfile field) {
    return booleanIs(field, true);
  }

  public QueryString isFalse(ColumnProfile field) {
    return booleanIs(field, false);
  }

  /**
   * checks for field being desired state
   */
  public QueryString booleanIs(ColumnProfile field,boolean desired){
    if(!desired) {
      not();
    }
    return word(field.fullName());
  }

  // means is null or == ''
  public QueryString isEmpty(ColumnProfile field) {
    Open().isNull(field.fullName());
    if(field.numericType()==ColumnTypes.CHAR || field.numericType()==ColumnTypes.TEXT) {
      or().nvPair(field.fullName(), "");
    }
    return Close();
  }

  public QueryString matching(ColumnProfile field1, ColumnProfile field2){
    return word(field1.fullName()).cat(EQUALS).word(field2.fullName());
  }

  /**
   * if the range wraps we must parenthesize and OR the terms
   * parens don't hurt if we aren't wrapping.
   * this presumes that the data being search is already within the modular range.
   */
  private QueryString rangy(boolean wrapper, ColumnProfile field, String start, String end, boolean closeEnd){
    return Open(field).cat(GTEQ).value(start) .cat(wrapper?OR:AND)
    .word(field.fullName()).cat(closeEnd?LTEQ:LT).value(end).Close();
  }

  public QueryString andRange(ColumnProfile rangeName, ObjectRange strange, boolean closeEnd){
    if(ObjectRange.NonTrivial(strange)){
      and().range(rangeName, strange, closeEnd);
    }
    return this;
  }

  public QueryString orRange(ColumnProfile rangeName,ObjectRange strange, boolean closeEnd){
    if(ObjectRange.NonTrivial(strange)){
      or().range(rangeName, strange, closeEnd);
    }
    return this;
  }

  public QueryString range(ColumnProfile rangeName,ObjectRange strange, boolean closeEnd){
    if(ObjectRange.NonTrivial(strange)){
      dbg.VERBOSE("range:"+strange);
      if(strange.singular()){ // +++ simplify using new ObjectRange.end() functionality
        nvPair(rangeName,strange.oneImage()); //ends are the same. just do an equals
      } else {
        rangy(false, rangeName, strange.oneImage(), strange.twoImage(), closeEnd);
      }
    }
    return this;
  }

  public QueryString andRange(ColumnProfile rangeName,ObjectRange strange){
    return andRange(rangeName, strange, true);
  }

  ///////////////////////////////
  private QueryString(String starter) {
    guts.setLength(0);//removes contents but not allocation
    guts.append(starter);
  }

  public static QueryString Select(){
    return new QueryString(SELECT);
  }

  public static QueryString Select(QueryString first){
    return Select().cat(first);
  }

  public static QueryString Select(ColumnProfile cp){
    return Select().cat(cp.fullName());
  }

  public static QueryString SelectDistinct(ColumnProfile cp){
    return Select().cat(DISTINCT).cat(cp.fullName());
  }

  public QueryString all() {
    return word(ALLFIELDS);
  }

  public QueryString all(TableProfile table) {
    return word(table.all());
  }

  public static QueryString SelectAll() {
    return new QueryString(SELECTALL);
  }

  public static QueryString SelectAllFrom(TableProfile table){
    return SelectAll().from(table);
  }

  public QueryString from(TableProfile first){
    return word(FROM).word(first.name());
  }

//  public QueryString fromOuter(TableProfile left, TableProfile right) {
//    return word(FROM).word(left.name()).word(OUTER).word(JOIN).word(right.name());
//  }

  // +++ should do this by passing in both tables and making sure they both have that field?
  // LEFT OUTER JOIN ASSOCIATE USING (associateid)
  // this function extracts the table name from the column profile !!!
  public QueryString leftOuterJoinUsing(ColumnProfile column) {
    return cat(LEFT).cat(OUTER).cat(JOIN).cat(column.table().name()).using(column);
  }

  public QueryString using(ColumnProfile column) {
    // do NOT use Open(column), or it will put the table name in it (column.fullname()), which will blow it!
    return cat(USING).Open().cat(column.name()).Close();
  }

  // PG only
  // the format of the query to do so is: SELECT nextval('tablename_fieldname_seq')
  public static QueryString SelectNextVal(ColumnProfile field) {
    return Select().word(NEXTVAL).Open().value(field.table().name()+"_"+field.name()+"_"+SEQ).Close();
  }

  public QueryString union(QueryString two){
    return word(UNION).word(String.valueOf(two));
  }

  private static QueryString Insert(TableProfile table){
    return new QueryString(INSERTINTO).cat(table.name());
  }

  public static QueryString Insert(TableProfile table, EasyProperties toInsert, UniqueId serialValue) {
    return Insert(table, toInsert, serialValue, false /*valuesFromDefaults*/);
  }

  public static QueryString Insert(TableProfile table, EasyProperties toInsert, UniqueId serialValue, boolean valuesFromDefaults) {
    QueryString names = QueryString.Insert(table).Open();
    QueryString values = QueryString.Clause().Close().Values();
    QueryString end = QueryString.Clause().Close();
    if(toInsert != null) {
      // be sure that the primary key is included in the list of properties, if valid
      ColumnProfile omitSerial;
      if(table.primaryKey != null) { // HOWEVER, all tables need to have a primary key !!!
        omitSerial = table.primaryKey.field;
        if(UniqueId.isValid(serialValue)) {
          // be sure that the field for the serial value is in the fieldlist
          toInsert.setInt(omitSerial.name(), serialValue.value());
        }
      } else {
        dbg.WARNING("Table " + table.name() + " does not have a primary key!  (bad implementation)");
      }
      // validate the property names with the fields to ensure insertion of at least SOME data?
      boolean first = true;
      for(Enumeration ennum = toInsert.propertyNames(); ennum.hasMoreElements();) {
        String name = (String)ennum.nextElement();
        ColumnProfile column = table.column(name);
        if(column == null) {
          dbg.ERROR("Insert(): field " + name + " not found in table " + table.name() + "!");
        } else {
          // prefix with commas if needed
          if (first) {
            first = false;
          } else {
            names.comma();
            values.comma();
          }
          // add strings
          names.cat(name);
          String value = toInsert.getProperty(name);
          dbg.VERBOSE("Property scan: " + name + "=" + value);
          values.cat(valueByColumnType(column, value, !valuesFromDefaults /*forceQuotes*/));
        }
      }
    }
    return names.cat(values).cat(end);
  }


  private static QueryString Update(TableProfile table) {
    return new QueryString(UPDATE).word(table.name()).cat(SET);
  }

  // call this, then .where()... to add a where clause.
  // Just BE SURE to add the clause, or you will change ALL RECORDS!!!
  public static QueryString Update(TableProfile table, EasyProperties toUpdate) {
    QueryString qs = Update(table); // this makes UPDATE TABLE SET
    // loop through the properties and set them
    // validate the property names with the fields to ensure insertion of at least SOME data?
    boolean first = true;
    for (Enumeration ennum = toUpdate.propertyNames(); ennum.hasMoreElements(); ) {
      String name = (String) ennum.nextElement();
      ColumnProfile column = table.column(name);
      if (column == null) {
        dbg.ERROR("Update(): field " + name + " not found in table " +
                  table.name() + "!");
      } else {
        // prefix with commas if needed
        if (first) {
          first = false;
        } else {
          qs.comma();
        }
        // find the value
        String value = toUpdate.getProperty(name);
        dbg.VERBOSE("Property scan: " + name + "=" + value);
        // tack on the name=value
        // NOTE! Do not use column.fullname!  *Might* cause problems with PG.
        qs.cat(column.name()).cat(EQUALS).cat(valueByColumnType(column, value));
      }
    }
    return qs;
  }

  private static String valueByColumnType(ColumnProfile column, String prevalue) {
    return valueByColumnType(column, prevalue, true /*forceQuotes*/);
  }

  private static String valueByColumnType(ColumnProfile column, String prevalue, boolean forceQuotes) {
    if(prevalue == null) { // do NOT use !NonTrivial() here!  NULL is the check we are doing.
      return NULL; // do not quote a null, but instead use the word null
    } else {
      ColumnTypes type = column.numericType();
      dbg.VERBOSE("ColumnTypes for " + column.name() + " is " + type);
      switch (type) {
        case SERIAL:
        case INT4: { // do not quote integer or serial !
          String postvalue = "";
          if(!StringX.NonTrivial(prevalue)) {
            // then we WANT null!
            if (!column.nullable()) {
              dbg.ERROR("PANIC! valueByColumnType(INT4) passed empty string, but NULL not allowed for column " +
                        column.fullName() + "!");
              // then go ahead and use the value passed to us!
            }
            return NULL; // and pray that the field can handle a null!
          } else {
            int integer = StringX.parseInt(prevalue);
            if ((integer == ObjectX.INVALIDINDEX) && column.isProbablyId()) { // but can't really check integrity any better than this
              // --- if this column doesn't allow null, this will except, HOWEVER, we can't *guess* at a value!
              // so, in that case, we should send a panic!
              if (!column.nullable()) {
                dbg.ERROR("PANIC! valueByColumnType(INT4) passed '"+prevalue+"', but NULL not allowed for column " +
                          column.fullName() + ", so using value verbatim");
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
        case TEXT:
        case CHAR: { // but we shouldn't have any CHARs, except for "char", single char values
          if("null".equalsIgnoreCase(prevalue.trim())) {
            // +++ what if null isn't allowed?  Let's have it put "" in that case
            if(column.nullable()) {
              return NULL;
            } else {
              return Quoted(""); // for cases where null is not valid
            }
          } else {
            if(!forceQuotes && prevalue.startsWith("'") && prevalue.endsWith("'")) { // don't add extra quotes if not needed
              return prevalue;
            }
            return Quoted(prevalue); // quote char or unknown type fields
          }
        }
      }
    }
  }

//  public static final QueryString StatisticsAndCleanup() {
//    return Vacuum(true, true, false);
//  }

  // postgresql only
  public static QueryString Vacuum(TableProfile tp, boolean verbose, boolean analyze, boolean full) {
    return VacuumAnalyze(tp, verbose, analyze, full, true);
  }
  public static QueryString VacuumAnalyze(TableProfile tp, boolean verbose, boolean analyze, boolean full, boolean vacuum) {
    QueryString qs = new QueryString("");
    if(vacuum) {
      qs.cat(VACUUM);
      if(full) {
        qs.cat(FULL);
      }
    }
    if(analyze) {
      qs.cat(ANALYZE);
    }
    if((vacuum || analyze) && verbose) {
      qs.cat(VERBOSE);
    }
    if((tp != null) && StringX.NonTrivial(tp.name())) {
      qs.cat(tp.name());
    }
    return qs;
  }

  // postgresql only
  public final QueryString limit(int n) {
    return cat(LIMIT).cat(n);
  }

  public static QueryString AlterTable(TableProfile table) {
    return new QueryString(ALTERTABLE).cat(table.name());
  }

  public static QueryString RenameColumn(String table, String from, String to) {
    return new QueryString(ALTERTABLE).cat(table).
        word(RENAMECOLUMN).word(from).cat(TO).word(to);
  }

  public static QueryString AddColumn(ColumnProfile column) {
    return QueryString.AlterTable(column.table()).word(ADD).createFieldClause(column, true);
  }

  public final QueryString AlterColumn(ColumnProfile column) {
    return cat(ALTER).word(column.name());
  }

  public static QueryString DropTable(TableProfile table) {
    return new QueryString(DROPTABLE).cat(table.name());
  }
  // column points to its table
  public final QueryString dropColumn(ColumnProfile column) {
    return word(DROPCOLUMN).cat(column.name());
  }
  public static QueryString DropIndex(IndexProfile index) {
    return new QueryString(DROPINDEX).cat(index.name);
  }
  public final QueryString DropNotNull() {
    return cat(DROP).not().cat(NULL);
  }
  public final QueryString SetNotNull() {
    return cat(SET).not().cat(NULL);
  }

  public final QueryString DropDefault() {
    return cat(DROP).cat(DEFAULT);
  }
  public final QueryString SetDefault(String def) {
    return cat(SET).cat(DEFAULT).cat(def); // +-+ test this
  }

  public static QueryString DropConstraint(TableProfile table, Constraint constr) {
    return AlterTable(table).word(DROPCONSTRAINT).word(constr.name);
  }


  public static QueryString DeleteFrom(TableProfile table) {
    QueryString qs=new QueryString(DELETEFROM);
    return qs.cat(table.name()).cat(SPACE);
  }

  public static QueryString Clause(){
    return new QueryString(EMPTY);
  }

  // testing these new things ...
  public static QueryString generateTableCreate(TableProfile tp) {
    QueryString qs = new QueryString(CREATETABLE).cat(tp.name());
    if(tp.numColumns() > 1) {
      qs.Open();
    }
    for(int i = 0; i < tp.numColumns(); i++) {
      qs.createFieldClause(tp.column(i), false /* not an existing table */);
      // on all but the last one, add a comma
      if(!(i == (tp.numColumns()-1))) {
        qs.comma();
      }
    }
    if(tp.numColumns() > 1) {
      qs.Close();
    }
    return qs;
  }

  public QueryString createFieldClause(ColumnProfile cp, boolean existingTable) {
    QueryString qs = this;
    qs.word(cp.name());
    ColumnTypes dbt = ColumnTypes.valueOf(cp.type().toUpperCase());
    if(cp.autoIncrement()) {
      //noinspection StatementWithEmptyBody
      if(dbt== ColumnTypes.SERIAL) {
        // this is fine!
      } else {
        if(dbt== ColumnTypes.INT4 || dbt== ColumnTypes.SMALLINT || dbt== ColumnTypes.TINYINT) {
          dbt = ColumnTypes.SERIAL;
        } else {
          dbg.ERROR("Don't know how to autoincrement the datatype " + dbt + "!");
        }
      }
    }
    switch(dbt) {
      default: {
        dbg.ERROR("Data type not considered in createFieldClause(): " + dbt.toString() + "!");
      }
      case CHAR: {
        if(cp.size() < 2) {
          qs.cat("\"char\""); // the PG "char" type, which implements char(1) in the main table (faster, smaller)
          break;
        } // else  do like all the rest
      }
      case BOOL:
      case DATETIME:
      case SERIAL:
      case TEXT:
        // +++ DO MORE OF THESE!!!
      case INT4: {
        qs.cat(dbt.name());
      } break;
    }
    // default goes here
    //noinspection StatementWithEmptyBody
    if(!existingTable && StringX.NonTrivial(cp.columnDef)) {
      qs.word(DEFAULT).word(cp.dbReadyColumnDef());
    } else {
      // do nothing
      // can't add a default clause to an 'alter table add field' statement in PG
      // also, if the default isn't defined, don't add a default clause
    }

    return qs;
  }

  // PG:  ALTER TABLE TXN ADD CONSTRAINT txnpk PRIMARY KEY(TXNID)
  public static QueryString addKeyConstraint(KeyProfile key) {
    QueryString qs = AlterTable(key.table).cat(ADDCONSTRAINT);
    String keytype = key.isPrimary() ? PRIMARYKEY : FOREIGNKEY;
    qs.word(key.name).cat(keytype).parenth(key.field.name());
    if(!key.isPrimary()) {
      ForeignKeyProfile fk = (ForeignKeyProfile)key;
      qs.cat(REFERENCES).cat(fk.referenceTable.name());
    }
    return qs;
  }

  public static QueryString CreateIndex(IndexProfile index) {
    QueryString ret = index.unique ? new QueryString(CREATEUNIQUEINDEX) : new QueryString(CREATEINDEX);
    ret.cat(index.name).cat(ON).cat(index.table.name()).parenth(index.columnNamesCommad());
    if(index.whereclause != null) {
      ret.cat(index.whereclause);
    }
    return ret;
  }

  public static QueryString ExplainAnalyze() {
    return QueryString.Clause().cat(EXPLAINANALYZE);
  }

  public static QueryString ShowParameter(String paramname) {
    return QueryString.Clause().cat(SHOW).cat(paramname);
  }

  public static QueryString TableStats(TableProfile table) {
    return QueryString.Clause().cat(
        "select a.relpages,a.reltuples,"+
        "b.n_tup_ins,b.n_tup_upd,b.n_tup_del "+
        "from pg_class a, pg_stat_user_tables b where a.relfilenode=b.relid "+
        "and a.relname='" + table.name() + "'");
  }

  public static QueryString TablePages(TableProfile table) {
    return QueryString.Select().cat(
        "reltuples,relpages "+
        "from pg_class where relname='"+table.name()+"'");
  }

  public static QueryString DatabaseAge(String databasename) {
    return QueryString.Select().cat(
        "age(datfrozenxid) from pg_database where datname='"+databasename+"'");
  }


  protected QueryString castAs(String what, String datatype) {
    //CAST(TXN.TRANSTARTTIME AS BIGINT)
    return cat(CAST).Open().cat(what).as(datatype).Close();
  }

  public final QueryString castAsBigint(String what) {
    return castAs(what, BIGINT);
  }

  public final boolean isReadOnly() {
    return isReadOnly(toString());
  }

  public static boolean isReadOnly(QueryString qs) {
    return qs == null || qs.isReadOnly();
// read only since it cannot change the database if it is null!
  }

  private static final String SLIMUPPERSELECT = SELECT.trim().toUpperCase();
  private static final String SLIMLOWERSELECT = SELECT.trim().toLowerCase();
  private static final String SLIMUPPERSHOW = SHOW.trim().toUpperCase();
  private static final String SLIMLOWERSHOW = SHOW.trim().toLowerCase();
  private static final String LEFTOFEXPLAIN = StringX.subString(EXPLAIN, 0, 6);// "EXPLAI" since only 6 chars long
  public static boolean isReadOnly(String qss) {
    if (qss != null) {
      qss = qss.trim(); // get rid of leading spaces
      if (qss.length() > 6) {
        String sqlop = StringX.TrivialDefault(StringX.subString(qss, 0, 6), "");
        dbg.VERBOSE("SQL op: " + sqlop);
        boolean isSelect;
        isSelect = SLIMUPPERSELECT.equalsIgnoreCase(sqlop);
        if (!isSelect) { // let's be sure ... [+++ we can make this better!]
          if ( (qss.contains(SLIMUPPERSELECT)) || (qss.contains(SLIMLOWERSELECT))) { // then it really might be a select, so look further
            // so, there is a select somewhere in there, but not as the first word
            // if the first word is "EXPLAIN" (special PG function), then it is probably a select
            if (LEFTOFEXPLAIN.equalsIgnoreCase(sqlop)) {
              // then this is a SELECT since both SELECT and EXPLAIN exist here
              isSelect = true;
            } // else if ... +++
          }
          if (!isSelect && (sqlop.startsWith(SLIMLOWERSHOW) || sqlop.startsWith(SLIMUPPERSHOW))) {
            return true;
          }
        }
        return isSelect;
      } else {
        dbg.VERBOSE("SQLOP too short to know if it is a select!");
        return false; // don't know, basically
      }
    } else {
      return true; // read only since it cannot change the database if it is null!
    }
  }

  /////////////////////
  public static QueryString genDropField(ColumnProfile column) {
    return QueryString.AlterTable(column.table()).dropColumn(column);
  }

  public static QueryString genDropIndex(String indexname) {
    return QueryString.DropIndex(new IndexProfile(indexname, null, new ColumnProfile[0]));
  }

  public static QueryString genDropTable(String tablename) {
    return QueryString.DropTable(TableProfile.create(new TableInfo(tablename), null, null));
  }

  public static QueryString genAddField(ColumnProfile column) {
    return QueryString.AddColumn(column);
  }

  public static QueryString genCreateIndex(IndexProfile index) {
    return QueryString.CreateIndex(index);
  }

  public static QueryString genAddPrimaryKeyConstraint(PrimaryKeyProfile primaryKey) {
    return QueryString.addKeyConstraint(primaryKey);
  }

  public static QueryString genAddForeignKeyConstraint(ForeignKeyProfile foreignKey) {
    return QueryString.addKeyConstraint(foreignKey);
  }

  public static QueryString genDropConstraint(TableProfile table, Constraint constr) {
    return QueryString.DropConstraint(table, constr);
  }

  public static QueryString genRenameColumn(String table, String oldname, String newname) {
    return QueryString.RenameColumn(table, oldname, newname);
  }

  public static QueryString genCreateTable(TableProfile tp) {
    return QueryString.generateTableCreate(tp);
  }
  // Note that you have to get a serial field's next value to use to insert the record
  static QueryString genSelectNextVal(ColumnProfile cp) {
    return QueryString.SelectNextVal(cp);
  }

  static QueryString genMaxId(ColumnProfile pk) {
    return genid(pk.table()).orderbydesc(pk).limit(1);
  }

  static QueryString genSelectRecord(TableProfile tp, UniqueId id) {
    return QueryString.SelectAllFrom(tp).where().nvPair(tp.primaryKey.field, id);
  }

  static QueryString genUpdateRecord(TableProfile tp, UniqueId id, EasyProperties ezp) {
    return QueryString.Update(tp, ezp).where().nvPair(tp.primaryKey.field, id);
  }
  public static QueryString whereNot(ColumnProfile column) {
    return QueryString.Clause().where().isFalse(column);
  }
  public static QueryString whereIsTrue(ColumnProfile column) {
    return QueryString.Clause().where().isTrue(column);
  }
  // fragment that can be used for all genid()s
  public static QueryString genid(TableProfile tp) {
    return QueryString.Select(tp.primaryKey.field).from(tp);
  }
  private static QueryString idDistinct(TableProfile tp) {
    return QueryString.SelectDistinct(tp.primaryKey.field).from(tp);
  }

  public static QueryString genChangeFieldNullable(ColumnProfile field) {
    QueryString qs = QueryString.AlterTable(field.table()).AlterColumn(field);
    if(field.nullable()) {
      qs.DropNotNull();
    } else {
      qs.SetNotNull();
    }
    return qs;
  }

  public static QueryString genChangeFieldDefault(ColumnProfile field) {
    QueryString qs = QueryString.AlterTable(field.table()).AlterColumn(field);
    if(StringX.NonTrivial(field.columnDef)) {
      qs.SetDefault(field.dbReadyColumnDef()); // dbReadyColumnDef() quotes things that need to be quoted
    } else {
      qs.DropDefault();
    }
    return qs;
  }


}
