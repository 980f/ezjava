package pers.hal42.database;

import pers.hal42.data.ObjectRange;
import pers.hal42.lang.StringX;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.text.AsciiIterator;
import pers.hal42.text.ListWrapper;
import pers.hal42.text.TextList;

import java.util.Iterator;

import static java.text.MessageFormat.format;
import static pers.hal42.database.QueryString.SqlKeyword.*;

/**
 * wraps StringBuilder since someone decided that class should be final.
 * mostly ensures spaces are inserted where they need to be, to reduce the number of program restarts when developing new queries.
 * Also of interest is quoting logic, which however should be deprecated in favor of using PreparedStatements which in the case of MySql does value quoting on a greater number of object types than this class does.
 */
public class QueryString {
  public static final ErrorLogStream dbg = ErrorLogStream.getForClass(PGQueryString.class);

  /** sql keywords, in CAPS and surrounded with spaces */
  public enum SqlKeyword {
    FROM,
    UPDATE,
    SET,
    INSERT,
    DELETE,
    ON,
    IN,
    BETWEEN, //beware this is inclusive of both ends.

    ADD,
    NULL,
    JOIN,
    LEFT,
    SHOW,
    EXPLAIN,
    /////////

    SELECT,
    AND,
    NOT,
    WHERE,

    ASC,
    DESC,
    UNION,
    VALUES,
    OR,
    GTEQ,
    GT,
    LTEQ,
    LT,

    IS,

    DISTINCT,

    COLUMN,
    SUM,
    COUNT,
    MAX,
    MIN,
    AS,
    HAVING,

    SUBSTRING,
    TO,
    FOR,
    CASE,
    END,
    ELSE,
    THEN,
    WHEN,
    USING,
    CAST,
    BIGINT,
    LIMIT,;
    String spaced;

    SqlKeyword() {
      spaced = " " + super.toString() + " ";
    }

    /**
     * @return nominal name with spaces around it.
     */
    @Override
    public String toString() {
      return spaced;
    }
  }

  private static final String ORDERBY = "ORDER BY";
  private static final String GROUPBY = "GROUP BY";
  private static final String EQUALS = "=";

//  protected static final String NEXTVAL = " NEXTVAL ";
  //   EMPTY = "";

//   ALLFIELDS = " * ";
//   SELECTALL = SELECT + ALLFIELDS;
//

  //  protected static final String SLIMUPPERSELECT = SELECT.trim().toUpperCase();
//  protected static final String SLIMLOWERSELECT = SELECT.trim().toLowerCase();
//  protected static final String SLIMUPPERSHOW = SHOW.trim().toUpperCase();
//  protected static final String SLIMLOWERSHOW = SHOW.trim().toLowerCase();
//  protected static final String LEFTOFEXPLAIN = StringX.subString(EXPLAIN, 0, 6);// "EXPLAI" since only 6 chars long
  public StringBuilder guts = new StringBuilder(200);//most queries are big
  /** state for deciding whether to use 'where' or 'and' when adding a filter term */
  protected boolean whered = false;
  /** whether to put line breaks in many places */
  public boolean wrappy = true;

  public QueryString(SqlKeyword starter) {
    guts.append(starter);
  }

  public QueryString(String starter) {
    guts.append(starter);
  }

  public QueryString() {
    guts.append("");
  }

  /** @returns query text */
  public String toString() {
    return String.valueOf(guts);
  }

  /** append @param sql wrapped with spaces */
  public QueryString cat(SqlKeyword sql) {
    guts.append(sql.spaced);
    return this;
  }

  /** append @param s wrapped with spaces */
  public QueryString cat(QueryString s) {
    space();
    cat(String.valueOf(s));
    space();
    return this;
  }

  /** append @param s wrapped with spaces */
  public QueryString cat(String s) {
    space();
    guts.append(s);
    space();
    return this;
  }

  /** append @param c without any spaces */
  public QueryString cat(char c) {
    guts.append(c);
    return this;
  }

  /** append number, with spaces */
  public QueryString cat(int s) {
    return cat((long) s);
  }

  /** append number with spaces */
  public QueryString cat(long s) {
    space();
    guts.append(s);
    space();
    return this;
  }

  public QueryString dot(String name) {
    guts.append('.');
    guts.append(name);
    space();
    return this;
  }


  /** append AS clause, with generous spaces */
  public QueryString alias(String aliaser) {
    return cat(AS).word(aliaser);
  }

  /** append AS clause, with generous spaces, advances @param aliaser */
  public QueryString alias(AsciiIterator aliaser) {
    return cat(AS).word(aliaser.next());
  }

