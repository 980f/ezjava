package pers.hal42.util;

// +++ turn this into a real cipher (javax.crypto.Cipher)!


import  java.io.*;

public final class CipherP86B2 {

  // use these!

  public static final OutputStream getOutputStream(OutputStream toFilter, String key, int primerNot95X) {
    return new CipherP86B2(key, primerNot95X).getOutputStream(toFilter);
  }

  public static final InputStream getInputStream(InputStream toFilter, String key, int primerNot95X) {
    return new CipherP86B2(key, primerNot95X).getInputStream(toFilter);
  }


  // internals

  private final OutputStream getOutputStream(OutputStream toFilter) {
    return new FilterOutputStream(toFilter) {
      public void write(int b) throws IOException {
        super.write(crypt(b));
      }
    };
  }

  private final InputStream getInputStream(InputStream toFilter) {
    return new FilterInputStream(toFilter) {
      public int read() throws IOException {
        return crypt(super.read());
      }
    };
  }

  private CipherP86B2(String keyChars, int primerNot95X) {
    // valid values are 0 - MAXINT
    // if < 0, default is used
    if(primerNot95X > -1) {
      key = primerNot95X;
      salt = primerNot95X;
    }
    byte password[] = keyChars.getBytes();
    keyChars = null; // to let it cleanup (overkill, I know)
    crypt(password); // encrypt the password; this gets the whole stream into a specific "state"
    Safe.fillBytes(password, (byte)255); // so it won't be visible before it gets cleaned up (overkill, I know)
  }

  private CipherP86B2() {
    // nobody can do this
  }

  // running state of the stream
  private int key;
  private int salt;

  private static final int mod95(int val) {
    // The mathematical MOD does not match the computer MOD. (this was from C; don't know what is the case for Java)
    // This may look strange, but it gets the job done, and portably at that.
    while (val >= 9500) val -= 9500;
    while (val >= 950) val -= 950;
    while (val >= 95) val -= 95;
    while (val < 0) val += 95;
    return (val);
  }

  /**********
   *
   * MMM: This is old documentation, but still interesting.
   * It is no longer restricted to a block of text (as the stream functions indicate).
   *
   * There is no reason to provide a system that will be *highly* secure,
   * as the algorithm is right here in decompilable code!
   *
   * For greater security, use the java-provided key-exchange encryption techniques.
   *
   *
   *  crypt - in place encryption/decryption of a buffer
   *
   *      1.  The encryption had to be inexpensive, both in terms of speed and space.
   *
   *      2.  The system needed to be secure against all but the most determined of attackers.
   *
   *  For encryption of a block of data, one calls crypt passing
   *  a pointer to the data block and its length. The data block is
   *  encrypted in place, that is, the encrypted output overwrites
   *  the input.  Decryption is totally isomorphic, and is performed
   *  in the same manner by the same routine.
   *
   *  Before using this routine for encrypting data, you are expected
   *  to specify an encryption key.  This key is an arbitrary string,
   *  to be supplied by the user.  To set the key takes two calls to
   *  crypt().  First, you call
   *
   *      crypt(NULL, vector)
   *
   *  This resets all internal control information.  Typically (and
   *  specifically in the case on MICRO-emacs) you would use a "vector"
   *  of 0.  Other values can be used to customize your editor to be
   *  "incompatible" with the normally distributed version.  For
   *  this purpose, the best results will be obtained by avoiding
   *  multiples of 95.
   *
   *  Then, you "encrypt" your password by calling
   *
   *      crypt(pass, strlen(pass))
   *
   *  where "pass" is your password string.  Crypt() will destroy
   *  the original copy of the password (it becomes encrypted),
   *  which is good.  You do not want someone on a multiuser system
   *  to peruse your memory space and bump into your password.
   *  Still, it is a better idea to erase the password buffer to
   *  defeat memory perusal by a more technical snooper.
   *
   *  For the interest of cryptologists, at the heart of this
   *  function is a Beaufort Cipher.  The cipher alphabet is the
   *  range of printable characters (' ' to '~'), all "control"
   *  and "high-bit" characters are left unaltered.
   *
   *  The key is a variant autokey, derived from a weighted sum
   *  of all the previous clear text and cipher text.  A counter
   *  is used as salt to obliterate any simple cyclic behavior
   *  from the clear text, and key feedback is used to assure
   *  that the entire message is based on the original key,
   *  preventing attacks on the last part of the message as if
   *  it were a pure autokey system.
   *
   *  Overall security of encrypted data depends upon three
   *  factors:  the fundamental cryptographic system must be
   *  difficult to compromise; exhaustive searching of the key
   *  space must be computationally expensive; keys and plaintext
   *  must remain out of sight.  This system satisfies this set
   *  of conditions to within the degree desired for MicroEMACS.
   *
   *  Though direct methods of attack (against systems such as
   *  this) do exist, they are not well known and will consume
   *  considerable amounts of computing time.  An exhaustive
   *  search requires over a billion investigations, on average.
   *
   *  The choice, entry, storage, manipulation, alteration,
   *  protection and security of the keys themselves are the
   *  responsibility of the user.
   *
   **********/

