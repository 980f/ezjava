package pers.hal42.database;

import pers.hal42.data.ObjectRange;
import pers.hal42.lang.StringX;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.text.TextList;

public class QueryString {
  public static final ErrorLogStream dbg = ErrorLogStream.getForClass(PGQueryString.class);

  protected static final String FROM = " FROM ";
  protected static final String UPDATE = " UPDATE ";
  protected static final String SET = " SET ";
  protected static final String INSERTINTO = " INSERT INTO ";
  protected static final String ON = " ON ";
  protected static final String IN = " IN ";
  protected static final String OPENPAREN = " (";
  protected static final String CLOSEPAREN = ") ";
  protected static final String SPACE = " ";
  protected static final String EQUALS = " = ";
  protected static final String ADD = " ADD ";
  protected static final String NULL = " null ";
  protected static final String JOIN = " JOIN ";
  protected static final String LEFT = " LEFT ";
  protected static final String SHOW = " SHOW ";
  protected static final String EXPLAIN = " EXPLAIN ";
  ///////////////////////////////////////
  // Keywords:
  protected static final String SELECT = " SELECT ";
  protected static final String AND = " AND ";
  protected static final String NOT = " NOT ";
  protected static final String WHERE = " WHERE ";
  protected static final String ORDERBY = " ORDER BY ";
  protected static final String GROUPBY = " GROUP BY ";
  protected static final String ASC = " ASC ";
  protected static final String DESC = " DESC ";
  protected static final String UNION = " UNION ";
  protected static final String VALUES = " VALUES ";
  protected static final String ALLFIELDS = " * ";
  protected static final String SELECTALL = SELECT + ALLFIELDS;
  protected static final String OR = " OR ";
  protected static final String COMMA = " , ";
  protected static final String GTEQ = " >= ";
  protected static final String GT = " > ";
  protected static final String LTEQ = " <= ";
  protected static final String LT = " < ";
  protected static final String EMPTY = "";
  protected static final String IS = " IS ";
  protected static final String ISNULL = IS + NULL; // no space since each has a space before and after
  protected static final String DISTINCT = " DISTINCT ";

  protected static final String COLUMN = " COLUMN ";

  protected static final String SUM = " SUM ";
  protected static final String COUNT = " COUNT ";
  protected static final String MAX = " MAX ";
  protected static final String MIN = " MIN ";
  protected static final String AS = " AS ";
  protected static final String HAVING = " HAVING ";
  //  protected static final String COMMAOUTER = COMMA + OUTER;
  protected static final String SUBSTRING = " SUBSTRING ";
  protected static final String TO = " TO ";
  protected static final String FOR = " FOR ";
  protected static final String CASE = " CASE ";
  protected static final String END = " END ";
  protected static final String ELSE = " ELSE ";
  protected static final String THEN = " THEN ";
  protected static final String WHEN = " WHEN ";
  protected static final String USING = " USING ";
  protected static final String CAST = " CAST ";
  protected static final String BIGINT = " BIGINT ";
  //added for postgres:
  protected static final String LIMIT = " LIMIT ";
  protected static final String NEXTVAL = " NEXTVAL ";
//

  //  protected static final String SLIMUPPERSELECT = SELECT.trim().toUpperCase();
//  protected static final String SLIMLOWERSELECT = SELECT.trim().toLowerCase();
//  protected static final String SLIMUPPERSHOW = SHOW.trim().toUpperCase();
//  protected static final String SLIMLOWERSHOW = SHOW.trim().toLowerCase();
//  protected static final String LEFTOFEXPLAIN = StringX.subString(EXPLAIN, 0, 6);// "EXPLAI" since only 6 chars long
  protected StringBuilder guts = new StringBuilder(200);//most queries are big

  protected QueryString(String starter) {
    guts.append(starter);
  }

  protected QueryString() {
    guts.append("");
  }

  public String toString() {//works with String +
    return String.valueOf(guts);
  }

  public QueryString cat(QueryString s) {
    cat(SPACE).cat(String.valueOf(s));
    return this;
  }

  public QueryString cat(String s) {
    guts.append(s);
    return this;
  }

  public QueryString cat(char c) {
    guts.append(c);
    return this;
  }

  public QueryString cat(int s) {
    return cat((long) s);
  }

  public QueryString cat(long s) {
    guts.append(s);
    return this;
  }

