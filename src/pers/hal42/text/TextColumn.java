package pers.hal42.text;

import pers.hal42.logging.ErrorLogStream;

public class TextColumn extends TextList{//simple text one

  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(TextColumn.class);

  int width; // will have to be overidden as there isn't a default constructor

  public TextList add(String ess) {
    return super.split(ess,width,true/*word wrap*/);
  }

  // this makes something
  public TextColumn(TextList tl, int width) {
    this(width);
    if(tl != null) {
      addCentered(tl);
    }
  }

  public TextColumn(String toParse, int width) {
    this(width);
    addCentered(new TextList(toParse,width,TextList.SMARTWRAP_ON));
  }

  public TextColumn(int width){
    this();
    this.width = width;
  }

  private TextColumn(){
    super();
  }

  /**
   * add label/value pair with label on left margin and value at right margin
   */
  public void justified(String label,String value){
    add(Fstring.justified(width,label,value));
  }

  /**
   * add label/value pair with name on left margin and value at right margin,
   * with your choice of character to fill the space between
   */
  public void justified(String label,String value,char filler){
    add(Fstring.justified(width,label,value,filler));
  }

  /**
   * add centered text, surrounded by filler
   */
  public void centered(String label,char filler){
    add(Fstring.centered(label,width,filler));
  }

  /**
   * and entries from a list, centering them
   */
  public void addCentered(TextList tl) {
    if(tl != null) {
      for(int i = 0; i < tl.size(); i++) {
        centered(tl.itemAt(i), ' ');
      }
    }
  }

}