  /** append newline and tab */
  public QueryString indent() {
    return cat("\n\t");
  }

  /** JOIN @param tablename ON aliaser.base.joincolumnname EQUALS aliaser.next().joincolumnname */
  public QueryString join(String tablename, String joincolumnname, AsciiIterator aliaser) {
    indent();
    cat(JOIN);
    cat(tablename);
    alias(aliaser.peek());
    cat(ON);
    guts.append(aliaser.first());
    guts.append('.');
    guts.append(joincolumnname);
    cat("=");
    guts.append(aliaser.next());
    dot(joincolumnname);
    return this;
  }

  public QueryString Open() {
    guts.append('(');
    return this;
  }

  public QueryString Open(ColumnProfile cp) {
    return Open().cat(cp.fullName());
  }

  /** @returns this after appending just a close paren, no added spaces. */
  public QueryString Close() {
    guts.append(')');
    return this;
  }

  public QueryString dot(String prefix, String postfix) {
    space();
    guts.append(prefix);
    dot(postfix);
    space();
    return this;
  }

  public Lister startList(String prefix) {
    Lister noob = new Lister(prefix);
    noob.q = this;
    return noob;
  }

  public Lister startList(String prefix, String closer) {
    Lister noob = new Lister(prefix, closer);
    noob.q = this;
    return noob;
  }

  /** append parenthesized list of columns */
  public QueryString catList(String prefix, Iterator<ColumnAttributes> cols, String closer) {
    try (QueryString.Lister pklister = startList(prefix, closer)) {
      cols.forEachRemaining(col -> pklister.append(col.name));
    }
    return this;
  }

  public QueryString catList(String prefix, String closer, ColumnAttributes... cols) {
    try (QueryString.Lister pklister = startList(prefix, closer)) {
      for (ColumnAttributes col : cols) {
        pklister.append(col.name);
      }
    }
    return this;
  }


  /** you can freely mix 'where' and 'and', this class internally chooses which verb to use based on state */
  public QueryString where() {
    if (whered) {
      return and();
    } else {
      whered = true;
      return cat(WHERE);
    }
  }

  /** you can freely mix 'where' and 'and', this class internally chooses which verb to use based on state */
  public QueryString and() {
    if (whered) {
      return cat(AND);
    } else {
      return where();
    }
  }

  public QueryString where(String columnish, Object valued) {
    where();
    nvPair(columnish, String.valueOf(valued));
    return this;
  }

  public QueryString quoted(String value) {
    if (value == null) {
      qMark();
    } else {
      cat("'");
      guts.append(value);
      cat("'");
    }
    return this;
  }

  public QueryString qMark() {
    return cat("?");
  }

  /** @returns this after appending "IN( @param list )" */
  public QueryString in(String list) {
    return cat(IN).Open().cat(list).Close();
  }

  public QueryString or() {
    return cat(OR);
  }

  public QueryString not() {
    return cat(NOT);
  }

  /** @returns this after appending "sum( @param str )" */
  public QueryString sum(String str) {
    return cat(SUM).Open().cat(str).Close();
  }


  /** @returns this after appending "in ( @param list )" adding commas as needed between list elements which are each quoted */
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

  /** @returns this after appending "in ( @param list )" adding commas as needed between list elements */
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

  public QueryString count(String it) {
    return cat(COUNT).Open().cat(it).Close();
  }

  public QueryString count(ColumnProfile cp) {
    return count(cp.fullName());
  }

  public QueryString max(String of) {
    return cat(MAX).Open().word(of).Close();
  }

  public QueryString max(ColumnProfile cp) {
    return max(cp.fullName());
  }

  public QueryString min(String of) {
    return cat(MIN).Open().word(of).Close();
  }

  public QueryString min(ColumnProfile cp) {
    return min(cp.fullName());
  }

  public QueryString as(String newname) {
    return cat(AS).word(newname);
  }

  //  CASE a WHEN 1 THEN 'one'
//         WHEN 2 THEN 'two'
//         ELSE 'other'
//  END
  public QueryString Decode(String field, String comparedTo, String ifEquals, String ifNot) {
//    return word(DECODE).Open().cat(field).comma().cat(comparedTo).comma().cat(ifEquals).comma().cat(ifNot).Close();
    return cat(CASE).cat(field).cat(WHEN).cat(comparedTo).cat(THEN).cat(ifEquals).cat(ELSE).cat(ifNot).cat(END);
  }

