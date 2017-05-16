package pers.hal42.math;

public class BaseConverter {

  //easier for debug to have this here.renamed as we don't like two vars that differ
  //only in capitolization.
  protected static final String symbolList = "0123456789abcdefghijklmnopqrstuvwxyz";
  //american convention, needs internationalization: +_+
  protected static final char radixChar = '.';
  // or use a TrueEnum (not necessary)
  protected static final int signum = 0;
  protected static final int prespace = 1;
  protected static final int digitsbefore = 2;
  protected static final int radixplace = 3;
  protected static final int trailingzero = 4;
  protected static final int digitsafter = 5;
  protected static final int trailingspace = 6;

  public static String dtoa(double d) {
    return dtoa(d, 0);
  }

  public static String dtoa(double d, int mindigits) {
    return dtoa(d, mindigits, true);
  }

  public static String dtoa(double d, int mindigits, boolean doRounding) {
    return dtoa(d, mindigits, doRounding, 10);
  }

  /* print with minimum precision: */
  public static String dtoa(double value, int mindigits, boolean doRounding, int base) {
    if (!dtoaBaseOk(base)) {
      return "0#NaN"; //really need to find the official NaN's
    }

//System.out.println("Converting " + value + " to base " + base);

    double radix = base; //so we don't do a zillion int to floats
    //do sign/magnitude split
    boolean isNegative = value < 0;
    value = Math.abs(value);
    double lsd = Math.pow(radix, -mindigits);//value of least significant digit
    if (doRounding) {
      value += lsd / 2; //already made value positive so we can ignore the sign here
    }
    int numToLeft = dtoaDigits(value, radix); //that is digits to left of radix point

    //presume mindigits is >=0, will fixup later if not
    int numChars = (numToLeft > 0 ? numToLeft : 1) //{integer part's digits or leading zero
      + (mindigits > 0 ? mindigits + 1 : 0)          //fraction's digits, plus '.'
      + (isNegative ? 1 : 0)         //for '-'
      ;   //}

    int leadingspaces = (numToLeft <= 0) ? 1 : 0; //leading 0 on fractions
    if (mindigits < 0) {  //then we have a multiple of 10 or 100 or 1000...
      if (numToLeft < -mindigits) {
        leadingspaces = -mindigits - numToLeft;
      }
    }

    double shifter = Math.pow(base, -numToLeft);  //negate here so we can ...
    double remainder = value * shifter;     //...mpy instead of divide here
    //the msd is now just after the radix point in remainder

    int oss = signum; //state machine starts with sign cursor

    char retval[] = new char[numChars];
    int i = 0;
    for (i = 0; i < numChars; i++) {
      switch (oss) {
        case signum: {
          if (isNegative) {
            retval[i] = '-';
          } else {
            --i;   //the only state where we do NOT generate a character
          }
          oss = (leadingspaces > 0) ? prespace : digitsbefore;
        }
        break;

        case prespace: { //at least one to get us here
          retval[i] = ((--leadingspaces) > 0) ? ' ' : '0';
          if (leadingspaces == 0) {
            if (numToLeft > 0) {
              oss = digitsbefore;
            } else {
              oss = mindigits > 0 ? radixplace : trailingspace; //+_+
            }
          }
        }
        break;

        case digitsbefore:
          // these are combined for ease of use (will it work?)
        case digitsafter: {
          remainder *= radix;
          int digit = (int) Math.floor(remainder);
          remainder -= digit;
          retval[i] = symbolList.charAt(digit);
          if (oss == digitsbefore) {
            if (--numToLeft == 0) {
              oss = mindigits > 0 ? radixplace : trailingspace;
            }
          } else { //digitsafter
            if (--mindigits == 0) {
              oss = trailingspace;
            }
          }
        }
        break;

        case radixplace: {
          retval[i] = radixChar;
          if (numToLeft < 0) {
            oss = trailingzero;
          } else {
            oss = mindigits > 0 ? digitsafter : trailingspace;
          }
        }
        break;

        case trailingzero: {
          retval[i] = '0';
          --mindigits;
          if (++numToLeft >= 0) {
            oss = digitsafter;
          }
        }
        break;

        case trailingspace: {
          retval[i] = ' ';
        }
        break;
      }
    }
    if (value == 30.0 && base == 10) {  // this was a problem in the past
      // +++ barf errors -- dtoadebug.dbg(dbg_INFO, "%f == %s?", value, retval.c_str());
    }
    return new String(retval);
  }


  //for numbers that are really an integer, but were using double to get more than 32 bits:
  public static String itoa(double d) {
    return itoa(d, 10);
  }

