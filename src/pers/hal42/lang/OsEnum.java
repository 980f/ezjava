package pers.hal42.lang;

public enum OsEnum {
  Linux,NT,Windows,Windows2000,SunOS;

  public int numValues(){
    return values().length;
  }

  /** shouldn't need stuff like this any more. */
  public static OsEnum from(int rawValue){
    return rawValue<values().length?values()[rawValue]:null;
  }

}

