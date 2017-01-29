package pers.hal42.util;

// READ ME !!!!
// many lines commented (///) since we aren't compiling with javax.crypto.
// if and when we need this class, uncomment the lines and install javax.crypto!

import java.io.*;
import java.security.*;
//import javax.crypto.*;


/*
All,

   Here is a JAVA app I put together that encrypts/decrypts text-files.
Which is something necessary for MAP. You may want to use it as a template
for protecting sensitive configuration files. Or for protecting your own
personal files, or whatever.....

  First time you run it, when your "secret key" is being generated, be
patient! on my P2-450, it takes about 15  seconds or so to generate a 3DES
key. However, once the key is generated, every other time you run this app,
it's rather speedy.


Enjoy!

Allan
*/


public class Crypt {

  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(Crypt.class);

  static {
///    Security.addProvider(new com.sun.crypto.provider.SunJCE());
  }

///  Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding"); // should be static ?
  Key key;
  private void generateKey() {
    ObjectInputStream in = null;
    ObjectOutputStream out = null;
    try {
      in = new ObjectInputStream(new FileInputStream("SecretKey"));
      key = (Key)in.readObject();
///      KeyGenerator generator = KeyGenerator.getInstance("DESede");
///      generator.init(new SecureRandom());
///      key = generator.generateKey();
      out = new ObjectOutputStream(new FileOutputStream("SecretKey"));
      out.writeObject(key);
    } catch (Exception e) {
      // bitch +++
    } finally {
      try {
        if(in!=null) {
          in.close();
        }
        if(out!=null) {
          out.close();
        }
      } catch (Exception e) {
        // bitch +++
      }
    }
  }

  private void encrypt(String plaintext, String cyphertext) {
    try {
///      cipher.init(Cipher.ENCRYPT_MODE, key);
      FileInputStream in = new FileInputStream(plaintext);
      FileOutputStream out = new FileOutputStream(cyphertext);

      int readem = in.available();
      byte[] buff = new byte[readem];
      in.read(buff, 0, readem);

///      byte[] raw = cipher.doFinal(buff);
      //BASE64Encoder encoder = new BASE64Encoder(); // +++ use our Base64Codec instead!  No reason to load that whole package for one class!
///      String base64 = encoder.encode(raw);

///      out.write(base64.getBytes());
      out.close();
      in.close();
    } catch (Exception e) {
      dbg.Caught(e);
    }
  }

  private void decrypt(String plaintext, String cyphertext) {
    try {
///      cipher.init(Cipher.DECRYPT_MODE, key);
      FileInputStream in = new FileInputStream(cyphertext);
      FileOutputStream out = new FileOutputStream(plaintext);
      // BASE64Decoder decoder = new BASE64Decoder(); // +++ use our Base64Codec instead!  No reason to load that whole package for one class!

      int avail = in.available();

      byte[] buff = new byte[avail];
      in.read(buff, 0, avail);

      byte[] raw = Base64Codec.decode(new String(buff).toCharArray());
///      byte[] results = cipher.doFinal(raw);

///      out.write(results);
      out.close();
      in.close();
    } catch (Exception e) {
      dbg.Caught(e);
    }
  }

  public static final void main (String[] args) throws Exception {
    if (args.length < 3) {
      System.out.println("Usage: Crypt -e|-d plaintext ciphertext");
      return;
    }
    Crypt c = new Crypt();
    String plaintext = args[1];
    String cyphertext = args[2];
    if (args[0].indexOf("e") != -1) {
      c.encrypt(plaintext, cyphertext);
    } else if (args[0].indexOf("d") != -1) {
      c.decrypt(plaintext, cyphertext);
    }
  }
}
