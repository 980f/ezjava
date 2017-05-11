package pers.hal42.data;

/** buffer with ascii internal framing. */


import pers.hal42.lang.StringX;
import pers.hal42.text.Ascii;
import pers.hal42.text.Fstring;

class AsciiOverflowError extends IllegalArgumentException {
  int size;
  String digits;

  AsciiOverflowError(int size, String digits) {
    this.size = size;
    this.digits = digits;
  }

  public String toString() {
    return "AsciiOverflowError: [" + digits + "] has more than " + size +
        " significant places";
  }
}

public class AsciiBuffer extends Buffer {
  boolean vomitous; //throw exception rather than set error flag
  boolean smart; //try to clip fields that don't fit
  /**
   * @return this after appending @param text left justified blank extended to file @param size places.
   * if longer than given size trailing chars are truncated.
   * when smart trim before truncation
   * @todo trim leading then trailing to deal with overflow. (rather than all)
   */
  public AsciiBuffer appendAlpha(int size, String text) {
    if (text.length() > size) {
      if (vomitous) {
        throw new AsciiOverflowError(size, text);
      }
      if (smart) {
        text = text.trim(); //fairly stupid smartness
      }
    }
    append(Fstring.fill(text, size, ' '));
    return this;
  }

  /**
   * number type fixed length fields are UNSIGNED right justified zero extended
   * @todo  check digits for non-decimal characters, we have that somewhere....
   */
  public AsciiBuffer appendNumeric(int size, String digits) {
    digits = StringX.OnTrivial(digits, "0"); //not our job to complain about bad values
    int overflow = digits.length() - size;
    if (overflow > 0) { //try to strip leading zeroes
      String clip = digits.substring(0, overflow);
      for (int i = overflow; i-- > 0; ) {
        if (clip.charAt(i) != '0') {
          if (vomitous) {
            throw new AsciiOverflowError(size, digits);
          }
          else {
            anError();
            //without break; here we get a count of how many chars were a problem
          }
        }
      }
      append(digits.substring(overflow));
    }
    else {
      append(Fstring.righted(digits, size, '0'));
    }
    return this;
  }

  /**
   * @param fixedsize is space for value @param number is the value
   */
  public AsciiBuffer appendNumber(int fixedsize, int number) {
    appendNumeric(fixedsize, Integer.toString(number));
    return this;
  }

  public AsciiBuffer appendNumber(int fixedsize, long number) {
    return appendNumeric(fixedsize, Long.toString(number));
  }

  /**
   * @param fixedsize is space for value @param number is the value
   */
  public AsciiBuffer appendSigned(int fixedsize, int number) {
    if (number < 0) {
      number = -number;
      --fixedsize;
      append('-');
    }
    appendNumeric(fixedsize, Integer.toString(number));
    return this;
  }

  public AsciiBuffer appendSigned(int fixedsize, long number) {
    if (number < 0) {
      number = -number;
      --fixedsize;
      append('-');
    }
    return appendNumeric(fixedsize, Long.toString(number));
  }

  /**
   * append a record separator
   */
  public AsciiBuffer endRecord() {
    append(Ascii.RS);
    return this;
  }

  /**
   * append a frame separator
   */
  public AsciiBuffer endFrame() {
    append(Ascii.FS);
    return this;
  }

  /**
   * append @param howmany frame separators
   */
  public AsciiBuffer emptyFrames(int howmany) {
    while (howmany-- > 0) {
      append(Ascii.FS);
    }
    return this;
  }

  /**
   * append variable length @param ofBytes terminate with frame separator
   */
  public AsciiBuffer appendFrame(String ofBytes) {
    append(StringX.TrivialDefault(ofBytes, "").getBytes()); //+_+ need to force encoding
    append(Ascii.FS);
    return this;
  }

  /**
   * append an integer @param num with at least @param min digits.
   */
  public AsciiBuffer appendNumericFrame(long num, int min) {
    String number = Long.toString(num);
    int len = number.length();
    if (len < min) {
      appendNumeric(min, number); //zero fills
      append(Ascii.FS);
    }
    else {
      appendFrame(number);
    }
    return this;
  }

  /**
   * append an object using String.valueof() to get image.
   * @return this
   */
  public AsciiBuffer frame(Object obj) {
    String wtf=String.valueOf(obj);
    append(wtf);
    endFrame();
    return this;
  }

  /**
   * @param alloc is the MAXIMUM number of bytes. object does NOT stretch.
   */
  protected AsciiBuffer(int alloc) {
    super(alloc);
    vomitous = false;
    smart = true;
  }

  public static AsciiBuffer Newx(int alloc) {
    return new AsciiBuffer(alloc);
  }

  public Buffer Clone() {
    AsciiBuffer newone = new AsciiBuffer(this.alloc);
    newone.vomitous = vomitous;
    newone.smart = smart;
    return newone;
  }

}
//$Id: AsciiBuffer.java,v 1.10 2003/10/07 20:09:06 mattm Exp $
