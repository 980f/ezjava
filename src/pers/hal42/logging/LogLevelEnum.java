package pers.hal42.logging;

import pers.hal42.lang.StringX;

/**
 * loggin threashold is an arbitrary in, but there are common levels defined here
 */
public enum LogLevelEnum {
  OFF(0),
  TRACE(5),
  VERBOSE(10),
  WARNING(20),
  ERROR(30),
  FATAL(99),;
  public final int level;

  LogLevelEnum(int level) {
    this.level = level;
  }

  /** all lle's have a level, but not all levels have an lle */
  public static String asString(int level) {
    for (LogLevelEnum lle : values()) {
      if (lle.level == level) {
        return lle.toString();
      }
    }
    return Integer.toString(level);
  }

  /**
   * @ return integer value of @param image, if it isn't a known enum value then see if it is the image of a number
   */
  static int levelFor(String image) {
    try {
      LogLevelEnum knownlevel = LogLevelEnum.valueOf(image);
      return knownlevel.level;
    } catch (Exception ignored) {
      return StringX.parseInt(image);
    }
  }
}
