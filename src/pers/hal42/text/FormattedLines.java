package pers.hal42.util;

import  java.util.Vector;
import pers.hal42.lang.ReflectX;

public class FormattedLines implements isEasy {
  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(FormattedLines.class, ErrorLogStream.VERBOSE);
  private static final String countKey = "count";

  protected char fillChar=' '; //for converting raw strings

  Vector storage=new Vector();
  public int size(){
    return storage!=null ? storage.size(): 0;
  }

  public boolean NonTrivial(){
    return size()>0;
  }

  public static int Sizeof(FormattedLines probate){
    return probate!=null? probate.size() : 0;
  }

  public FormattedLines reverse(){
    VectorX.reverse(storage);
    return this;
  }

  /**
   * convert to strings now that we know the @param width of the presentation device
   * @param paragraph is added to if it exists, else a new one is created.
   */
  public TextList formatted(int width,TextList paragraph){
    if(paragraph==null){
      paragraph=new TextList(storage.size());
    }
    for(int i=0;i<storage.size();i++){//#preserve order
      paragraph.add(itemAt(i).formatted(width));
    }
    return paragraph;
  }

  /**
   * convert to strings now that we know the @param width of the presentation device
   */
  public TextList formatted(int width){
    return formatted(width,null);
  }

  public boolean add(Object arf){
    if(arf==null){
      return true;//successfully added nothing
    }
    if(arf instanceof FormattedLineItem){
      return storage.add(arf);
    }
    if(arf instanceof String){
      return storage.add(new FormattedLineItem((String)arf,fillChar));
    }
    if(arf instanceof TextList){//+_+could expedite this frequently used case
      add(((TextList)arf).Vector());
    }
    if(arf instanceof Vector){//this includes FormattedLines itself.
     //failed to call back this class// return super.addAll((Vector)arf);
      Vector victor=(Vector)arf;//cast
      int count=victor.size();
      for(int i=0;i<count;i++){//retain order
        if(!add(victor.elementAt(i))){
          //do we try the rest?
          //no: quit on any error-for debug reasons
          return false;
        }
      }
      return true;
    }
    //could throw unsupportedCast, but we are too nice for that
    return false;//didan't add it!
  }

  public boolean add(String left,String right){
    return add(FormattedLineItem.pair(left,right));
  }

  public FormattedLineItem itemAt(int index) {
    return (index < storage.size()) ? (FormattedLineItem)storage.elementAt(index) : (FormattedLineItem)null;
  }

  public FormattedLines(){
    storage=new Vector();
  }

  public FormattedLines(Object arf){
    this();
    add(arf);
  }

  public FormattedLines(int initialCapacity) {
    storage=new Vector(initialCapacity);
  }

  public static final FormattedLines Empty() {
    return new FormattedLines();
  }
///////////////////////////////////////////////
// this had to go somewhere... here for when we can do two columns
/**
 * @param te value will get destroyed! it is for getting to the underlying class
 */
public static final FormattedLines menuListing(TrueEnum te,TrueEnum parent){
  if(te == null){
    return Empty();
  }
  int size=te.numValues();
  FormattedLines newone=new FormattedLines(size+1);
//this was ugly  newone.add(FormattedLineItem.pair("Menu Type:",ReflectX.justClassName(te)));
  if(TrueEnum.IsLegal(parent)){
    newone.add(  ReflectX.justClassName(parent),parent.menuImage());
  }
  for(int i=0;i<size;i++){
    te.setto(i);
    newone.add(te.menuImage());
  }
  return newone;
}

////////////////////////////////////////////////////////
//isEasy and other transport related items
  public EasyCursor asProperties(){
    EasyCursor ezp = new EasyCursor();
    save(ezp);
    return ezp;
  }
//new way, 1.003
  public void save(EasyCursor ezc){
    ezc.setVector(storage);
  }
  public void load(EasyCursor ezc){
    storage=ezc.getVector(FormattedLineItem.class);
  }

// pre 1.003
  private FormattedLines fromString(String from) {
    EasyCursor ezp = new EasyCursor();
    ezp.fromString(from, true);
    dbg.VERBOSE("fromString() using: " + ezp);
    load(ezp);
    return this;
  }

}

