// +++ rewrite this as a filterstream !!!

// if you are doing pieces of a larger block of data (file for instance),
// except for the last block you send,
// only pass in blocks (arrays) of bytes whose length is evenly divisible by 3
// and only pass in blocks of chars whose length is evenly divisible by 4

// it does work, though.

package pers.hal42.math;

public class Base64Codec {

  // code characters for values 0..63
  private static final char alphabet[] =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=".toCharArray();
  private static final char XX = 255;
  // Table for decoding base64
  private static final char codes[] = {
    XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX,
    XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX,
    XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, 62, XX, XX, XX, 63,
    52, 53, 54, 55, 56, 57, 58, 59, 60, 61, XX, XX, XX, XX, XX, XX,
    XX, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
    15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, XX, XX, XX, XX, XX,
    XX, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
    41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, XX, XX, XX, XX, XX,
    XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX,
    XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX,
    XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX,
    XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX,
    XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX,
    XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX,
    XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX,
    XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX,
  };

  public static String toString(byte dataX3[]) {
    char[] ncoded = encode(dataX3);
    return new String(ncoded);
  }

  // returns an array of base64-encoded characters to represent the passed data array.
  public static char[] encode(byte dataX3[]) {
    int length = dataX3.length;
    int len = ((length + 2) / 3) * 4;
    char out[] = new char[len];

    // 3 bytes encode to 4 chars.  Output is always an even multiple of 4 characters.
    for (int i = 0, index = 0; i < length; i += 3, index += 4) {
      boolean quad = false;
      boolean trip = false;

      int val = (0xFF & (int) dataX3[i]);
      val <<= 8;
      if ((i + 1) < length) {
        val |= (0xFF & (int) dataX3[i + 1]);
        trip = true;
      }
      val <<= 8;
      if ((i + 2) < length) {
        val |= (0xFF & (int) dataX3[i + 2]);
        quad = true;
      }
      out[index + 3] = alphabet[(quad ? (val & 0x3F) : 64)];
      val >>= 6;
      out[index + 2] = alphabet[(trip ? (val & 0x3F) : 64)];
      val >>= 6;
      out[index + 1] = alphabet[val & 0x3F];
      val >>= 6;
      out[index + 0] = alphabet[val & 0x3F];
    }
    return out;
  }

  public static byte[] fromString(String s) {
    char[] caster = new char[s.length()];
    for (int i = caster.length; i-- > 0; ) {
      caster[i] = s.charAt(i);
    }
    return decode(caster);
  }


  // Returns an array of bytes which were encoded in the passed character array.
  public static byte[] decode(char dataX4[]) {
    int length = dataX4.length;
    int len = ((length + 3) / 4) * 3;
    if (length > 0 && dataX4[length - 1] == '=') {
      --len;
    }
    if (length > 1 && dataX4[length - 2] == '=') {
      --len;
    }
    byte out[] = new byte[len];

    int shift = 0;   // # of excess bits stored in accum
    int accum = 0;   // excess bits
    int index = 0;

    for (char aDataX4 : dataX4) {
      int value = codes[aDataX4 & 0xFF];   // ignore high byte of char
      if (value >= 0) {                     // skip over non-code
        accum <<= 6;            // bits shift up by 6 each time thru
        shift += 6;             // loop, with new bits being put in
        accum |= value;         // at the bottom.
        if (shift >= 8) {     // whenever there are 8 or more shifted in,
          shift -= 8;         // write them out (from the top, leaving any
          byte c = (byte) ((accum >> shift) & 0xff);
          if (index < len) {
            out[index++] = c;     // excess at the bottom for next iteration.
          }
        }
      }
    }
    return out;
  }

  // testing stuff:
  public static String Usage() {
    return "parameters: {+|-} string";
  }

  public static void Test(String[] argv) {
    try {
      int argc = argv.length;
      boolean deen = true;
      String str = "!_!";
      switch (argc) {
        case 2:
          str = argv[--argc];
        case 1:
          deen = !argv[--argc].equals("-");
          break;
        default:
          System.out.println("Excess command line args\n");
        case 0:
          System.out.println(Usage());
          return;
      }
      System.out.println((deen ? "En" : "De") + "coding '" + str + "' \nresulted in '" +
        (deen ? new String(encode(str.getBytes())) : new String(decode(str.toCharArray()))) + "'.");
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

}


/*
// for raw unsigned char data, since nstring does not like anything that doesn't end in a '\0' (and resizes with that in mind)
class uCharBuffer : public dVector<unsigned char>
{
public:
  void fromString(const char *tmp)
  {
    if(!tmp) {
      resize(0);
      return;
    }
    resize(safeStrLen(tmp));
    int i;
    forVectorElements((*this), i) {
      (*this)[i] = tmp[i];
    }
  }

  nString AsnString()
  {
    nString tmp(size() + 1);
    int i;
    forVectorElements((*this), i) {
      tmp[i] = (*this)[i];
    }
    return tmp;
  }
};

*/
// NOTE!!!!!  '\0' are not appended to strings,
//            as the buffers could contain ANYTHING, not just printable characters
//            therefore, you should make your buffer one char longer than needed and
//            place a '\0' in the character position at usedLength.

/*
base64 encoding, in short

QP (base64-encoded) contains uppercase, lowercase, numbers, '+', '/' and '='.

Take the encoded stuff in groups of 4 characters and turn each character into a code 0 to 63 thus:

    A means of labelling the content of mail messages.
    A-Z map to 0 to 25
    a-z map to 26 to 51
    0-9 map to 52 to 61
    + maps to 62
    / maps to 63

Express the four numbers thus found (all 0 to 63) in binary:

00aaaaaa 00bbbbbb 00cccccc 00dddddd

This then maps to _three_ real bytes formed thus:

aaaaaabb bbbbcccc ccdddddd

Equals signs (one or two) are used at the end of the encoded block to indicate that the text was not an integer multiple of three bytes long.

postmaster@epfl.ch
See:  http://www.cis.ohio-state.edu/htbin/rfc/rfc2045.html
      http://info.internet.isi.edu/in-notes/rfc/files/rfc1421.txt
*/

