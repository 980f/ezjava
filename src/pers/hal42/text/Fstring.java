package pers.hal42.text;

import pers.hal42.lang.CharX;
import pers.hal42.lang.StringX;

public class Fstring {
  //<quickie> patch for formerly lossy justification
  public boolean newlining = true;//originally this was false and data could be lost
  public char targetNewline = '\n';//and this may NOT be the system's newline
  protected StringBuffer buffer;
  protected int length; //separate from buffer as buffer sometimes gets mangled
  protected char filler;
  //the above is far more common than anything else..
  //</quickie>


  public Fstring(int len, char fillChar, String initializer) {
    filler = fillChar;
    buffer = new StringBuffer(length = len);
    setto(initializer);
  }

  public Fstring(int len, char fillChar) {
    this(len, fillChar, "");
  }

  public Fstring(String initializer) {
    this(initializer.length(), ' ', initializer);
  }

  public Fstring(int len) {
    this(len, ' ');
  }

  private Fstring() {
    //illegal
  }

  public Fstring centered(String newtext) {
    int newlen = newtext.length();
    if (newlen > length) {//keep only the front end
      buffer = new StringBuffer(newtext.substring(0, length));
    } else {
      int endfill = (length - newlen) / 2;
      int prefill = length - (newlen + endfill);
      buffer = new StringBuffer(length);
      while (prefill-- > 0) {
        buffer.append(filler);
      }
      buffer.append(newtext);

      while (endfill-- > 0) {
        buffer.append(CharX.matchingBrace(filler));
      }
    }
    return this;
  }

  protected boolean filled(int gap) {
    if (gap >= 0) {//keep only the rear end
      while (gap-- > 0) {
        buffer.append(filler);
      }
      return true;
    } else {
      return false;
    }
  }

  public Fstring righted(String newtext) {
    buffer = new StringBuffer(length);
    int gap = length - newtext.length();
    if (newtext == null) {
      newtext = "";
    }
    if (filled(gap)) {
      buffer.append(newtext);
    } else { //keep only the rear end
      buffer.append(newtext.substring(-gap));
    }
    return this;
  }

  public Fstring setto(String newtext) {
    if (newtext == null) {
      return this;
    }
    int newlen = newtext.length();
    if (newlen > length) {//keep only the front end
      buffer = new StringBuffer(newtext.substring(0, length));
    } else {
      buffer = new StringBuffer(newtext);
      buffer.ensureCapacity(length); //for efficiency
      while (newlen++ < length) { //but we don't want to trust setLength's filler so:
        buffer.append(filler);
      }
    }
    return this;
  }

  public Fstring setto(String lhs, String rhs) {
    lhs = StringX.TrivialDefault(lhs, "");
    rhs = StringX.TrivialDefault(rhs, "");
    int gap = length - (lhs.length() + rhs.length());

    if (gap >= 0) {
      buffer = new StringBuffer(lhs);
      buffer.ensureCapacity(length); //for efficiency
      filled(gap);//return is always true...
      buffer.append(rhs);
    } else {
      if (newlining) {
        buffer = new StringBuffer(lhs);
        buffer.ensureCapacity(1 + length); //for efficiency,newline and fullwidth
        buffer.append(targetNewline);
        if (filled(length - rhs.length())) {
          buffer.append(rhs);
        } else {
          buffer.append(StringX.subString(rhs, 0, length));
        }
      } else { //gotta truncate one of them...
        gap = length - rhs.length();//reusing gap for 'lefthand space available'
        buffer = new StringBuffer(StringX.subString(rhs, 0, length));//preserve leftpart of **rhs**
        if (gap >= 0) {
          buffer.insert(0, lhs.substring(0, gap));
        }
      }
    }
    return this;
  }

  public String toString() {
    return String.valueOf(buffer);
  }

  /**
   * todo: null s give null fstring. need to fix this. Til then use "".
   */
  public static String fill(String s, int len, char fillChar) {
    return String.valueOf(new Fstring(len, fillChar, s));
  }

  public static String centered(String s, int len, char fillChar) {
    return String.valueOf(new Fstring(len, fillChar).centered(s));
  }

  /**
   * hook
   * someday will perhaps have mirror imaged chars automatically handled by fstring
   */
  public static String winged(String s, int len) {
    return String.valueOf(new Fstring(len, '>').centered(" " + s + " "));
  }

  public static String righted(String s, int len, char fillChar) {
    return String.valueOf(new Fstring(len, fillChar).righted(s));
  }

  public static String zpdecimal(int value, int length) {
    return righted(Integer.toString(value), length, '0');
  }

  public static String justified(int len, String lhs, String rhs, char fillChar) {
    return String.valueOf(new Fstring(len, fillChar).setto(lhs, rhs));
  }

  public static String justified(int len, String lhs, String rhs) {
    return justified(len, lhs, rhs, '.');
  }

}

