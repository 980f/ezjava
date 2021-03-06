package pers.hal42.data;
import pers.hal42.logging.ErrorLogStream;

/**
 * String content validator
 */
public class ContentValid {
  static final ErrorLogStream dbg = ErrorLogStream.getForClass(ContentValid.class);

  public static boolean legalChar(ContentType ctype, char c) {
    switch (ctype) {
      default:
      case unknown: {
        dbg.WARNING("Validating unknown field");
        return true; //no time to waste perfecting this
      }
      case arbitrary:
        return true; //no constraints
      case alphanum:
        return Character.isSpaceChar(c) || Character.isLetterOrDigit(c);
      case decimal:
        return Character.isDigit(c);
      case hex:
        return Character.digit(c, 16) >= 0;
      case purealpha:
        return Character.isLetter(c);
    }
  }

  public static boolean legalChars(ContentType ctype, String proposed) {
    for (int i = proposed.length(); i-- > 0; ) {
      if (!legalChar(ctype, proposed.charAt(i))) {
        dbg.WARNING("legalChars: Bad Char @{0}: {1}-->{2}", i, proposed.substring(0, i), proposed.substring(i));
        return false;
      }
    }
    return true; //must be all good to get here.
  }

  public static boolean IsNumericType(ContentType ctype) {
    switch (ctype) {
      default:
        return false;
      case decimal:
      case money:
      case date:
      case time:
      case zulutime:
      case select:
        return true;
    }
  }

  /** abbreviated access to most common types */
  protected static ContentType typecode(char mscode) {
    switch (Character.toLowerCase(mscode)) {
      case 'a':
        return ContentType.purealpha;
      case 'n':
        return ContentType.decimal;
      case 's':
        return ContentType.unknown; //+_+ need symbol type
      case 'x':
        return ContentType.arbitrary;
    }
    return ContentType.unknown;
  }

}
