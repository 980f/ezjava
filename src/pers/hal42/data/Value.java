/**
* Title:        Value
* Description:
* Copyright:    2000 PayMate.net
* Company:      paymate
* @author       paymate
* @version      $Id: Value.java,v 1.4 2001/11/03 00:19:55 andyh Exp $
*/
package pers.hal42.data;

public class Value {//support for editing typed content
/*
 to simplify getting the users of this class hierarchy operational
 we have stubbed what will eventually be abstract members so that we
 can use this base class as a placeholder for NYI extended classes.
 */

  public ContentType charType(){
    return ContentType.unknown;
  }

  public String Image(){//stub
    return "BaseValueError";
  }

  public long asLong(){//override when it makes sense
    return 0L;
  }

  public int asInt(){//override when it makes sense
    return 0;
  }

  public boolean asBoolean(){//override when it makes sense
    return false;
  }

  public boolean setto (String image){//stub
    return false;
  }

  public void Clear(){//stub
    return;
  }

  public boolean acceptChar(char keyStroke){
    return ContentValid.legalChar( charType(), keyStroke);
  }

  public boolean acceptString(String proposed){
    return ContentValid.legalChars(charType(), proposed) && setto(proposed);
  }
  /**
   * @param mask rules:A alpha N number S symbol X anything ucase==required lcase==optional
   * @return true if the value is compatible with the mask
   */
  public static boolean fitsMask(String value, String mask){
    char ch;
    char type;
    int scanner;
    if(value.length()>mask.length()){
      return false; //string too long
    }

    int stop=value.length();
    for(scanner=0;scanner<stop;scanner++){
      ch=value.charAt(scanner);
      type=mask.charAt(scanner);
      if(!ContentValid.legalChar(ContentValid.typecode(type),ch)){
        return false;
      }
    }
    //remaining mask chars must all be optional
    while(scanner<mask.length()){
      if(Character.isUpperCase(mask.charAt(scanner))){//ucase flags mandatory
        return false;
      }
    }
    return true; //passed all tests
  }

}
//$Id: Value.java,v 1.4 2001/11/03 00:19:55 andyh Exp $
