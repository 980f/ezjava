package pers.hal42.lang;
// includes stringbuffer stuff.  Maybe can create a StringBufferX to put that stuff in.

public class StringX {
  private StringX() {
    // I exist for static reasons only
  }

  // +++ Need a StartsWith(...)

//  /**
//   * Takes an array of strings and makes one big string by putting ", " in between each.
//   * This function preserves the order of the items in the list.
//   */
//  public static final String commaDelimitedCat(String[] list) {
//    String all = ""; //+_+ use stringbuffer, mkaes less garbag
//    boolean first = true;
//    int len = list.length;
//    for (int i = 0; i < list.length; i++) {
//      all += ( (first) ? "" : ", ") + list[i];
//      first = false;
//    }
//    return all;
//  }

  // --- extremely inefficient.  improve!
  //alh has a state machine in some old C code, til then this is nicely compact source.
  public static String replace(String source, String toReplace,String with) {
    return replace(new StringBuffer(TrivialDefault(source, "")),TrivialDefault(toReplace, ""), TrivialDefault(with, ""));
  }

  public static String replace(StringBuffer source, String toReplace, String with) {
    boolean recurse = false;
    if (source == null) {
      return null;
    }
    if (!NonTrivial(toReplace) || toReplace.equals(with)) {
      return String.valueOf(source);
    }
    with = TrivialDefault(with, "");
    int lookLen = toReplace.length();
    int withLen = with.length();
    int searchFrom = 0;
    int foundAt = 0;
    while ( (foundAt = source.substring(searchFrom).indexOf(toReplace)) > -1) {
      int reallyAt = foundAt + searchFrom; //foundat is relative to searchFrom
      // +_+ improve this by dealing with cases separately (srcLen == repLen, srcLen < repLen, srcLen > repLen)
      source.delete(reallyAt, reallyAt + lookLen);
      source.insert(reallyAt, with);
      searchFrom = recurse ? 0 : reallyAt + withLen; // would recurse==true go infinite?  maybe
      //would only be infinite if with contains toReplace. would exahust memory if so.
    }
    return String.valueOf(source);
  }

  private static final String SINGLEQUOTE = "'";
  // Used by database stuff.  Don't change without testing on all databases!
  public static final String singleQuoteEscape(String s) {
    String ret = null;
    if (s != null) {
      if (s.indexOf(SINGLEQUOTE) != ObjectX.INVALIDINDEX) {
        s = replace(s, SINGLEQUOTE, "''"); // convert one single-quote into two
      }
      ret = SINGLEQUOTE + s + SINGLEQUOTE;
    }
    return ret;
  }

  private static final char SINGLEQUOTECHAR = '\'';
  // Used by database stuff.  Don't change without testing on all databases!
  public static final String unSingleQuoteEscape(String s) {
    String ret = null;
    if(NonTrivial(s) && (s.length() > 1)) {
      if((s.charAt(0) == SINGLEQUOTECHAR) && (s.charAt(s.length()-1) == SINGLEQUOTECHAR) ) {
        s = subString(s, 1, s.length()-1);  // ??? does this work correctly?
      }
      ret = replace(s, "''", SINGLEQUOTE); // convert two single-quotes into one
    }
    return ret;
  }

  /**
   * "arrname[","label" => "arrname[label]"
   * "arrname{","label" => "arrname{label}"
   * "" or null,"content" => " content "  i.e. default bracket char is space
   *
   */
  public static String bracketed(String brack, String content) {
    if (!NonTrivial(brack)) {
      brack = " ";
    }
    return brack + content + CharX.matchingBrace(lastChar(brack));
  }

  public static String bracketed(char brack, String content) {
    return String.valueOf(brack) + content + CharX.matchingBrace(brack);
  }

  public static String bracketed(String brack, char content) {
    return bracketed(brack, String.valueOf(content));
  }

  public static char lastChar(String s) {
    if (NonTrivial(s)) {
      return s.charAt(s.length() - 1);
    }
    else {
      return CharX.INVALIDCHAR;
    }
  }

  public static final String insert(String receiver, int where, String toinsert) {
    if ( (where != ObjectX.INVALIDINDEX) && (where < receiver.length())) {
      StringBuffer sb = new StringBuffer(receiver);
      sb.insert(where, toinsert);
      return sb.toString();
    }
    return receiver;
  }

