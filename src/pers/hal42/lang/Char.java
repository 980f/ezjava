package pers.hal42.ext;

import pers.hal42.lang.Index;
import pers.hal42.lang.StringX;

/**
 * Created by andyh on 4/3/17.
 */
public class Char {
  public
  byte raw;

  public static boolean isPresent(String flags, byte flag) {
    return StringX.NonTrivial(flags) && flags.indexOf(flag) >= 0;
  } /* isPresent */

  /**
   * characters needing escaping in many situations, a larger set than either C or Java standards.
   * In the string below there are pairs of (escape code: escaped char) e.g. n\n
   * it looks confusing as some of the chars being escaped are unchanged by the process.
   * when removing a slash you search for the char and pass back the odd member of the pair
   * when adding slashes you find the char and emit a slash and the even member of the pair
   */
  static String SeaScapes =
    "a\u0001" +
      "b\b" +      //STX
      "c\0003" +   //ETX
      "f\f" +
      "n\n" +
      "r\r" +
      "t\t" +
      "v\u000B" +  //VT
      "\\\\" +
      "//" +
      "''" +
      "??" +       //might add '*' to this list, for escaping globs.
      "\"\"";


  boolean needsSlash() {
    return new Index(SeaScapes.indexOf(raw)).isOdd();//isOdd includes checking for valid.
  }

  /**
   * while named for one usage this works symmetrically
   */
  char slashee() {
    Index present = new Index(SeaScapes.indexOf(raw));
    if (present.isValid()) {
      return SeaScapes.charAt(1 ^ present.get());//xor with 1 swaps even and odd.
    } else {
      return (char) raw;
    }
  }


////////////////////////////////////

  boolean numAlpha() {
    return Character.isLetterOrDigit(raw) || isPresent("+-.", raw);
  }

  boolean startsName() {
    return Character.isJavaIdentifierStart(raw);
  }

  boolean isDigit() {
    return Character.isDigit(raw);
  }

  boolean isControl() {
    return Character.isISOControl(raw);
  }

  boolean isInNumber() {
    return isDigit() || in("+-.Ee");
  }

  boolean isWhite() {
    return Character.isWhitespace(raw);
  }

  boolean in(String tokens) {
    return isPresent(tokens, raw);
  }

  boolean isHexDigit() {//todo:1 use math instead of string search.
    return Character.isDigit(raw) || isPresent("aAbBcCdDeEfF", raw);
  }


  int hexDigit() {
    int trusting = (raw | 0x20) - '0';//tolowerthen subtract char for zero.
    if ((trusting > 9)) {
      trusting -= 39;
    }
    return trusting; //'A'-'0' = 17, want 10 for that
  }

  char hexNibble(int sb) {
    int nib = 15 & (raw >> (sb * 4)); //push to low nib
    return (char) ((nib > 9) ? ('A' + nib - 10) : '0' + nib);
  }

}
