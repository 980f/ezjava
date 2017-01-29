package pers.hal42.lang;

import pers.hal42.lang.Value;
import pers.hal42.lang.ContentType;

public class Bool extends Value implements Comparable {

  private boolean amtrue;

  public boolean testandclear(){
    synchronized (this) {
      try {
        return amtrue;
      }
      finally {
        amtrue=false;
      }
    }
  }

  public boolean testandset(){
    synchronized (this) {
      try {
        return amtrue;
      }
      finally {
        amtrue=true;
      }
    }
  }


  //synch set and clear with testandclear
  public synchronized void set(){
    amtrue=true;
  }

  public synchronized void Clear(){
    amtrue=false;
  }

  public Bool(boolean amtrue) {
    this.amtrue = amtrue;
  }

  public Bool() {
    this(false);
  }

  public boolean Value() {
    return amtrue;
  }

  public ContentType charType(){//for Value hierarchy
    return new ContentType(ContentType.alphanum);
  }

  public String Image(){
    return toString(amtrue);
  }

  public String toString(){
    return Image();
  }

  public long asLong(){
    return amtrue?1L:0L;
  }

  public int asInt(){
    return asInt(amtrue);
  }

  public static int asInt(boolean b){
    return b?1:0;
  }

  public boolean asBoolean(){
    return amtrue;
  }

  public boolean setto (String image){
    amtrue=For(image);
    return amtrue;
  }


  public static final Bool TRUE = new Bool(true);
  public static final Bool FALSE = new Bool(false);

  public static final int stuffMask(int pattern, int bitnum, boolean value){
    if(value){
      return pattern | (1<<bitnum);
    } else {
      return pattern &~(1<<bitnum);
    }
  }
  public static final long stuffMask(long pattern, int bitnum, boolean value){
    if(value){
      return pattern | (1<<bitnum);
    } else {
      return pattern &~(1<<bitnum);
    }
  }

  public static final boolean bitpick(int pattern, int bitnum){
    return (pattern & (1<<bitnum))!=0;
  }

  public static final boolean bitpick(long pattern, int bitnum){
    return (pattern & (1L<<bitnum))!=0;
  }

////////////////////////////
// a string as a sparse array with index encoded as a char
  public static final boolean flagPresent(int ch,String flagset){
    return StringX.NonTrivial(flagset) && flagset.indexOf(ch)>=0;
  }

  public static final boolean flagPresent(char ch,String flagset){
    return flagPresent((int)ch,flagset);
  }

  public static final boolean flagPresent(String s,String flagset){
    return StringX.NonTrivial(s)&&flagPresent(s.charAt(0),flagset);
  }

  public static final boolean For(int eye){
    return eye!=0;
  }

  public static final boolean isEven(int eye){
    return (eye&1)==0;
  }

  public static final boolean isOdd(int eye){
    return (eye&1)!=0;
  }

  public static final boolean For(long ell){
    return ell!=0;
  }

  // +++ write a tester for this!
  public static final boolean For(String trueorfalse){
    boolean ret = false;
    try {
      if(StringX.equalStrings(SHORTTRUE(), trueorfalse)) {
        ret = true;
      } else if(Boolean.valueOf(trueorfalse).booleanValue()) {
        ret = true;
      } else if(StringX.equalStrings("t", trueorfalse)) { // the database returns "t" for true!
        ret = true;
      } else if(StringX.parseInt(trueorfalse) == 1) {
        ret = true;
      } else {
        ret = false;
      }
    } catch (Exception ex) {
      // +++ ??? dbg.Caught(ex);
    } finally {
      return ret;
    }
  }

  public static final boolean[] MapFromLong(long ell){
    boolean[] map=new boolean[64];
    int i=0;
    for(long bitp=Long.MIN_VALUE;i<64;bitp>>>=1){
      map[i++]=(ell&bitp)!=0;
    }
    return map;
  }

  private static final String SHORTTRUE() {
    return "Y";
  }
  private static final String SHORTFALSE() {
    return "N";
  }

  public static final String TRUESTRING = (new Boolean(true)).toString();
  public static final String FALSESTRING = (new Boolean(false)).toString();
  public static final String toString(boolean isTrue) {
    return isTrue ? TRUESTRING : FALSESTRING;
  }
  private static final String TRUESTR = toString(true);
  private static final String FALSESTR = toString(false);
  public static final String TRUE() {
    return TRUESTR;
  }
  public static final String FALSE() {
    return FALSESTR;
  }

  public static final long LongFromMap(boolean [] map){
    long ell=0;
    int i=0;
    for(long bitp=Long.MIN_VALUE;i<64;bitp>>>=1){
      if(map[i++]){
        ell|=bitp;
      }
    }
    return ell;
  }

  public static final int signum(boolean positive){
    return positive? 1: -1;
  }

  public static final char signChar(boolean positive){
    return positive? '+': '-';
  }

  public static final char dash(boolean dash){
    return dash? '-': ' ';
  }

  public boolean equals(Object obj) {
    if (obj instanceof Boolean) {
      return amtrue == ((Boolean)obj).booleanValue();
    }
    if (obj instanceof Bool) {
      return amtrue == ((Bool)obj).amtrue;
    }
    return false;
  }

  /**
   * @return false is less than true
   */
  public int compareTo(Object obj){//override image compare
    if (obj instanceof Bool) {
      return asInt()-((Bool)obj).asInt();//+_+
    }
    return super.compareTo(obj);//fall back to string compare...
  }

}

