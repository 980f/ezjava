package pers.hal42.text;

import pers.hal42.lang.StringX;
import pers.hal42.lang.CharX;
import pers.hal42.text.Formatter;

public class Ascii {

  public final static byte NUL=0x00;
  public final static byte SOH=0x01;
  public final static byte STX=0x02;//
  public final static byte ETX=0x03;//
  public final static byte EOT=0x04;//End Of Transmission aka ^D.
  public final static byte ENQ=0x05;//
  public final static byte ACK=0x06;//positive acknowledgement
  public final static byte BEL=0x07;//
  public final static byte BS =0x08;//
  public final static byte TAB=0x09;//
  public final static byte LF =0x0A;//
  public final static byte VT =0x0B;//vertical tab
  public final static byte FF =0x0C;//
  public final static byte CR =0x0D;//
  public final static byte SO =0x0E;//SHift OUt
  public final static byte SI =0x0F;//Shift In
  public final static byte DLE=0x10;//^P
  public final static byte DC1=0x11;//also DC1
  public final static byte DC2=0x12;//
  public final static byte DC3=0x13;//also DC3
  public final static byte DC4=0x14;//
  public final static byte NAK=0x15;//negative acknowledgement
  public final static byte SYN=0x16;//synchronous something or other
  public final static byte ETB=0x17;//???
  public final static byte CAN=0x18;//cancel
  public final static byte EM =0x19;//
  public final static byte SUB=0x1A;// "substitute"
  public final static byte ESC=0x1B;//  escape
  public final static byte FS =0x1C;// field separator
  public final static byte GS =0x1D;// group separator
  public final static byte RS =0x1E;// record separator
  public final static byte US =0x1F;// unit separator
  public final static byte SP =0x20;// space

  // rest are printables ...
  public final static byte EXCLAMATION =0x21;// !
  public final static byte DOUBLEQUOTE =0x22;// "
  public final static byte POUND       =0x23;// #
  public final static byte DOLLAR      =0x24;// $
  public final static byte PERCENT     =0x25;// %
  public final static byte AMPERSAND   =0x26;// &
  public final static byte SINGLEQUOTE =0x27;// '
  public final static byte OPENPARAN   =0x28;// (
  public final static byte CLOSEPARAN  =0x29;// )
  public final static byte ASTERISK    =0x2A;// *
  public final static byte PLUS        =0x2B;// +
  public final static byte COMMA       =0x2C;// ,
  public final static byte DASH        =0x2D;// -
  public final static byte PERIOD      =0x2E;// .
  public final static byte SLASH       =0x2F;// /

  public final static byte ZERO  =0x30;// 0
  public final static byte ONE   =0x31;// 1
  public final static byte TWO   =0x32;// 2
  public final static byte THREE =0x33;// 3
  public final static byte FOUR  =0x34;// 4
  public final static byte FIVE  =0x35;// 5
  public final static byte SIX   =0x36;// 6
  public final static byte SEVEN =0x37;// 7
  public final static byte EIGHT =0x38;// 8
  public final static byte NINE  =0x39;// 9

  public final static byte COLON        =0x3A;// :
  public final static byte SEMICOLON    =0x3B;// ;
  public final static byte LESSTHAN     =0x3C;// <
  public final static byte EQUALS       =0x3D;// =
  public final static byte GREATERTHAN  =0x3E;// >
  public final static byte QUESTIONMARK =0x3F;// ?
  public final static byte AT           =0x40;// @

  public final static byte A =0x41;// A
  public final static byte B =0x42;// B
  public final static byte C =0x43;// C
  public final static byte D =0x44;// D
  public final static byte E =0x45;// E
  public final static byte F =0x46;// F
  public final static byte G =0x47;// G
  public final static byte H =0x48;// H
  public final static byte I =0x49;// I
  public final static byte J =0x4A;// J
  public final static byte K =0x4B;// K
  public final static byte L =0x4C;// L
  public final static byte M =0x4D;// M
  public final static byte N =0x4E;// N
  public final static byte O =0x4F;// O
  public final static byte P =0x50;// P
  public final static byte Q =0x51;// Q
  public final static byte R =0x52;// R
  public final static byte S =0x53;// S
  public final static byte T =0x54;// T
  public final static byte U =0x55;// U
  public final static byte V =0x56;// V
  public final static byte W =0x57;// W
  public final static byte X =0x58;// X
  public final static byte Y =0x59;// Y
  public final static byte Z =0x5A;// Z

  public final static byte OPENSQUAREBRACE =0x5B;// [
  public final static byte BACKSLASH       =0x5C;// \
  public final static byte CLOSESQUAREBRACE=0x5D;// ]
  public final static byte CARET           =0x5E;// ^
  public final static byte UNDERSCORE      =0x5F;// _
  public final static byte BACKQUOTE       =0x60;// `

  public final static byte a =0x61;// a
  public final static byte b =0x62;// b
  public final static byte c =0x63;// c
  public final static byte d =0x64;// d
  public final static byte e =0x65;// e
  public final static byte f =0x66;// f
  public final static byte g =0x67;// g
  public final static byte h =0x68;// h
  public final static byte i =0x69;// i
  public final static byte j =0x6A;// j
  public final static byte k =0x6B;// k
  public final static byte l =0x6C;// l
  public final static byte m =0x6D;// m
  public final static byte n =0x6E;// n
  public final static byte o =0x6F;// o
  public final static byte p =0x70;// p
  public final static byte q =0x71;// q
  public final static byte r =0x72;// r
  public final static byte s =0x73;// s
  public final static byte t =0x74;// t
  public final static byte u =0x75;// u
  public final static byte v =0x76;// v
  public final static byte w =0x77;// w
  public final static byte x =0x78;// x
  public final static byte y =0x79;// y
  public final static byte z =0x7A;// z

