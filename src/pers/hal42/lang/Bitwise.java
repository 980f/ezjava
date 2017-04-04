package pers.hal42.lang;

/**  bit field merging and related items.
 * Originally this was C++ code that was used in microcontrollers to actuate hardware pins. Much was lost in migrating to Java.
 * Created by andyh on 4/4/17.
 */
public class Bitwise {

  /** @return byte address argument as a pointer to that byte */
//URGENTLY //irritating to step through during debug.
  public static boolean bit(int patter, int bitnumber){
    return (patter & (1 << bitnumber)) != 0;
  }


  public static boolean isOdd(int pattern){
    return 0!=(pattern&1);
  }

  public static boolean isEven(int pattern){
    return ! isOdd(pattern);
  }

  public int patter;

  public Bitwise setBit( int bitnumber){
    patter |= (1 << bitnumber);
    return this;
  }

  public Bitwise clearBit(int bitnumber){
    patter &= ~(1 << bitnumber);
    return this;
  }



  public Bitwise assignBit(int bitnumber,boolean one){
    if(one){
      setBit(bitnumber);
    } else {
      clearBit(bitnumber);
    }
    return this;
  }

//  public static class BitReference {
//    int &word;
//    int mask;
//    /** initialize from a memory address and bit therein. If address isn't aligned then bitnumber must be constrained to stay within the same word*/
//    BitReference(int memoryAddress,int bitnumber):
//    word(*atAddress(memoryAddress&~3)),
//    mask(1<<(31& ((memoryAddress<<3)|bitnumber))){
//      //now it is an aligned 32 bit entity
//    }
//    boolean operator =(boolean set)const{
//      if(set){
//        word|=mask;
//      } else {
//        word &=~mask;
//      }
//      return set;
//    }
//
//    operator boolean()const{
//      return (word&mask)!=0;
//    }
//  };


  /** @return splice of two values according to @param mask */
  public static int insertField(int target, int source, int mask){
    return (target & ~mask) | (source & mask);
  }

  /** splices a value into another according to @param mask */
  public int merge(int source, int mask){
    return patter= insertField(patter,source, mask);
  }


  /** @return bits @param msb through @param lsb set to 1.
   * Default arg allows one to pass a width for lsb aligned mask of that many bits */
  public static int fieldMask(int msb,int lsb){
    return (1 << (msb+1)) - (1<<lsb);
  }

  /** @return bits @param lsb for width @param width set to 1.*/
  public static int bitMask(int lsb,int width){
    return (1 << (lsb+width)) - (1<<lsb);
  }


  /** use the following when offset or width are NOT constants, else you should be able to define bit fields in a struct and let the compiler to any inserting*/
  public static int insertField(int target, int source, int msb, int lsb){
    return insertField(target, source<<lsb ,fieldMask(msb,lsb));
  }

  public int mergeField(int source, int msb, int lsb){
    return patter=insertField(patter,source,msb,lsb);
  }

  public static int extractField(int source, int msb, int lsb){
    return (source&fieldMask(msb,lsb)) >> lsb ;
  }


  /** use the following when offset or width are NOT constants, else you should be able to define bit fields in a struct and let the compiler to any inserting*/
  public static int insertBits(int target, int source, int lsb, int width){
    return insertField(target, source<<lsb ,bitMask(lsb,width));
  }

//  public int mergeBits(int source, int lsb, int width){
//    return merge(source<<lsb,bitMask(lsb,width));
//  }



  public static  int extractBits(int source, int lsb, int width){
    return (source & bitMask(lsb,width)) >> lsb ;
  }



//  /** for when the bits to pick are referenced multiple times and are compile time constant
//   * trying to bind the item address as a template arg runs afoul of the address not being knowable at compile time.
//   * while it is tempting to have a default of 1 for msb/width field, that is prone to users walking away from a partially edited field.
//   */
//  template <int lsb, int msb, boolean msbIsWidth=true> class BitFielder {
//    enum {
//      mask = msbIsWidth?bitMask(lsb,msb):fieldMask(msb ,lsb) // aligned mask
//    };
//    public:
//    static int extract(int &item){
//      return (item & mask) >> lsb;
//    }
//
//    static int mergeInto(int &item,int value){
//      int merged= (item & ~mask) | ((value << lsb) & mask);
//      item=merged;
//      return merged;
//    }
//  };
//
//  template <int lsb> class BitPicker {
//    enum {
//      mask = bitMask(lsb) // aligned mask
//    };
//    public:
//    int extract(int &item) const {
//      return (item & mask) >> lsb;
//    }
//
//    int operator ()(int &&item)const{
//      return extract(item);
//    }
//
//    boolean operator()(int &word,boolean set)const{
//      if(set){
//        word|=mask;
//      } else {
//        word &=~mask;
//      }
//      return set;
//    }
//  };
//
//  /** Create this object around a field of an actual data item.
//   * trying to bind the address as a template arg runs afoul of the address often not being knowable at compile time*/
//  template <int lsb, int msb, boolean msbIsWidth=false> class BitField: public BitFielder<lsb,msb,msbIsWidth> {
//    int &item;
//    public:
//    BitField(int &item): item(item){
//    }
//    operator int() const {
//      return BitFielder<lsb,msb>::extract(item);
//    }
//    void operator =(int value) const {
//      BitFielder<lsb,msb>::mergeInto(item ,value );
//    }
//  };
//
//
//  /** for hard coded absolute (known at compile time) address and bit number */
//  template <int memoryAddress,int bitnumber> struct KnownBit {
//    enum {
//      word= memoryAddress&~3,
//        mask=(1<<(31& ((memoryAddress<<3)|bitnumber)))
//    };
//
//    boolean operator =(boolean set)const{
//      if(set){
//      *atAddress(word)|=mask;
//      } else {
//      *atAddress(word) &=~mask;
//      }
//      return set;
//    }
//
//    operator boolean()const{
//      return (*atAddress(word)&mask)!=0;
//    }
//  };
//
//
/////////////////////////////////////////////
///// a group of discontiguous bits, used for bitmasking
//
//
///** declarative part of 3 step template magic */
//  template <int ... list> struct BitWad;
//
//  /** termination case of 3 step template magic */
//  template <int pos> struct BitWad<pos> {
//    enum { mask = 1 << pos };
//    public:
//    inline static int extract(int varble){
//      return (mask & varble);
//    }
//
//  static boolean exactly(int varble, int match){
//    return extract(varble) == extract(match); // added mask to second term to allow for lazy programming
//  }
//
//  static boolean all(int varble){
//    return extract(varble) == mask;
//  }
//
//  static boolean any(int varble){
//    return extract(varble) != 0;
//  }
//
//  static boolean none(int varble){
//    return extract(varble) == 0;
//  }
//};
//
///** assemble a bit field, without using stl. */
//template <int pos, int ... poss> struct BitWad<pos, poss ...> {
//enum { mask= BitWad<pos>::mask | BitWad<poss ...>::mask };
//
//public:
//  inline static int extract(int varble){
//  return (mask & varble);
//  }
//
//static boolean exactly(int varble, int match){
//  return extract(varble) == extract(match); // added mask to second term to allow for lazy programming
//  }
//
//static boolean all(int varble){
//  return extract(varble) == mask;
//  }
//
//static boolean any(int varble){
//  return extract(varble) != 0;
//  }
//
//static boolean none(int varble){
//  return extract(varble) == 0;
//  }
//  };

}
