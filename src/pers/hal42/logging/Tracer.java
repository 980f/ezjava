package pers.hal42.logging;

import pers.hal42.lang.StringX;

public class Tracer extends ErrorLogStream {
  public String location;

  public String prefix(){
    return location !=null? "At "+location+":":"";
  }

  public String functionName(){
    return ActiveMethod;
  }

  public void mark(String location){
    this.location=location;
    if(StringX.NonTrivial(location)) {
      VERBOSE("mark");
    }
  }

  public void Caught(Throwable caught){
    super.Caught(prefix(),caught);
  }

  public void ERROR(String msg){
    super.ERROR(prefix()+msg);
  }

  public void WARNING(String msg){
    super.WARNING(prefix()+msg);
  }

  public void VERBOSE(String msg){
    super.VERBOSE(prefix()+msg);
  }

  protected Tracer(LogSwitch ls){
    super(ls);
  }

  public Tracer(Class claz,String suffix) {
    super(LogSwitch.getFor(claz,suffix));
    location="start";
  }

  public Tracer(Class claz) {
    this(claz,null);
  }

  public Tracer(Class claz,int spam){
    this(claz,null,spam);
  }

  public Tracer(Class claz,String suffix,int spam){
    this(claz,suffix);
    this.setLevel(spam);
  }

}

