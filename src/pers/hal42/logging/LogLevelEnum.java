package pers.hal42.logging;

import pers.hal42.lang.StringX;

public enum LogLevelEnum {
  OFF,
  VERBOSE,
  WARNING,
  ERROR,
  FATAL,
    ;
  /** @ return integer value of @param image, if it isn't a known enum value then see if it is the image of a number */
  int indexOf(String image){
    try {
      LogLevelEnum knownlevel = LogLevelEnum.valueOf(image);
      return knownlevel.ordinal();
    } catch (Exception ignored){
      return StringX.parseInt(image);
    }
  }
}
