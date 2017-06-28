package pers.hal42.lang;

public enum OsEnum {
  Linux, Windows;  //removed obsolete OS's NT and Sun/Solaris

  public static int numValues() {
    return values().length;
  }

  /**
   * shouldn't need stuff like this any more.
   */
  public static OsEnum from(int rawValue) {
    return rawValue < values().length ? values()[rawValue] : null;
  }

}

