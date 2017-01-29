package pers.hal42.util;


public enum ColumnJustification  {
  CENTERED,JUSTIFIED, PLAIN,WINGED;

  public static ColumnJustification CopyOf(ColumnJustification rhs){//null-safe cloner
    return (rhs!=null)? new ColumnJustification(rhs) : new ColumnJustification();
  }
/** @return whether it was invalid */
  public boolean AssureValid(int defaultValue){//setto only if invalid
    if( ! isLegal() ){
       setto(defaultValue);
       return true;
    } else {
       return false;
    }
  }

}