  private final void crypt(byte bytes []) {
    for(int i = 0; i < bytes.length; i++) { /* for every character in the buffer */
      bytes[i] = (byte)crypt(bytes[i]);
    }
  }

  /**
   * should only pass this "byte"s!
   */
  private final int crypt(int original) {
    int cc = original;
    /* only encipher printable characters */
    if((cc > 31) && (cc < 127)) {

// this next chunk might behave differently under java than it did under C:
      /**  If the upper bit (bit 29) is set, feed it back into the key.  This
      assures us that the starting key affects the entire message.  **/
      key &= 0x1FFFFFFFL;   /* strip off overflow */
      if ((key & 0x10000000L) > 0) { // do these longs need to be here?
        key ^= 0x0040A001L;     /* feedback */
      }

// this next chunk might behave differently under java than it did under C:
      /**  Down-bias the character, perform a Beaufort encipherment, and
      up-bias the character again.  We want key to be positive
      so that the left shift here will be more portable and the
      mod95() faster   **/
      cc = mod95((int) (key % 95) - (cc - ' ')) + ' ';

      /**  the salt will spice up the key a little bit, helping to obscure
      any patterns in the clear text, particularly when all the
      characters (or long sequences of them) are the same.  We do
      not want the salt to go negative, or it will affect the key
      too radically.  It is always a good idea to chop off cyclics
      to prime values.  **/
      if (++salt > 20856) {
        salt = 0;             /* prime modulus (max = 20856) */
      }

      /**  our autokey (a special case of the running key) is being
      generated by a weighted checksum of clear text, cipher
      text, and salt.   **/
      key = key + key + cc + original + salt;
    } else if ((cc >= 0) && (cc < 32)) { /* handle the lower ones */
      cc = 31 - cc;
    } else { /* handle the upper ones */
      cc = 128 - (cc - 127) + 127;
    }
    return cc & 255;
  }

  // --- for testing
  public static final void main(String [] args) {
    String key = "THIS is a GREAT big test !!+ 0123456789AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz,./;'[]`-=\\<>?:\"{}~!()_+|\n\r\t";
    int primer = (int)Math.sqrt(System.currentTimeMillis());
    test(primer, key, (args.length == 0) ? key : args[0], true);
  }

  private static final void test(int primer, String key, String toTest, boolean URLed) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ByteArrayInputStream bais = new ByteArrayInputStream(toTest.getBytes());
    try {
      Streamer.swapStreams(bais, CipherP86B2.getOutputStream(baos, key, primer));
      System.out.println("key: " + key);
      System.out.println("primer: " + primer);
      String encrypted = baos.toString();
      System.out.println("encrypted: " + encrypted);

      if(URLed){
        baos.reset();
        bais = new ByteArrayInputStream(encrypted.getBytes());
        OutputStream os = new net.paymate.net.URLEncoderFilterOutputStream(baos);
        Streamer.swapStreams(bais, os);
        String urled = baos.toString();
        System.out.println("URLencoded: " + urled);

        baos.reset();
        bais = new ByteArrayInputStream(urled.getBytes());
        InputStream is = new net.paymate.net.URLDecoderFilterInputStream(bais);
        Streamer.swapStreams(is, baos);
        String unurled = baos.toString();
        System.out.println("URLdecoded: " + unurled);

        boolean match2 = unurled.equals(encrypted);
        System.out.println("Strings " + (match2 ? "DO" : "DO NOT") + " match!");

        encrypted = unurled;
      }

      baos.reset();
      bais = new ByteArrayInputStream(encrypted.getBytes());

      // handle the urling out and in here, and print notes on them:
      Streamer.swapStreams(bais, CipherP86B2.getOutputStream(baos, key, primer));
      String decrypted = baos.toString();
      System.out.println("decrypted: " + decrypted);
      boolean match = decrypted.equals(toTest);
      System.out.println("Strings " + (match ? "DO" : "DO NOT") + " match!");
      if(!match) {
        System.out.println("decrypted = ["+decrypted+"]");
        System.out.println("original  = ["+toTest+"]");
      }
    } catch (Exception e) {
      System.out.println("Exception: " + e);
      e.printStackTrace(System.out);
    }

  }

}
