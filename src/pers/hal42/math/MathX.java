package pers.hal42.math;

import static java.lang.Math.abs;

public class MathX {
  public static final int INVALIDINTEGER = Integer.MIN_VALUE; //NOT the same as invalid index
  public static final long INVALIDLONG = Long.MIN_VALUE;

  private MathX() {
    // don't instantiate me.  I am here for static purposes
  }

  /**
   * check for nearly equal, to deal with parsing of text yielding values that aren't the same as a later print.
   */
  public static boolean nearlyEqual(double value, double other) {
    if (value == other) {
      return true;//expeditious and a zero after this point is a denorm
    }
    if (value == 0.0) {
      return false;
    }
    double diff = value - other;
    return abs(diff) <= 2 * Math.ulp(value);
  }

  public static int signum(int signed) {
    return Integer.compare(signed, 0);
  }

  public static int signum(long signed) {
    return signed > 0 ? 1 : signed < 0 ? -1 : 0;
  }

  public static int signum(double signed) {
    return signed > 0 ? 1 : signed < 0 ? -1 : 0;
  }

  public static int signum(float signed) {
    return signed > 0 ? 1 : signed < 0 ? -1 : 0;
  }

  /**
   * @return exponent of log base 2 of @param
   * -1 rather than -infinity for 0.
   * test cases: 0=>-1 1=>0 2,3=>1 4,5,6,7=>2 8..15=>3
   */
  public static int log2(int num) {
    int log = -1;//rather than infinity
    if (num < 0) {//mostly do this to protect against shift being signed
      num = -num; //and 8000 is NOT a problem!
    }
    while (num != 0) {
      num >>= 1;
      ++log;
    }
    return log;
  }

  /**
   * @return a signed number extracted from a set of bits within an integer.
   * initially used by ncrA parsing.
   * doesn't deal with end before start (field crossing boundary)
   */
  public static int getSignedField(int datum, int start, int end) {
    return (datum << (31 - start)) & ~((1 << end) - 1);
  }

  /**
   * @return two nibbles from @param high and @param low ls digits in the ls byte of an int.
   * todo:2 move into a byte package!
   */
  public static int packNibbles(int high, int low) {
    return ((high & 15) << 4) + (low & 15);
  }

  // +++ why promote to only float, but then promote to double later?
  public static double ratio(int num, int denom) {
    return (denom == 0) ? 0 : ((double) num) / ((double) denom);
  }

  public static double ratio(long num, long denom) {
    return (denom == 0) ? 0 : ((double) num) / ((double) denom);
  }

  public static long percent(long dividend, long divisor) {
    return Math.round(ratio(dividend * 100, divisor));
  }

  // true of numerator.0 / denominator.0 is a whole number
  public static boolean /*whole*/multipleOf(int numerator, int denominator) {
    return (numerator % denominator) == 0;
  }

  /** @returns sign of difference of objects. Intended for values already boxed, else just use native compares. */
  public static int cmp(Number lefty, Number righty) {
    if (lefty instanceof Integer) {
      return signum(lefty.intValue() - righty.intValue());
    } else if (lefty instanceof Double) {
      return signum(lefty.doubleValue() - righty.doubleValue());
    } else if (lefty instanceof Long) {
      return signum(lefty.longValue() - righty.longValue());
    } else {
      return signum(lefty.floatValue() - righty.floatValue());
    }
  }
}