  public static final int byteAt(String s, int at, int Default) {
    try {
      return 255& (int)s.charAt(at);
    }
    catch (Exception ex) {
      return Default;
    }
  }
  public static final int byteAt(String s, int at) {
    return byteAt(s,at,MathX.INVALIDINTEGER);
  }
  public static final int firstByte(String s) {
    return byteAt(s,0,MathX.INVALIDINTEGER);
  }
  public static final int lastByte(String s) {
    return byteAt(s,0,MathX.INVALIDINTEGER);
  }

  /**
   * @return first char if it exists else @param Default one provided
   */

  public static final char charAt(String s, int at, char Default) {
    try {
      return s.charAt(at);
    }
    catch (Exception ex) {
      return Default;
    }
  }

  public static final char charAt(String s, int at) {
    return charAt(s, at, (char) 0);
  }

  /**
   * @return first char if it exists else @param Default one provided
   */

  public static final char firstChar(String s, char Default) {
    return charAt(s, 0, Default);
  }

  /**
   * @return first char if it exists else space
   */
  public static final char firstChar(String s) {
    return firstChar(s, ' ');
  }

  /**
   * @return length of a string, ObjectX.INVALIDINDEX if null, 0 if trivial
   */
  public static final int lengthOf(String s) {
    return (s != null) ? s.length() : ObjectX.INVALIDINDEX;
  }

  /**
   * This either prepends [LEFT = true] or appends [LEFT = false] characters to extend a string to length,
   * or it truncates the string if it is too long.
   */
  public static final String fill(String source, char filler, int length,
                                  boolean left) {
    source = TrivialDefault(source, "");
    source.trim();
    if (length == source.length()) {
      return source;
    }
    int more = length - source.length();
    if (more < 1) {
      return source.substring(0, length); // +++ check
    }
    StringBuffer str = new StringBuffer(source);
    if (left) {
      for (int i = more; i-- > 0; ) {
        str.insert(0, filler);
      }
    }
    else {
      for (int i = more; i-- > 0; ) {
        str.append(filler);
      }
    }
    return String.valueOf(str);
  }

  public static final String subString(String s, int start) {
    return subString(s, start, (s != null) ? s.length() : start); // to prevent nullpointer exceptions
  }

  public static final String left(String s, int count) {
    return subString(s, 0, count); // to prevent nullpointer exceptions
  }

  public static final String right(String s, int length) { //need an Fstring variant...
    return tail(s, length);
  }

  /**
   * @return non-null string despite bad arguments.
   * Will make a string from as many chars as are available after the start, possibly 0.
   */
  public static final String subString(String s, int start, int end) {
    int length;
    if (s != null && start >= 0 && end > start && start < (length = s.length())) {
      return s.substring(start, Math.min(length, end));
    }
    else {
      return "";
    }
  }

  public static final String subString(StringBuffer s, int start, int end) {
    int length;
    if (s != null && start >= 0 && end > start && start < (length = s.length())) {
      return s.substring(start, Math.min(length, end));
    }
    else {
      return "";
    }
  }

  public static final String restOfString(String s, int start) {
    if (s != null && start >= 0 && start < s.length()) {
      return s.substring(start);
    }
    else {
      return "";
    }
  }

  public static final String tail(String s, int length) { //need an Fstring variant...
    int start = lengthOf(s) - length;
//System.err.println("tail ["+length+"]:"+s+" start="+start);
    return start > 0 ? restOfString(s, start) : s;
  }

  public static final String trim(String dirty) {
    return trim(dirty, true, true);
  }

  public static final String trim(String dirty, boolean leading,
                                  boolean trailing) {
    if (NonTrivial(dirty)) {
      int start = 0;
      int end = dirty.length();
      if (leading) {
        while (start < end) {
          if (Character.isWhitespace(dirty.charAt(start))) {
            ++start;
          }
          else {
            break;
          }
        }
      }
      if (trailing) {
        while (end-- > 0) {
          if (!Character.isWhitespace(dirty.charAt(end))) {
            break;
          }
        }
      }
      return subString(dirty, start, ++end);
    }
    return "";
  }

  public static final String proper(String toChange) {
    return subString(toChange, 0, 1).toUpperCase() +
        restOfString(toChange, 1).toLowerCase();
  }

