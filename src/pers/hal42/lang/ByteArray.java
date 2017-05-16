package pers.hal42.lang;

import pers.hal42.text.Ascii;

import java.util.Arrays;

public class ByteArray {
  public final static int bytesperlong = 8;

  private ByteArray() {
    // I exist for static purposes
  }

  /***
   * @return byte array init'ed with @param numbytes lsbytes of long
   */

  public static byte[] bytesOfLong(long ell, int numbytes) {
    byte[] retval = new byte[numbytes];
    while (numbytes-- > 0) {
      retval[numbytes] = (byte) (ell & 255);
      ell >>= 8;
    }
    return retval;
  }

  /**
   * create new array inserting byte @param toinsert into @param src byte array at @param offset cursor
   */
  public static byte[] insert(byte[] src, int toinsert, int offset) {
    byte[] target;
    if (src != null) {
      if (offset <= src.length) {
        target = new byte[src.length + 1];
        System.arraycopy(src, 0, target, 0, offset);
        System.arraycopy(src, offset, target, offset + 1, src.length - offset);
      } else { //stretch until new offset is a legal position.
        target = new byte[offset + 1];
        System.arraycopy(src, 0, target, 0, src.length);
        //new byte[] will have filled the rest with 0.
      }
      target[offset] = (byte) toinsert;
    } else {
      target = new byte[1];
      target[0] = (byte) toinsert;
    }
    return target;
  }

  public static byte[] insert(byte[] src, int toinsert) {
    return insert(src, toinsert, 0);
  }

  // some byte stuff
  public static byte[] newBytes(int length, byte filler) {
    byte[] bytes = new byte[length];
    return fillBytes(bytes, filler);
  }

  // great for erasing passwords
  public static byte[] fillBytes(byte[] bytes, byte filler) {
    for (int i = bytes.length; i-- > 0; ) {
      bytes[i] = filler;
    }
    return bytes;
  }

  /**
   * @param end not included. end=length+start, start is a position.
   */
  public static byte[] subString(byte[] s, int start, int end) {
    int length;
    if (s != null && start >= 0 && end > start && start < (length = s.length)) {
      if (end > start + length) {
        end = length;
      }
      length = end - start;
      if (length > 0) {
        byte[] sub = new byte[length];
        System.arraycopy(s, start, sub, 0, length);
        return sub;
      }
    }
    return new byte[0];
  }

  public static byte[] subString(byte[] s, int start) {
    if (s != null) {
      return subString(s, start, s.length);
    } else {
      return new byte[0];
    }
  }

  /**
   * this converts a byte array in high to low order into a long.
   * if the byte array @param hitolow exceeds @see bytesperlong then
   * the LEADING bytes are skipped.
   */
  public static long unpackLong(byte[] hitolow) {
    long newone = 0L;
    int numbytes = hitolow.length;
    int i = numbytes - bytesperlong;
    if (i < 0) {//good, normal value
      i = 0;
      //and numbytes is fine
    }
    while (i < numbytes) {//#order matters!
      newone <<= 8;
      newone |= hitolow[i++];
    }
    return newone;
  }

  public static long littleEndian(byte[] msfirst, int offset, int length) {
    long ell = 0;
    for (int i = length < 8 ? length : 8; i-- > 0; ) {
      ell <<= 8;
      ell += (msfirst[offset + i] & 255);//
    }
    return ell;
  }

  public static long bigEndian(byte[] msfirst, int offset, int length) {
    long ell = 0;
    if (length > 8) {
      length = 8;
    }
    for (int i = 0; i < length; i++) {
      ell <<= 8;
      ell += (msfirst[offset + i] & 255);//
    }
    return ell;
  }

  public static boolean NonTrivial(byte[] ba) {//oh for templates ....
    return ba != null && ba.length > 0;
  }

  public static byte[] replaceNulWithSpace(byte[] source) {
    return replace(source, Ascii.NUL, Ascii.SP);
  }

  public static byte[] replace(byte[] source, byte toReplace, byte with) {
    if (toReplace != with) {
      for (int i = source.length; i-- > 0; ) {
        if (source[i] == toReplace) {
          source[i] = with;
        }
      }
    }
    return source;
  }

  // NOTE: "toReplace" MUST be the same length as "with"
  // NOTE: "toReplace" and "with" must be smaller in length than "source"
  public static byte[] replace(byte[] source, byte[] toReplace, byte[] with) {
    if ((source != null) &&
      (toReplace != null) &&
      (with != null) &&
      (toReplace != with) && // not the same object
      (!Arrays.equals(toReplace, with)) && // not equal in content
      (toReplace.length == with.length) &&
      (toReplace.length < source.length)) {
      int index = indexOf(source, toReplace);
      while (index > -1) {
        System.arraycopy(with, 0, source, index, with.length);
        index = indexOf(source, toReplace, index + with.length);
      }
    }
    return source;
  }

  public static int indexOf(byte[] source, byte[] searchFor) {
    return indexOf(source, searchFor, 0);
  }

  /**
   * Returns the index within this string of the first occurrence of the
   * specified substring, starting at the specified index.  The integer
   * returned is the smallest value <tt>k</tt> for which:
   * <blockquote><pre>
   *     k &gt;= Math.min(fromIndex, str.length()) && this.startsWith(str, k)
   * </pre></blockquote>
   * If no such value of <i>k</i> exists, then -1 is returned.
   *
   * @param str       the substring for which to search.
   * @param fromIndex the index from which to start the search.
   * @return the index within this string of the first occurrence of the
   * specified substring, starting at the specified index.
   * @throws java.lang.NullPointerException if <code>str</code> is
   *                                        <code>null</code>.
   */
  public static int indexOf(byte[] source, byte[] str, int fromIndex) {
    return indexOf(source, 0, source.length, str, 0, str.length, fromIndex);
  }

  /**
   * Code shared by String and StringBuffer to do searches. The
   * source is the character array being searched, and the target
   * is the string being searched for.
   *
   * @param source       the characters being searched.
   * @param sourceOffset offset of the source string.
   * @param sourceCount  count of the source string.
   * @param target       the characters being searched for.
   * @param targetOffset offset of the target string.
   * @param targetCount  count of the target string.
   * @param fromIndex    the index to begin searching from.
   */
  public static int indexOf(byte[] source, int sourceOffset, int sourceCount, byte[] target, int targetOffset, int targetCount, int fromIndex) {
    if (fromIndex >= sourceCount) {
      return (targetCount == 0 ? sourceCount : -1);
    }
    if (fromIndex < 0) {
      fromIndex = 0;
    }
    if (targetCount == 0) {
      return fromIndex;
    }

    byte first = target[targetOffset];
    int i = sourceOffset + fromIndex;
    int max = sourceOffset + (sourceCount - targetCount);

    startSearchForFirstChar:
    while (true) {
      /* Look for first character. */
      while (i <= max && source[i] != first) {
        i++;
      }
      if (i > max) {
        return -1;
      }

      /* Found first character, now look at the rest of v2 */
      int j = i + 1;
      int end = j + targetCount - 1;
      int k = targetOffset + 1;
      while (j < end) {
        if (source[j++] != target[k++]) {
          i++;
          /* Look for str's first char again. */
          continue startSearchForFirstChar;
        }
      }
      return i - sourceOffset; /* Found whole string. */
    }
  }
}
