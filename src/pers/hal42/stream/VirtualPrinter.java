package pers.hal42.stream;

import pers.hal42.text.TextList;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class VirtualPrinter extends PrintStream {
  protected ByteArrayOutputStream sbos ;

  private VirtualPrinter(ByteArrayOutputStream bs) {
    super(bs);
    sbos=bs;
  }

  public VirtualPrinter() {
    this(new ByteArrayOutputStream());
  }

  public String backTrace(){
    try {
      return String.valueOf(sbos);
    } finally {
      sbos.reset();
    }
  }

  public TextList Image(int width){
    return new TextList( backTrace(),width,TextList.SMARTWRAP_ON);
  }

}

