package pers.hal42.lang;

/**
 * todo:2 rest of Char.cpp
 */

public class CharX {
  public static final char INVALIDCHAR = '\uFFFF';//todo:2 unicode can be more than 16 bits, java is behind the times on this.
  /**
   * left - right pairs.
   * the &; pair is for html escapes.
   */
  private static final String fillerPairs = "[]{}<>()&;";

  private CharX() {
    // I exist for static purposes only
  }

  /**
   * @return complementary character, if none then returns given char
   */
  public static char matchingBrace(char brace) {
    int at = fillerPairs.indexOf(brace);
    return at >= 0 ? fillerPairs.charAt(at ^ 1) : brace;
  }
}
