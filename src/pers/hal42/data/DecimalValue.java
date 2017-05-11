package pers.hal42.data;


import pers.hal42.lang.StringX;

public class DecimalValue extends Value {
  long content;

  public ContentType charType(){
    return ContentType.decimal;
  }

  public String Image(){
    return Long.toString(content);
  }

  public boolean setto(String image){
    content= StringX.parseLong(image);
    return true; //+++ detect overflow
  }

  public void Clear(){
    content=0;
  }

  public int asInt(){
    return (int) content;
  }

  public long asLong(){
    return content;
  }

  public DecimalValue(DecimalValue rhs){
    if(rhs!=null){
      content=rhs.content;
    } else {
      Clear();
    }
  }

  public DecimalValue(long init){
    content=init;
  }

  public DecimalValue(){
    Clear();
  }

}