  /**
   * @return an index safe for the second operand of string::subString, if the cutter
   * char is not found then we will cut at the end of the string.
   * @param s is a string that we wish to cut out a piece of
   * @param cutter is the separator character
   */
  public static final int cutPoint(String s, char cutter) {
    try {
      int cutat = s.indexOf(cutter);
      return cutat < 0 ? s.length() : cutat;
    }
    catch (Exception any) {
      return 0;
    }
  }

  private static final StringBuffer delete(StringBuffer sb, int start, int end) {
    //    if (start < 0)      throw new StringIndexOutOfBoundsException(start);
    //  if (start > end)      throw new StringIndexOutOfBoundsException();
    if (sb != null && start >= 0 && start <= end) {
      sb.delete(start, end);
    }
    return sb;
  }

  /** simple minded string parser
   *  @param parsee words get removed from the front of this
   *  @return the next contiguous non-white character grouping.
   */
  public static final String cutWord(StringBuffer parsee) {
    String retval = ""; //so that we can easily read past the end without getting nulls
    if (NonTrivial(parsee)) {
      int start = 0;
      int end = parsee.length();
      while (start < end) {
        if (Character.isWhitespace(parsee.charAt(start))) {
          ++start;
        }
        else {
          break;
        }
      }
      int cut = start + 1;
      while (cut < end) {
        if (!Character.isWhitespace(parsee.charAt(cut))) {
          ++cut;
        }
        else {
          break;
        }
      }
      retval = subString(parsee, start, cut);
      delete(parsee, 0, cut); //removes initial separator too
    }
    return retval;
  }

  ///////////////////////////////////////////////
  public static final boolean NonTrivial(String s) {
    return s != null && s.length() > 0;
  }

//  public static final boolean hasSubstance(String s) {
//    return s != null && s.trim().length() > 0;
//  }

  public static final boolean NonTrivial(StringBuffer sb) {
    return sb != null && NonTrivial(String.valueOf(sb));
  }

  public static final String TrivialDefault(Object o) {
    return TrivialDefault(o, null);
  }

  public static final String TrivialDefault(Object o, String def) {
    def = TrivialDefault(def, "");
    return ObjectX.NonTrivial(o) ? String.valueOf(o) : def;
  }

  /**
   * abysmally named, the one argument isn't tested for being equal to ALL of the strings
   * how about "equalsAny"  or "memberOf"?
   * @param one a string
   * @param two a set of strings
   * @param ignoreCase how to compare
   * @return which string matches, or INVALIDINDEX
   */
  public static final int equalStrings(String one, String[] two,
                                       boolean ignoreCase) {
    if (two != null) {
      for (int i = 0; i < two.length; i++) {
        if (equalStrings(one, two[i], ignoreCase)) {
          return i;
        }
      }
    }
    return ObjectX.INVALIDINDEX;
  }

  public static final int equalStrings(String one, String[] two) {
    return equalStrings(one, two, false);
  }

  public static final boolean equalAnyStrings(String one, String [ ] two) {
    return equalAnyStrings(one, two, false);
  }

  public static final boolean equalAnyStrings(String one, String [ ] two, boolean ignoreCase) {
    return equalStrings(one, two, ignoreCase) != ObjectX.INVALIDINDEX;
  }

  public static final boolean equalStrings(String one, String two) {
    return equalStrings(one, two, false);
  }

  public static final boolean equalStrings(String one, String two,
                                           boolean ignoreCase) {
    if (NonTrivial(one)) {
      if (NonTrivial(two)) {
        return ignoreCase ? one.equalsIgnoreCase(two) : one.equals(two);
      }
      else {
        return false;
      }
    }
    else {
      return!NonTrivial(two); //both trivial is a match
    }
  }

  public static int findIndex(String[] a, String o) {
    for (int i = a.length; i-- > 0; ) {
      if (equalStrings(a[i], o)) { // this is safer than the above !!!!
        return i;
      }
    }
    return ObjectX.INVALIDINDEX; // insert at the end
  }

  /**
   * trivial strings are sorted to start of list?
   */
  public static final int compareStrings(String one, String two) {
    if (NonTrivial(one)) {
      if (NonTrivial(two)) {
        return one.compareTo(two);
      }
      else {
        return -1;
      }
    }
    else {
      return +1;
    }
  }