  public QueryString Open() {
    return cat(OPENPAREN);
  }

  public QueryString Open(ColumnProfile cp) {
    return Open().cat(cp.fullName());
  }

  public QueryString Close() {
    return cat(CLOSEPAREN);
  }


  public QueryString where() {
    return cat(WHERE);
  }

  public QueryString and() {
    return cat(AND);
  }

  public QueryString or() {
    return cat(OR);
  }

  public QueryString not() {
    return cat(NOT);
  }

  protected QueryString in(String list) {
    return word(IN).Open().cat(list).Close();
  }

  public QueryString inQuoted(TextList list) {
    // have to do this list manually, as quoting has some special rules ...
    // create a new list with the quoted strings in it, then cat them into a StringBuffer and output as a string.
    // This would be faster if it was directly into a StringBuffer, but I am in a hurry right now.
    TextList list2 = new TextList(list.size());
    for (int i = 0; i < list.size(); i++) {
      list2.add(Quoted(list.itemAt(i)));
    }
    return inUnquoted(list2);
  }

  public QueryString inUnquoted(TextList list) {
    return in(list.asParagraph(","));
  }

  public QueryString inUnquoted(ColumnProfile cp, TextList list) {
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

  protected QueryString sum(String str) {
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
  public QueryString AndInList(ColumnProfile field, TextList options) {
    if ((options != null) && (field != null)) {
      switch (options.size()) {
      case 0://make no changes whatsoever
        break;
      case 1://a simple compare
        return nvPair(field, options.itemAt(0));
      default://2 or more must be or'd together or use "IN" syntax %%%

        break;
      }
    }
    return this;
  }

  public QueryString SetJust(ColumnProfile field, String val) {
    // do NOT use nvPair(field,val) here, as it puts a table. prefix on the fieldname
    // eg: table.field=value.  This FAILS in PG!
    // instead, use the following:
    return cat(SET).nvPair(field.name(), val);
  }

  public QueryString SetJust(ColumnProfile field, int val) {//wishing for templates....
    return cat(SET).nvPair(field.name(), val); // do not change this until you read the above function !!!
  }

  protected QueryString SetJust(ColumnProfile field, Long val) {//wishing for templates....
    return cat(SET).nvPair(field.name(), val); // do not change this until you read the above function !!!
  }

  public QueryString Values(String first) {
    return Values().value(first);
  }

  public QueryString Values() {
    return cat(VALUES + OPENPAREN);
  }

  public QueryString comma() {
    return cat(COMMA);
  }

  public QueryString comma(ColumnProfile cp) {
    return comma().cat(cp.fullName());
  }

  public QueryString comma(TableProfile tp) {
    return comma().cat(tp.name());
  }

  public QueryString comma(QueryString qs) {
    return comma().cat(qs);
  }

  public QueryString comma(int s) {
    return comma().cat(s);
  }

  protected QueryString word(String s) {
    return cat(SPACE).cat(s).cat(SPACE);
  }

  public QueryString parenth(String s) {
    return cat(SPACE).Open().cat(s).Close().cat(SPACE);
  }

  public QueryString parenth(QueryString s) {
    return parenth(String.valueOf(s));
  }

  public QueryString value(String s) {
    return cat(Quoted(s));
  }

  public QueryString value(char c) {
    return cat(Quoted(c));
  }

  public QueryString value(long ell) {
    return cat(/*Quoted(*/ell/*)*/); // DO NOT quote this; it can cause the database txn to be very slow, as it has to convert its type before EACH comparison!
  }

  public QueryString quoted(char c) {
    return cat(SPACE).value(c);
  }

  public QueryString commaQuoted(String s) {
    return cat(COMMA).value(s);
  }

  public QueryString commaQuoted(char c) {
    return cat(COMMA).value(c);
  }

  public QueryString orderbyasc(ColumnProfile first) {
    return cat(ORDERBY).cat(first.fullName()).cat(ASC); // --- possible cause of bugs
  }

  public QueryString orderbydesc(ColumnProfile first) {
    return cat(ORDERBY).cat(first.fullName()).cat(DESC); // --- possible cause of bugs
  }

  public QueryString orderbydesc(int columnFirst) {
    return cat(ORDERBY).cat(String.valueOf(columnFirst)).cat(DESC);
  }

  public QueryString orderbyasc(int columnFirst) {
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
   * Use one function that takes an object and does intelligent things with it instead of cols of these ???
   */
  public QueryString nvPairNull(ColumnProfile field) {
    return word(field.fullName()).cat(EQUALS).cat(NULL);
  }

  public QueryString nvPair(ColumnProfile field, String value) {
    return nvPair(field.fullName(), value);
  }

  public QueryString nvPair(ColumnProfile field, int value) {
    return nvPair(field.fullName(), value);
  }

  public QueryString nvPair(ColumnProfile field, long value) {
    return nvPair(field.fullName(), value);
  }

  public QueryString nvPair(ColumnProfile field, Long value) {
    return nvPair(field.fullName(), value); // --- just made this use the full name so that won't get the "ambiguous column" bug
  }

  public QueryString nvPair(String field, String value) {
    return word(field).cat(EQUALS).value(value);
  }

  public QueryString nvPair(String field, int value) {
    return nvPair(field, (long) value);
  }

  public QueryString nvPair(String field, long value) {
    return word(field).cat(EQUALS).value(value);
  }

  public QueryString nvPair(String field, Long value) {
    return word(field).cat(EQUALS).cat((value == null) ? NULL : String.valueOf(value));
  }

  public QueryString nvcompare(ColumnProfile field, String cmpop, String value) {
    return word(field.fullName()).cat(cmpop).value(value);
  }

  public QueryString nvcompare(ColumnProfile field, String cmpop, long value) {
    return word(field.fullName()).cat(cmpop).value(value);
  }

  public QueryString nGTEQv(ColumnProfile field, String value) {
    return nvcompare(field, GTEQ, value);
  }

  public QueryString nGTEQv(ColumnProfile field, long value) {
    return nvcompare(field, GTEQ, value);
  }

  public QueryString nLTEQv(ColumnProfile field, String value) {
    return nvcompare(field, LTEQ, value);
  }

  public QueryString nLTEQv(ColumnProfile field, long value) {
    return nvcompare(field, LTEQ, value);
  }

  public QueryString nLTv(ColumnProfile field, String value) {
    return nvcompare(field, LT, value);
  }

  public QueryString nLTv(ColumnProfile field, long value) {
    return nvcompare(field, LT, value);
  }

  public QueryString nGTv(ColumnProfile field, String value) {
    return nvcompare(field, GT, value);
  }

  public QueryString nGTv(ColumnProfile field, long value) {
    return nvcompare(field, GT, value);
  }

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
  public QueryString booleanIs(ColumnProfile field, boolean desired) {
    if (!desired) {
      not();
    }
    return word(field.fullName());
  }

  // means is null or == ''
  public QueryString isEmpty(ColumnProfile field) {
    Open().isNull(field.fullName());
    if (field.getType().isTextlike()) {
      or().nvPair(field.fullName(), "");
    }
    return Close();
  }

  public QueryString matching(ColumnProfile field1, ColumnProfile field2) {
    return word(field1.fullName()).cat(EQUALS).word(field2.fullName());
  }

  /**
   * if the range wraps we must parenthesize and OR the terms
   * parens don't hurt if we aren't wrapping.
   * this presumes that the data being search is already within the modular range.
   */
  protected QueryString rangy(boolean wrapper, ColumnProfile field, String start, String end, boolean closeEnd) {
    return Open(field).cat(GTEQ).value(start).cat(wrapper ? OR : AND).word(field.fullName()).cat(closeEnd ? LTEQ : LT).value(end).Close();
  }

  public QueryString andRange(ColumnProfile rangeName, ObjectRange strange, boolean closeEnd) {
    if (ObjectRange.NonTrivial(strange)) {
      and().range(rangeName, strange, closeEnd);
    }
    return this;
  }

  public QueryString orRange(ColumnProfile rangeName, ObjectRange strange, boolean closeEnd) {
    if (ObjectRange.NonTrivial(strange)) {
      or().range(rangeName, strange, closeEnd);
    }
    return this;
  }

  public QueryString range(ColumnProfile rangeName, ObjectRange strange, boolean closeEnd) {
    if (ObjectRange.NonTrivial(strange)) {
      dbg.VERBOSE("range:" + strange);
      if (strange.singular()) { // +++ simplify using new ObjectRange.end() functionality
        nvPair(rangeName, strange.oneImage()); //ends are the same. just do an equals
      } else {
        rangy(false, rangeName, strange.oneImage(), strange.twoImage(), closeEnd);
      }
    }
    return this;
  }

  public QueryString andRange(ColumnProfile rangeName, ObjectRange strange) {
    return andRange(rangeName, strange, true);
  }

  public QueryString all() {
    return word(ALLFIELDS);
  }

  public QueryString using(ColumnProfile column) {
    // do NOT use Open(column), or it will put the table name in it (column.fullname()), which will blow it!
    return cat(USING).Open().cat(column.name()).Close();
  }

  public QueryString union(QueryString two) {
    return word(UNION).word(String.valueOf(two));
  }

  // postgresql only
  public final QueryString limit(int n) {
    return cat(LIMIT).cat(n);
  }


  protected QueryString castAs(String what, String datatype) {
    //CAST(TXN.TRANSTARTTIME AS BIGINT)
    return cat(CAST).Open().cat(what).as(datatype).Close();
  }

  public final QueryString castAsBigint(String what) {
    return castAs(what, BIGINT);
  }
//
//  public final boolean isReadOnly() {
//    return isReadOnly(toString());
//  }

  /*
       Rules:
       1) If it contains any single quotes, escape them with another single quote
       2) Wrap it in single quotes,
    */
  public static String Quoted(String s) {
    String ret = StringX.singleQuoteEscape(s);
    dbg.VERBOSE("Quoted():" + s + "->" + ret);
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
   * "this " is a double quote"
   * is not valid.  This one is:
   * 'this " is a double quote'
   * And so is this one:
   * "this "" is a double quote"
   * And this one:
   * 'this '' is a single quote'
   */


  public static String Quoted(char c) {
    return Quoted(String.valueOf(c));
  }

  public static String Quoted(long ell) {
    return Quoted(String.valueOf(ell));
  }

  public static QueryString Select() {
    return new QueryString(SELECT);
  }

  /**
   * bridge for using old stuff wiht new builder technique
   */
  public static QueryString Wrap(String fullquery) {
    return new QueryString(fullquery);//todo:2 additional method to idiot check like for terminating semi
  }

  public static QueryString Select(QueryString first) {
    return Select().cat(first);
  }

  public static QueryString Select(ColumnProfile cp) {
    return Select().cat(cp.fullName());
  }

  public static QueryString SelectDistinct(ColumnProfile cp) {
    return Select().cat(DISTINCT).cat(cp.fullName());
  }

  public static QueryString SelectAll() {
    return new QueryString(SELECTALL);
  }

//   /** @returns wehter query modifies the database, records or schema*/
//  public static boolean isReadOnly(String qss) {
//    if (qss != null) {
//      qss = qss.trim(); // get rid of leading spaces
//      if (qss.length() > 6) {
//        String sqlop = StringX.TrivialDefault(StringX.subString(qss, 0, 6), "");
//        dbg.VERBOSE("SQL op: " + sqlop);
//        boolean isSelect;
//        isSelect = SLIMUPPERSELECT.equalsIgnoreCase(sqlop);
//        if (!isSelect) { // let's be sure ... [+++ we can make this better!]
//          if ((qss.contains(SLIMUPPERSELECT)) || (qss.contains(SLIMLOWERSELECT))) { // then it really might be a select, so look further
//            // so, there is a select somewhere in there, but not as the first word
//            // if the first word is "EXPLAIN" (special PG function), then it is probably a select
//            if (LEFTOFEXPLAIN.equalsIgnoreCase(sqlop)) {
//              // then this is a SELECT since both SELECT and EXPLAIN exist here
//              isSelect = true;
//            } // else if ... +++
//          }
//          if (!isSelect && (sqlop.startsWith(SLIMLOWERSHOW) || sqlop.startsWith(SLIMUPPERSHOW))) {
//            return true;
//          }
//        }
//        return isSelect;
//      } else {
//        dbg.VERBOSE("SQLOP too short to know if it is a select!");
//        return false; // don't know, basically
//      }
//    } else {
//      return true; // read only since it cannot change the database if it is null!
//    }
//  }




  public static QueryString Clause() {
    return new QueryString(EMPTY);
  }

  public static QueryString whereNot(ColumnProfile column) {
    return QueryString.Clause().where().isFalse(column);
  }

  public static QueryString whereIsTrue(ColumnProfile column) {
    return QueryString.Clause().where().isTrue(column);
  }
}
