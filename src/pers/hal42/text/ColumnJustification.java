package pers.hal42.text;


public enum ColumnJustification {
  PLAIN,
  /**
   * distribute excess whitespace where whitespace exists
   */
  JUSTIFIED,
  /**
   * excess space split between the start and end
   */
  CENTERED,
  /**
   * like centered, but with tangible mirro symmetric filler in the extra space
   */
  WINGED

  //enums are copyable
//  public static ColumnJustification CopyOf(ColumnJustification rhs){//null-safe cloner
//    return (rhs!=null)? new ColumnJustification(rhs) : new ColumnJustification();
//  }

///** @return whether it was invalid */
//  public boolean AssureValid(int defaultValue){//setto only if invalid
//    if( ! isLegal() ){
//       setto(defaultValue);
//       return true;
//    } else {
//       return false;
//    }
//  }

}