  public static String itoa(double value, int base) {
    if (value < 1.0) { //or take sign then fabs then still do this.
      return "0";
    }
    return dtoa(value, 0, true/*round it*/, base);
  }

  //dtoa co-functions:
  protected static boolean dtoaBaseOk(int base) { //compare to max and min args
    return (base > 1) && (base <= symbolList.length());
  }

  protected static int dtoaDigits(double value) {
    return dtoaDigits(value, 10.0);
  }

  protected static int dtoaDigits(double value, double radix) {
    double logBased = Math.log(Math.abs(value)) / Math.log(radix); //any log function will do
    //value== radix^power is a problem. if logbased is exactly an integer add one
    //  logBased+=.0000000000001; //cheap fix, not perfect but pretty good
    return 1 + (int) Math.floor(logBased);
  }

  /**
   * alh believes these can replace dtoaDigits, and run tremendously faster
   */


  public static int DigitCount(int value, int radix) {
    if (value != MathX.INVALIDINTEGER) {
      IntegralPower igp = IntegralPower.Above(value, radix);
      if (!igp.overflowed) {
        return igp.exponent;
      }
    }
    return MathX.INVALIDINTEGER;
  }

  public static int DigitCount(int value) {
    return DigitCount(value, 10);
  }

  public static double cvtBased(char ascii[], int base) {
    if (ascii != null) {
      boolean sawdp = false; //haven't seen a decimal point yet
      boolean isnegative = false;
      double retval = 0;
      double radix = base; //to make for a single timely conversion
      double dm = 0; //digit multiplier
      boolean stillwhite = true;
      for (char c : ascii) {
        if (c == '-') {
          isnegative = true;
          continue;
        }
        //testing for '-' before white space allows spaces after sign
        if (stillwhite) {
          if (c == ' ' || c == '\t') { //very limited definition of 'white'
            continue; //and we are still white
          } else {
            stillwhite = false;
          }
        }

        if (c == radixChar) {
          sawdp = true;
          dm = 1 / radix;
        } else {
          int d = symbolList.indexOf(Character.toLowerCase(c));
          if (d >= 0) {
            if (sawdp) {
              retval += d * dm;
              dm /= radix;
            } else {
              retval *= radix;
              retval += d;
            }
          } else {
            //we stop at first unknown char
            break;
          }
        }
      }
      return isnegative ? -retval : retval;
    } else {
      return 0;
    }
  }

  /*
  ******************************************************************************
  * FUNCTION....:	strBaseCvt( oldvalue, oldbase, newbase )
  * PARAMETERS..:	oldvalue (C) - 	the value to be converted
  *				        oldbase (N) - 	the base that the oldvalue is in (2-36)
  *				        newbase (N) - 	the base that the newvalue is in (2-36)
  *								ie: the base you are converting to
  * RETURNS.....:	the new value calculated, null on error
  * ABSTRACT....:	converts a text-string number from an old base to a new base.
  ******************************************************************************
  */
  public static String strBaseCvt(String oldvalue, int oldbase, int newbase) {
    if ((oldbase < 2) || (oldbase > 36)) {
      return null;
      //return "Old base [" + oldbase + "] must be between 2 and 36.";
    }
    if ((newbase < 2) || (newbase > 36)) {
      return null;
      //return "New base [" + newbase + "] must be between 2 and 36.";
    }
    oldvalue = oldvalue.trim().toUpperCase();
    int valuelen = oldvalue.length();
    long value = 0;
    String basestr = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    long loc;

    // decode
    // +++ change this to not use doubles or longs,
    // +++ but instead to just use arrays of bytes or something
    for (int i = 0; i < valuelen; i++) {
      int pos = basestr.indexOf(oldvalue.charAt(i));
      value = value * oldbase + pos;
    }

    // +++ this all needs to be fixed.  no time right now

    // encode
    loc = 1;
    while ((1 - (value / loc)) < 0) {
      loc *= newbase;
    }
    StringBuffer newvalue = new StringBuffer(newbase * oldvalue.length() / oldbase + 10);
    while (loc >= 1) {
      newvalue.append(((value >= loc) ?
        basestr.charAt((int) (Math.floor((value * 1.0) / loc))) :
        '0'));
      value %= loc;
      loc /= newbase;
    }
    // +++ fix this later.  For now, strip leading 0's:
    if (newvalue.charAt(0) == '0') {
      newvalue.deleteCharAt(0);
    }
    return String.valueOf(newvalue);
  }
}