  public final static byte OPENCURLY  =0x7B;// {
  public final static byte VERTICAL   =0x7C;// |
  public final static byte CLOSECURLY =0x7D;// }
  public final static byte TILDE      =0x7E;// ~

//move to safe {
  public static String bracket(String text){//--->>safe
    return "["+text+"]";
  }

  public static String bracket(long ell){//--->>safe
    return bracket(String.valueOf(ell));
  }

  public static String bracket(int eye){//--->>safe
    return bracket(String.valueOf(eye));
  }

  public static String bracket(boolean bee){//--->>safe
    return bracket(String.valueOf(bee));
  }

  public static String bracket(Object obj){//--->>safe
    if(obj ==null){
      return bracket("null");
    }
    if (obj instanceof byte[]) {
      return bracket(image((byte[])obj));
    }
    return bracket(String.valueOf(obj));
  }

  public static String bracket(byte [] ba){//--->>safe
    return bracket(image(ba));
  }

  //move to safe }

  public static String imageBracketSpace(byte b){
    return image(b, true);
  }
  public static String image(byte b){
    return image(b, false);
  }
  private static String image(byte b, boolean bracketSpace){
    switch (b) {
      case NUL: return bracket("nul");
      case SOH: return bracket("soh");
      case STX: return bracket("STX");
      case ETX: return bracket("ETX");
      case EOT: return bracket("EOT");
      case ENQ: return bracket("ENQ");
      case ACK: return bracket("ACK");
      case BEL: return bracket("bel");
      case BS:  return bracket("bs");
      case TAB: return bracket("tab");
      case LF:  return bracket("LF");
      case VT:  return bracket("vt");
      case FF:  return bracket("ff");
      case CR:  return bracket("CR");
      case SO:  return bracket("so");
      case SI:  return bracket("si");
      case DLE: return bracket("dle");
      case DC1: return bracket("dc1");
      case DC2: return bracket("dc2");
      case DC3: return bracket("dc3");
      case DC4: return bracket("dc4");
      case NAK: return bracket("NAK");
      case SYN: return bracket("syn");
      case ETB: return bracket("etb");
      case CAN: return bracket("can");
      case EM:  return bracket("em");
      case SUB: return bracket("sub");
      case ESC: return bracket("esc");
      case FS:  return bracket("FS");
      case GS:  return bracket("GS");
      case RS:  return bracket("RS");
      case US:  return bracket("US");
      case SP:  return bracketSpace ? bracket("sp") : " ";
    }
    if(b<32 || b>126){//non-printables as hex
      return bracket(Formatter.ox2(b));
    }
    //else the ascii char
    return String.valueOf((char)b);
  }

  public static String image(int b){
    return image((byte)b);
  }

  public static StringBuffer image(byte []ba){
    return image(ba,0,ba.length);
  }
  public static StringBuffer imageBracketSpace(byte []ba){
    return image(ba,0,ba.length,true);
  }
  /**
   * generate filtered image of bytes from @param start up to but not including @param end.
   */
  public static StringBuffer image(byte []ba, int start, int end){
    return image(ba,0,ba.length,false);
  }
  private static StringBuffer image(byte []ba, int start, int end, boolean bracketSpace){
    StringBuffer newone=new StringBuffer();
    if(ba!=null){
      for(int i=start;i<end;i++){
        newone.append(image(ba[i],bracketSpace));
      }
    } else {
      // +++ do something if we get a null?
    }
    return newone;
  }
  /**
   * @return a string interpreting ascii control chars.
   * convert tabs etc to spaces.
   */
  public static String cooked(byte []raw){
    if(ByteArray.NonTrivial(raw)){
      StringBuffer clean=new StringBuffer(raw.length);
      for(int i=0;i<raw.length;i++){
        byte c= raw[i];
        switch(c){
          default:  clean.append(image(c)); break;
            case NUL: continue; //simply omit nulls
            case STX: clean.setLength(0);   break; //erase preceding stuff
            case ETX:
            case EOT: return clean.toString();//"clear to end of line"

            case ESC:
              return ""; //common "oops" characters

            case CR: case LF: case BEL: case TAB: case FF:
              clean.append(' ');
              break; //whitespace

            case BS:
              if(clean.length()>0){
                clean.setLength(clean.length()-1);
              }
              break;
        }
      }
      return clean.toString();//someone elses job to do any trimming.
    } else {
      return "";//trivial but not null.
    }
  }

  public static char Char(byte b){
    return (char)b;
  }

  public static char Char(int b){
    if(b<0 || b>255){
      return CharX.INVALIDCHAR;
    } else {
      return (char)b;
    }
  }

  public static String cooked(String raw){
    return StringX.NonTrivial(raw) ? cooked(raw.getBytes()) : "";
  }

  public static final String CRLF="\r\n";

  public static byte [] byteAsArray(int bite){
    byte [] newone=new byte[1];
    newone[0]=(byte)bite;
    return newone;
  }

  public static byte [] emptyArray(){
    return new byte[0];
  }

}

