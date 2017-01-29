package pers.hal42.lang;

import pers.hal42.lang.ContentType;

public class ContentValid {
  public static final boolean legalChar( ContentType ctype, char c){
    switch(ctype.Value()){
      default:
      case ContentType.unknown: {
        return true; //no time to waste perfecting this
      }
      case ContentType.arbitrary: return true; //no constraints
      case ContentType.alphanum:  return Character.isSpaceChar(c)|| Character.isLetterOrDigit(c);
      case ContentType.decimal:   return Character.isDigit(c);
      case ContentType.hex:       return Character.digit(c,16)>=0;
      case ContentType.purealpha: return Character.isLetter(c);
    }
  }

  public static final boolean legalChars ( ContentType ctype , String proposed){
    for(int i = proposed.length();i-->0; ){
      if (!legalChar( ctype, proposed.charAt(i))){
        return false;
      }
    }
    return true; //must be all good to get here.
  }

  public static final boolean IsNumericType(ContentType ctype){
    switch(ctype.Value()){
      default:
        return false;
      case ContentType.decimal:
      case ContentType.money:
      case ContentType.ledger:
      case ContentType.cardnumber:
      case ContentType.date:
      case ContentType.time:
      case ContentType.zulutime:
      case ContentType.select:
        return true;
    }
  }

  public static final ContentType typeForCode(char mscode){
    return new ContentType(typecode(mscode));
  }

  protected static final int typecode(char mscode){
    switch(Character.toLowerCase(mscode)){
      case 'a': return ContentType.purealpha;
      case 'n': return ContentType.decimal;
      case 's': return ContentType.unknown; //+_+ need symbol type
      case 'x': return ContentType.arbitrary;
    }
    return ContentType.unknown;
  }

}