  /** enhanced TrivialDefault
   * @return a string that is not zero length
   * @param s proposed string, usually from an untrusted variable
   * @param defaultReplacement fallback string, usually a constant
   */
  public static final String OnTrivial(String s, String defaultReplacement) {
    //this would of course be an infinite loop if the constant below were changed to be trivial...
    return NonTrivial(s) ? s : OnTrivial(defaultReplacement, " ");
  }

  public static final int OnTrivial(String s, int defaultvalue) {
    return NonTrivial(s) ? parseInt(s) : defaultvalue;
  }

  public static final boolean OnTrivial(String s, boolean defaultvalue) {
    return NonTrivial(s) ? Bool.For(s) : defaultvalue;
  }

  public static final String unNull(String s) {
    return TrivialDefault(s, ""); // kinda redundant
  }

  public static final String TrivialDefault(String s, String defaultReplacement) {
    return NonTrivial(s) ? s : defaultReplacement;
  }

  public static final String clipEOL(String s, String EOL) {//final eol is undesirable, clip it off.
    String returner = unNull(s);
    if(returner.length() > 0) {
      int index = returner.lastIndexOf(EOL);
      if(index == (returner.length()-EOL.length())) {
        returner = returner.substring(0, index);
      }
    }
    return returner;
  }

  public final static String removeCRs(String source) {
    return removeAll("\r", source);
  }

  public final static String removeAll(String badchars,String source){
    if(!NonTrivial(badchars)) {
      return source;
    }
    StringBuffer copy=new StringBuffer(source.length());
    for(int i=0;i<source.length();i++){
      char c=source.charAt(i);
      if(badchars.indexOf(c)<0){
        copy.append(c);
      }
    }
    return String.valueOf(copy);
  }

  protected static final String [][] replacers = {
      { "\\\\", "\\" },
        { "\\b", "\b" },
        { "\\t", "\t" },
        { "\\n", "\n" },
        { "\\f", "\f" },
        { "\\\"", "\"" },
        { "\\r", "\r" },
      { "\\'", "\'" },
  };

  public static final String unescapeAll(String source) {
    if(NonTrivial(source)) {
      for(int i = replacers.length; i-->0;) {
        source = replace(source, replacers[i][0], replacers[i][1]); // not really efficient, but a kludge anyway ---
      }
    }
    return source;
  }

  public static String afterLastDot(String dottedName) {
    return afterLast(dottedName, '.');
  }

  public static String afterLast(String dottedName, char thing) {
    return subString(dottedName, dottedName.lastIndexOf(thing) + 1);
  }

  private static int backSpace(StringBuffer buf){
    if(buf!=null){
      int tokill=buf.length()-1;
      if(tokill>=0){
        int ret=buf.charAt(tokill);
        buf.setLength(tokill);
        return ret;
      }
    }
    return MathX.INVALIDINTEGER;
  }

  public static final int parseInt(String s){//default radix 10
    return parseInt(s,10);
  }

  /** java.lang.Long.parseLong throws spurious exceptions,
  *  and doesn't accept unsigned hex numbers that are otherwise acceptible to java.
  */
  public static final long parseLong(String s,int radix){
    long acc=0;
    boolean hadSign=false;
    if(NonTrivial(s)){
      s=s.trim();
      int numchars=lengthOf(s);

      if( numchars<=0 || radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
        return 0;
      }

      int nextChar=0;
      if(s.charAt(nextChar)=='-'){
        ++nextChar;
        hadSign=true;
      }

      if(s.charAt(nextChar)=='+'){//tolerate signed strings.
        ++nextChar;
      }

      while(nextChar<numchars){
        int digit = Character.digit(s.charAt(nextChar++),radix);
        if(digit<0){
          break; //give em what we got so far
        }
        acc*=radix;
        acc+=digit;
        /* could throw overflow:
        if(acc<0){//marginal overflow)
          if(!hadSign){
            hadSign=true;
            acc=80000...000 - acc;
          } else {
            real overflow...
          }
        }
        */
      }
      return hadSign? -acc : acc;
    }
    return 0;
  }

  public static final long parseLong(String s){//default radix 10
    return parseLong(s,10);
  }

  public static final double parseDouble(String s){
    return parseDouble(s,0.0);
  }

  public static final double parseDouble(String s,double onerror){
    try {
      return Double.parseDouble(s);
    } catch (Exception caught){
      return onerror;      //be silent on errors
    }
  }

  public static final int parseInt(String s,int radix){
    return (int) (parseLong(s,radix) & 0xFFFFFFFF) ;
  }
}
