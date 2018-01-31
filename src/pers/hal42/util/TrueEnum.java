/*

This class predated java having an Enum class.
We may bring back some of its functionality as wrappers since the Java folk did a minimal and exception happy implementation of the concepts.


The commented out static's indicate that the method would logically be static
but since we always use these from a derived class we need the context of some object
to determine which derived class.
It is not surprising that something that SHOULD be fundamental to the language
is hard to implement using that language.

 */
package pers.hal42.util;
//
//// this class is so basic that it shouldn't use things like textlist.
//// in other words, this class should be an independent unit in ..lang.
//
////we use reflection to associated text with enumerated values.
//import java.lang.reflect.*;
//
//public abstract class TrueEnum extends RawEnum {//extensions define symbols, this enforces range
//  // the only thing you have to override to use this class:
//  protected abstract String [ ] getMyText();
//  public abstract int numValues();
//
////#created undesirable construction loops//  protected static final ErrorLogStream dbg = ErrorLogStream.getForClass(TrueEnum.class);
//  protected final static int invalid =ObjectX.INVALIDINDEX; //alh made this public for things that operate upon TrueEnums, and trueenum croaked!
//  protected final static String invalidStr = "BAD CHOICE";//!!!! this text makes it to the clerk interface for undefined keys...
//
//// moved to RawEnum//  protected int value=invalid;
//
//  public void Clear(){
//    value=invalid;
//  }
//
//  public static final int Invalid(){//public access fubar'd the symbol extraction stuff.
//    return invalid;
//  }
//
//  public /*static */ String [ ] getText() {
//    return getMyText();
//  }
//
//  public /*static */ boolean Legal(int rawValue){
//    return rawValue>invalid && rawValue<numValues();
//  }
//
//  public /*static */ int Coerced(int rawValue){
//    return Legal(rawValue) ? rawValue : Invalid();
//  }
//
//  public /*static */ String TextFor(int rawValue){
//    int newVal = Coerced(rawValue);
//    return (newVal == invalid) ? invalidStr : text(newVal);
//  }
//
///**
// * @mmdeprecated IS BORKED
// * @return whether the rhs's underlying class is the same as 'this's underlying class.
// */
////  public boolean isCompatibleWith(TrueEnum rhs){//+++ always returns FALSE!
////    return this.getClass()==rhs.getClass();
////  }
//
//  public boolean isLegal(){
//    return Legal(value);
//  }
//
//  public static boolean IsLegal(TrueEnum probate){
//    return probate!=null && probate.isLegal();
//  }
//
///**
// * @return whether @param rawValue matches internal value.
// * <br>If both are invalid then even if they are different this returns true.
// */
//  public boolean is(int rawValue){//overloads RawEnum to make illegal==illegal
//    return value == Coerced(rawValue);
//  }
//
//  /**
//   * @mmdeprecated
//   */
////  public boolean is(TrueEnum rhs) {
////    return isCompatibleWith(rhs) ? (value == rhs.value) : false;
////  }
//
//// moved to RawEnum
////  public int Value(){
////    return value;
////  }
//
//  public String Image(){
//    return TextFor(value);
//  }
//
//  public String toString(){
//    return TextFor(value)+"["+value+"]";
//  }
//  /**
//   * @return "3: option3"
//   */
//  public String menuImage(){
//    return String.valueOf(value)+":"+Image();
//  }
//
//  public int setto(int rawValue){
//    return value= Coerced(rawValue);
//  }
//
//  public int setto(String textValue){
//    if(StringX.NonTrivial(textValue)) {
//      textValue = textValue.trim();
//    }
//    return value=indexOf(textValue, getText());//#inline assign
//  }
//
//  /**
//   * @return whether this and the other exist and are of the same actual class and have the same numerical value.
//   */
//  public boolean equals(TrueEnum other){
//    return other!=null && (other.getClass()== this.getClass()) && other.value==this.value ;
//  }
//
//  protected static final int indexOf(String textValue, String [ ] tl){ //+++remove susceptibility to leading and trailing whitespace.
//    for(int i = tl.length; i-->0;) {
//      if(StringX.indexOf(textValue, tl[i], true)) {
//        return i;
//      }
//    }
//    return invalid;
//  }
//
//  public TrueEnum(){
//    value=invalid;
//  }
//
//  public TrueEnum(int rawValue){
//    value=setto(rawValue);
//  }
//
//  public TrueEnum(String textValue){
//    value=setto(textValue);
//  }
//
//  // for the new "combined" enumerations; you can overload these if you want to do something special
//  // but be sure to handle the "invalid" case
//  public String text(int i){
//    return getText()[Coerced(i)];
//  }
//
//  /// used by text array builder.
//  private static final boolean isEnumField(Field fld){
//    int modbits=fld.getModifiers();
//    return  Modifier.isFinal(modbits)
//    && Modifier.isPublic(modbits)
//    && Modifier.isStatic(modbits)
//    && fld.getType() == int.class ;
//  }
//
//  protected static final String [ ] nameVector(Class extension) {
//    try {
//      Field[] f=extension.getFields();
//      String[ ] t = new String[f.length+1];
//      int num;
//      int THElength = 0;
//      String name;
//      for(int i = f.length; i-->0;) {
//        Field fld = f[i];
//        if(isEnumField(fld)){
//          num = fld.getInt(null);
//          THElength = Math.max(num+1, THElength);
//          name = fld.getName();
//          if(THElength >= t.length) {
//            String t2 [ ] = new String[THElength];
//            System.arraycopy(t, 0, t2, 0, t.length);
//            t = t2;
//          }
//          boolean found = false;
//          for(int j = t.length; j-->0;) {
//            if(StringX.indexOf(name, t[j], false)) {
//              found = true;
//              break;
//            }
//          }
//          if(found) {
//            // skip
//          } else {
//            t[num]=name;
//          }
//        }
//      }
//      // +++ what to do about pockets?  For now we fill them in:
//      for(int i = t.length; i-->0;) {
//        if(t[i] == null) {
//          t[i]=invalidStr;
//        }
//      }
//      // now, return the appropriate subset if needed
//      if(THElength < t.length) {
//        String t2 [ ] = new String[THElength];
//        System.arraycopy(t, 0, t2, 0, THElength);
//        t = t2;
//      }
//      return t;
//    } catch (Exception e) {
//      return new String[ 0 ];
//    }
//  }
//
//  public String toSpam() {
//    return toString() + "/{" + dump("", " | ") + "}";
//  }
//
//  public String dump(String prefix, String separator) {
//    String [ ] mytxt = getMyText();
//    StringBuffer retval = new StringBuffer();
//    separator = StringX.TrivialDefault(separator, "");
//    prefix    = StringX.TrivialDefault(prefix, "");
//    Fstring formatter = new Fstring((int)(((1.0*numValues())/10.0)+.5),' ');
//    for(int i = 0; i < numValues(); i++){//in ascending order for clarity
//      formatter.righted(""+i);
//      String test = prefix + formatter + ": " + mytxt[i] + separator;
//      retval.append(test);
//    }
//    return retval.toString();
//  }
//
//  public static TrueEnum Clone(TrueEnum rhs){//rarely used as we must typecast to use it.
//    try {
//      TrueEnum newone= (TrueEnum) rhs.getClass().newInstance();
//      newone.value=rhs.value;
//      return newone;
//    }
//    catch (Exception ex) {
//      return null;
//    }
//  }
//
//  /**
//   * this guy violates the spirit of TrueEnum by allowing decimal strings to be used
//   * to set a value. this is very useful for our debuggers.
//   */
//  public static TrueEnum makeEnum(TrueEnum prototype,String value){
//    if(prototype instanceof TrueEnum){
//      TrueEnum newone= TrueEnum.Clone(prototype);
//      if(StringX.NonTrivial(value)){
//        newone.setto(value);
//        if( ! newone.isLegal()){
//          int pick=StringX.parseInt(value);
//          newone.setto(pick);
//        }
//      }
//      return newone;
//    } else {
//      return null;
//    }
//  }
//
//
//}
//
//
