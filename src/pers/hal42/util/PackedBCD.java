package pers.hal42.util;

import pers.hal42.lang.StringX;

public class PackedBCD {
  private String bcd;

  public String toString() {
    return bcd;
  }

  /**
   * @return integer value of packed bcd number
   * empty string returns 0.
   */
  public long Value() {
    return parseString(bcd);
  }

  public static PackedBCD New(String bcd) {
    PackedBCD newone = new PackedBCD();
    newone.bcd = bcd;
    return newone;
  }

  public static int fromChar(char onebcd) {
    return onebcd & 15 + 10 * (onebcd >> 4);
  }

  public static long parseString(String bcd) {
    if (StringX.NonTrivial(bcd)) {
      long ell = 0;
      long power = 1;
      for (int i = bcd.length(); i-- > 0; ) {
        ell += power * fromChar(bcd.charAt(i));
        power *= 100;
      }
      return ell;
    } else {
      return 0;
    }
  }

}

