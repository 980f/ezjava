package pers.hal42.stream;

public enum ContentType {
  arbitrary,
  purealpha,
  alphanum,
  password,
  decimal,
  hex,
  money,
  date,
  time,
  zulutime,
  //  taggedset,
//  select,
  unknown;

  public boolean IsNumericType() {
    switch (this) {
      default:
        return false;
      case decimal:
      case money:
      case date:
      case time:
      case zulutime:
//      case select:
        return true;
    }

  }

  public boolean legalChar(char c) {
    switch (this) {
      default:
      case unknown: {
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

  public boolean legalChars(String proposed) {
    for (int i = proposed.length(); i-- > 0; ) {
      if (!legalChar(proposed.charAt(i))) {
        return false;
      }
    }
    return true; //must be all good to get here.
  }

  public static ContentType typecode(char mscode) {
    switch (Character.toLowerCase(mscode)) {
      case 'a':
        return purealpha;
      case 'n':
        return decimal;
      case 's':
        return unknown; //+_+ need symbol type
      case 'x':
        return arbitrary;
    }
    return ContentType.unknown;
  }


}