  /** @returns this after "VALUES (" */
  public QueryString Values() {
    return cat(VALUES).Open();
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

  /** @returns this after appending set value=? [,value=?] for each item in @param colnames */
  public QueryString prepareToSet(Iterable<String> colnames) {
    try (QueryString.Lister lister = startSet()) {
      colnames.forEach(lister::prepareSet);
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

  public QueryString comma() {
    guts.append(", ");
    return this;
  }

  public QueryString comma(String s) {
    return comma().cat(s);
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

  public QueryString word(String s) {
    space();
    guts.append(s);
    space();
    return this;
  }

  public QueryString comma(int s) {
    return comma().cat(s);
  }

  /** ensure there is a space at the end of the string */
  private QueryString space() {
    if (guts.length() > 0 && guts.charAt(guts.length() - 1) != ' ') {
      guts.append(' ');
    }
    return this;
  }

  /** append '(' @param s ')' with appropriate spaces */
  public QueryString parenth(String s) {
//    space();
    Open().cat(s).Close();
    space();
    return this;
  }

  public QueryString quoted(char c) {
    space();
    return value(c);
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

  public QueryString commaQuoted(String s) {
    return comma().value(s);
  }

  public QueryString commaQuoted(char c) {
    return comma().value(c);
  }

  public QueryString orderby(String columnName) {
    return cat(ORDERBY).cat(columnName);
  }

  /** @returns this after conditionally adding an orderby clause: ascending of @param orderly is positive, descending if negative */
  public QueryString orderby(String columnName, int orderly) {
    if (orderly > 0) {
      return orderby(columnName).cat(ASC);
    }
    if (orderly < 0) {
      return orderby(columnName).cat(DESC);
    }
    return this;
  }

//  public QueryString commaAsc(ColumnProfile next) {
//    return comma().cat(next.fullName()).cat(ASC);
//  }

  public QueryString orderbyasc(ColumnProfile first) {
    return orderby(first.fullName(), 1);
  }

  public QueryString orderbydesc(ColumnProfile first) {
    return orderby(first.fullName(), -1);
  }

  public QueryString orderbydesc(int columnFirst) {
    return orderby(String.valueOf(columnFirst), -1);
  }

  public QueryString orderbyasc(int columnFirst) {
    return orderby(String.valueOf(columnFirst), 1);
  }

//  public QueryString commaDesc(ColumnProfile next) {
//    return comma().cat(next.fullName()).cat(DESC);
//  }

  public QueryString having() {
    return cat(HAVING);
  }

  public QueryString groupby(ColumnProfile first) {
    return cat(GROUPBY).cat(first.fullName()); // --- possible cause of bugs
  }

  public QueryString nvPair(String field, Long value) {
    return word(field).cat(EQUALS).cat((value == null) ? NULL.spaced : String.valueOf(value));
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

  /** append  " field =?" */
  public QueryString nvPairPrepared(String field) {
    word(field);
    cat(EQUALS);
    qMark();
    return this;
  }

  /** append "field.name=?" */
  public QueryString prepared(ColumnAttributes field) {
    return nvPairPrepared(field.name);
  }

  public QueryString nvPair(String field, int value) {
    return nvPair(field, (long) value);
  }

  public QueryString nvPair(String field, long value) {
    return word(field).cat(EQUALS).value(value);
  }

  public QueryString nvcompare(ColumnProfile field, SqlKeyword cmpop, String value) {
    return word(field.fullName()).cat(cmpop).value(value);
  }

  public QueryString nvcompare(ColumnProfile field, SqlKeyword cmpop, long value) {
    return word(field.fullName()).cat(cmpop).value(value);
  }

  public QueryString isNull(String field) {
    return word(field).cat(IS).cat(NULL);
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

  @SuppressWarnings("unchecked")
  public QueryString andRange(ColumnProfile rangeName, ObjectRange strange, boolean closeEnd) {
    if (ObjectRange.NonTrivial(strange)) {
      and().range(rangeName, strange, closeEnd);
    }
    return this;
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

  @SuppressWarnings("unchecked")
  public QueryString orRange(ColumnProfile rangeName, ObjectRange strange, boolean closeEnd) {
    if (ObjectRange.NonTrivial(strange)) {
      or().range(rangeName, strange, closeEnd);
    }
    return this;
  }

  @SuppressWarnings("unchecked")
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

  public QueryString all() {
    return word("*");
  }

  public QueryString andRange(ColumnProfile rangeName, ObjectRange strange) {
    return andRange(rangeName, strange, true);
  }

  public QueryString union(QueryString two) {
    return cat(UNION).word(String.valueOf(two));
  }

  public QueryString using(ColumnProfile column) {
    // do NOT use Open(column), or it will put the table name in it (column.fullname()), which will blow it!
    return cat(USING).Open().cat(column.name()).Close();
  }

  public final QueryString castAsBigint(String what) {
    return castAs(what, BIGINT.toString());
  }

  // postgresql only
  public final QueryString limit(int n) {
    return cat(LIMIT).cat(n);
  }


  protected QueryString castAs(String what, String datatype) {
    //CAST(TXN.TRANSTARTTIME AS BIGINT)
    return cat(CAST).Open().cat(what).as(datatype).Close();
  }

  public Lister startSet() {
    lineBreak();
    return new Lister("SET ", "");
  }

  private Lister dupLister() {
    return new Lister(" ON DUPLICATE KEY UPDATE ", "");//strange but true, a list with invisible parens
  }

  /**
   * mysql quirk: while inserting fields into an insert statement record the names of non-pk fields in a TextList.
   * pass that TextList to this method and those fields will be updated with what otherwise was inserted
   *
   * @returns this
   */
  public QueryString OnDupUpdate(TextList nonPks) {
    lineBreak();
    try (Lister lister = dupLister()) {
      nonPks.iterator().forEachRemaining(lister::duplicateUpdate);
    }
    return this;
  }

  /**
   * mysql quirk: while inserting fields into an insert statement record the names of non-pk fields in a TextList.
   * pass that TextList to this method and those fields will be updated with what otherwise was inserted
   *
   * @returns this
   */
  public QueryString OnDupUpdate(Iterator<ColumnAttributes> cols) {
    lineBreak();
    try (Lister lister = dupLister()) {
      cols.forEachRemaining(lister::duplicateUpdate);
    }
    return this;
  }

  public QueryString OnDupUpdate(ColumnAttributes... cols) {
    lineBreak();
    try (Lister lister = dupLister()) {
      for (ColumnAttributes col : cols) {
        lister.duplicateUpdate(col.name);
      }
      return this;
    }
  }

  public QueryString from(TableInfo ti) {
    lineBreak();
    return cat(FROM).cat(ti.fullName());
  }

  /** suggested linebreak, @see indent for forceful one */
  public void lineBreak() {
    if (wrappy) {
      indent();
    }
  }

  public void wherePrepared(ColumnAttributes col) {
    where().prepared(col);
  }

  /** where clause for prepared statement */
  public QueryString whereColumns(ColumnAttributes... cols) {
    for (ColumnAttributes col : cols) {
      wherePrepared(col);
    }
    return this;
  }


  /** where clause for prepared statement */
  public QueryString whereColumns(Iterator<ColumnAttributes> cols) {
//only called 'where' once, then prepared many    cols.forEachRemaining(where()::prepared);
    while (cols.hasNext()) {
      wherePrepared(cols.next());
    }
    return this;
  }

  /** append prepared statement with a field for each member of list. */
  public QueryString whereList(ColumnAttributes col, int listSize) {
    where();
    word(col.name);
    cat(IN);
    try (Lister simple = new Lister("(")) {
      simple.append("?");
    }
    return this;
  }

  /** "Set [name=?]*" */
  public Lister prepareToSet(ColumnAttributes... cols) {
    QueryString.Lister lister = startSet();
    for (ColumnAttributes col : cols) {
      lister.prepareSet(col.name);
    }
    return lister;
  }

  public QueryString prepareValues(Iterator<ColumnAttributes> cols) {
    int marks;
    try (QueryString.Lister lister = startList("(")) {
      cols.forEachRemaining(col -> lister.append(col.name));
      marks = lister.counter;
    }
    try (QueryString.Lister lister = startList(" VALUES (")) {
      while (marks-- > 0) {
        lister.append("?");
      }
    }
    return this;
  }

  /**
   * {where|and} colname between ? and ? .
   * As always beware that BETWEEN is inclusive, and as such not suitable for a sliding window
   */
  public QueryString whereBetween(ColumnAttributes col) {
    return where().cat(col.name).cat(SqlKeyword.BETWEEN).qMark().cat(AND).qMark();
  }

  /**
   * @returns this after appending 'functioname('.
   * <br> mysql doesn't tolerate space between function name and opening paren
   */
  public QueryString function(SqlKeyword keyword) {
    guts.append(keyword.toString().trim());
    return Open();
  }


  /** creates a querystring fills out column name, sets up adding the from clause when you close the lister. */
  public static Lister Select(TableInfo ti, Iterator<ColumnAttributes> cols) {
    final QueryString qry = Select();
    final String closer = format("{0} {1}", FROM, ti.fullName());
    QueryString.Lister lister = qry.startList("", closer);
    cols.forEachRemaining(col -> lister.append(col.name));
    return lister;
  }

  public static QueryString Count(ColumnAttributes col) {
    return Select().function(COUNT).cat(col.name).Close();
  }

  /** @returns new query starting "delete from sch.tab" */
  public static QueryString DeleteFrom(TableInfo ti) {
    return new QueryString().cat(DELETE).cat(FROM).cat(ti.fullName());
  }

  /** @returns new query starting "insert  table sch.tab" */
  public static QueryString Insert(TableInfo ti) {
    return Insert().cat(ti.fullName());
  }

  /** create new query starting "insert " */
  public static QueryString Insert() {
    QueryString noob = new QueryString();
    return noob.cat(INSERT);
  }

  /** @returns new query starting with: "update schema.tablename " */
  public static QueryString Update(TableInfo ti) {
    return Clause(SqlKeyword.UPDATE.toString()).cat(ti.fullName());
  }

  /** create new query starting "select *" */
  public static QueryString SelectAll() {
    QueryString noob = new QueryString();
    return noob.cat(SELECT).all();
  }

  /**
   * Rules:
   * 1) If it contains any single quotes, escape them with another single quote
   * 2) Wrap it in single quotes
   */
  public static String Quoted(String s) {
    return StringX.singleQuoteEscape(s);
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

  /** @returns new {@link QueryString} starting with Select */
  public static QueryString Select() {
    return new QueryString(SELECT);
  }

  /** @returns new {@link QueryString} starting with "Select " then @param first */
  public static QueryString Select(QueryString first) {
    return Select().cat(first);
  }

  /** @returns new {@link QueryString} starting with "Select table.name " */
  public static QueryString Select(ColumnProfile cp) {
    return Select().cat(cp.fullName());
  }

  /** @returns new {@link QueryString} starting with "Select justnameNoTable." */
  public static QueryString Select(ColumnAttributes col) {
    return Select().cat(col.name);
  }

  /** @returns new {@link QueryString} starting with "Select distinct(columname)" */
  public static QueryString SelectDistinct(ColumnProfile cp) {
    return Select().cat(DISTINCT).cat(cp.fullName());
  }

  /** @returns new empty {@link QueryString} */
  public static QueryString Clause(String message, Object... clump) {
    QueryString clause = new QueryString();
    clause.cat(format(message, clump));
    return clause;
  }

  /** @returns New clause starting with Where column=false */
  public static QueryString WhereNot(ColumnProfile column) {
    return QueryString.Clause("").where().isFalse(column);
  }

  public static QueryString WhereIsTrue(ColumnProfile column) {
    return QueryString.Clause("").where().isTrue(column);
  }

  public QueryString self() {
    return this;
  }
  /** list builder aid. Now that this code is not inlined in many places we can test whether inserting a comma and then removing it later takes more time than checking for the need for a comma with each item insertion. */
  public class Lister extends ListWrapper {
    public QueryString q;

    /** begin a list with a comma as the closer */
    public Lister(String prefix) {
      this(prefix, ") ");
    }

    /** begin a list */
    public Lister(String prefix, String closer) {
      super(guts, prefix, closer);
      q = self();
    }

    /** begin a list with strange separator */
    public Lister(String prefix, String closer, String comma) {
      this(prefix, closer);
      super.comma = comma;
    }

    /** add a preparedStatement clause: "name=?" */
    public void prepareSet(String columnname) {
      append(format("{0}=?", columnname));
    }

    public void prepareSet(ColumnAttributes col) {
      append(format("{0}=?", col.name));
    }

    /** mysql: update clause of insert or update. */
    public void duplicateUpdate(String columname) {
      append(format("{0}=VALUES({0})", columname));
    }

    public void duplicateUpdate(ColumnAttributes col) {
      append(format("{0}=VALUES({0})", col.name));
    }

    public Lister appendList(String prefix, String closer) {
      append("");  //get's our comma and accounts for it
      return new Lister(prefix, closer);
    }

    public void append(ColumnAttributes col) {
      append(col.name);
    }

    public QueryString insertOrUpdate(ColumnAttributes... cols) {
      for (ColumnAttributes col : cols) {
        prepareSet(col);
      }
      close();//because 'this' doesn't conveniently get finalized before the parent query string gets used.
      return OnDupUpdate(cols);
    }

  }
}
