package pers.hal42.lang;

import com.sun.org.apache.xml.internal.dtm.ref.DTMDefaultBaseIterators;
import pers.hal42.ext.Char;
import sun.awt.image.ByteBandedRaster;

import java.util.jar.Pack200;

import static pers.hal42.text.Ascii.U;

/**
 * Created by andyh on 4/4/17.
 */
public class UTF8 extends Char {

  /** represents one byte of a UTF8 multibyte character, not to be confused with a Unicode character which is a 32 bit entity
   * in addition to utf8 info this wraps ctype functions making them members of a char.
   */
    public UTF8(byte raw ){
//      Char(raw);
    }

    UTF8 setto(byte raw){
      this.raw = raw;
      return this;
    }

    boolean is(UTF8 other) {
      return is(other.raw);
    }

    /** compare byte.
     * @returns whether this is the same value as @param ch. */
    boolean is(int ch) {
      return raw == (byte)ch;
    }


    /** if you skip a byte then numFollowers will be illegal*/
    boolean isMultibyte() {
      return (raw & 0x80) !=0; //treating illegals as multibyte.
    }

    /** since we can't point to an int we create a class */
    public class Packer {
      public int uch;
      public void setto(int uch) {
        this.uch = uch;
      }

      Packer(int uch) {
        setto(uch);
      }
      void firstBits( int uch, int  nf)   {
        if((int) (raw) < 0xC0) { //80..BF are illegal raw, we ignore that here, C0 and C1  are not specfied so lump them in as well
          //we can't trust numFollowers if this byte isn't multiByte
          uch=0;   //illegal argument
        } else {
          if(nf==~0){//unknown, so go compute
            nf=numFollowers();
          }
          //need to keep 6-nf bits
          uch= Bitwise.fieldMask(6-nf,1) & raw;
        }
      }

      void  moreBits( int &uch)   {
        uch<<=6;
        uch |= fieldMask(6)&byte (raw);
      }

      int  pad( int uch,  int  followers)  {
        return uch<<=(6*followers);
      }

  }
//    /** bits extracted from this byte, @param nf is value from numFollers, ~0 means call numFollowers else if already done pass tha back in.*/
//  void firstBits( int &uch,  int  nf=~0)  ;
//  /** merges bits from tihs presumed to be continuation byte into @param uch */
//  void moreBits( int &uch)  ;
//  /** pretend remaining bytes were all zeroes */
//  static void pad( int &uch,  int  followers)  ;
//
//  /** @returns number of 10xxxxxx bytes needed for given @param unichar unicode char.*/
//  static  int  numFollowers(int unichar) ;
//
//  /** @returns 1st byte of sequence given @param followers value returned from @see numFollowers(int)*/
//  static byte  firstByte(int unichar, int  followers) ;
//
//  /** @returns intermediate or final byte, @param followers is 0 for the final one */
//  static byte  nextByte(int unichar, int  followers)  ;
//
//  static char hexNibble( int uch, int  sb)  ;

  /** first byte tells you how many follow, number of leadings ones -2 (FE and FF are both 5)
   *  subsequent bytes start with 0b10xx xxxx 80..BF, which are not legal single byte chars.
   */
   int numFollowers()   {//todo:00 fix signedness problems
    if(raw < 0xC0) { //80..BF are illegal raw, we ignore that here, C0 and C1  are not specfied so lump them in as well
      return 0;   //7 bits    128
    }
    //unrolled loop:
    if(raw < 0xE0) {
      return 1;   //5 bits,  6 bits 2k
    }
    if(raw < 0xF0) {
      return 2;   //4bits, 6, 6    64k
    }
    if(raw < 0xF8) {
      return 3;   //3 bits, 6,6,6  2M
    }
    if(raw < 0xFC) {//not yet used
      return 4;
    }
    return 5; //not yet used
  }


   int   numFollowers(int unichar) {
    if(unichar < (1 << 7)) {//quick exit for ascii
      return 0;
    }
    for(int f=1;f<6;++f){
       int  bits=(6*f) + (6-f);
      if(unichar<(1U << bits)) {
        return f;
      }
    }
    //it appears unicode is likely to stop at 2G characters
    return 0;//not yet implementing invalid extensions.
  } //  numFollowers

  byte  firstByte(int unichar,  int  followers)  {
    if(followers>0) {
      byte  prefix=(byte)0xFC;
      prefix <<= (5 - followers);//1->C0, 2->E0 3->F0 4->F8
      unichar >>= (6 * followers);
      return prefix |= unichar;
    } else {
      return static_cast<byte >(unichar);//only the one byte needed.
    }
  }

  byte   nextByte(int unichar,  int  followers)  {
     int  shift = 6 * followers;
    unichar >>= shift;
    unichar &= fieldMask(6);
    unichar |= (1 << 7);
    return static_cast<byte >(unichar);//# truncate to 8 bits.
  }

  char  hexNibble( int uch,  int  sb)   {
    byte  nib= 15&(uch>>(sb*4)); //push to low nib
    return nib>9? 'A'+nib-10: '0'+nib;
  }



}; // class UTF8

#if 0

  to stream a  int as utf8:

  int followers = numFollowers(unichar);
  out << firstByte(unichar,followers);
  while(followers-->0) {
  out << nextByte(unichar,followers);
  }

  coalescing utf8 stream into a unichar:

  int numfollowers=utf8.numFollowers();
  if(numfollowers>0){
   int uch=0;
  utf8.firstBits(uch);
  while(numfollowers-->0){
  utf8=next();
  utf8.moreBits(uch);
  }
  }

  #endif

  #endif // UTF8_H
}
