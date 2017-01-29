/**
 * TODO:
 * +++ ColumnJustification and/or TextColumn rework to:
 *      Justification.Left fill right side with spacer
 *      Justification.right fill left side with spacer
 *      Justification.centered fill both sides, if number of fill chars is oddput the exgtra one on {right|left}
 *      Justification.Paired fill between given pair of items, if item don't fit create two lines one .left and one .right.
 *        and not yet implemented:
 *      Justification.Full spread the fill between words where word is defined by Character.isWhitespace().
 */
package pers.hal42.util;

import pers.hal42.lang.StringX;
import pers.hal42.lang.Fstring;

public class FormattedLineItem implements isEasy {//carries info, doesn't actually DO any formatting
  // data
  public String              name          = "";
  public String              value         = "";
  public char                filler        = ' ';
  public ColumnJustification justification = new ColumnJustification(ColumnJustification.PLAIN);
  // keys
  private static final String nameKey          = "name";
  private static final String valueKey         = "value";
  private static final String fillerKey        = "fill";
  private static final String justificationKey = "just";
  // quickies
  public static final ColumnJustification justified  = new ColumnJustification(ColumnJustification.JUSTIFIED);
  public static final ColumnJustification centered   = new ColumnJustification(ColumnJustification.CENTERED);
  public static final ColumnJustification plain      = new ColumnJustification(ColumnJustification.PLAIN);
  public static final ColumnJustification winged     = new ColumnJustification(ColumnJustification.WINGED);

  //raison d'etre
  /**
   * convert to string now that we know the width of the presentation device
   */
  public String formatted(int width){
    switch(justification.Value()) {
      default:
      case ColumnJustification.PLAIN:     return name + StringX.TrivialDefault(value , "");
      case ColumnJustification.JUSTIFIED: return Fstring.justified(width, name,value, (filler == 0) ? '.' : filler);
      case ColumnJustification.CENTERED:  return Fstring.centered(name, width, filler);
      case ColumnJustification.WINGED:    return Fstring.winged(name, width);
    }
  }

  /**
   * @return number of interesting chars.
   */
  public int meat(){
    switch(justification.Value()) {
      default:
//      case ColumnJustification.CENTERED:
//      case ColumnJustification.PLAIN:
        return name.length();
      case ColumnJustification.JUSTIFIED:
        return name.length()+value.length();
      case ColumnJustification.WINGED:
        return name.length()+2+2;//wings include spaces around centered text/
    }
  }

  /**
   * can double without the line wrapping.
   */
  public boolean canDouble(int fullwidth){
    return meat()<=fullwidth/2;
  }

  /**
   * @param fullwidth is width of device for normalfont
   * @param doubleFont and normalFont are the control characters to wrap the line with
   */
  public String doubleWide(int fullwidth,char doubleFont,char normalFont){
    return String.valueOf(doubleFont)+formatted(fullwidth/2)+String.valueOf(normalFont);
  }

  public void save(EasyCursor ezp){
    ezp.setString(nameKey         , name);
    ezp.setString(valueKey        , value);
    ezp.setChar  (fillerKey       , filler);
    ezp.saveEnum (justificationKey, justification);
  }

  public void load(EasyCursor ezp) {
    name   = ezp.getString(nameKey);
    value  = ezp.getString(valueKey);
    filler = ezp.getChar(fillerKey,' ');//spaces were coming back as nulls when going to properties
    ezp.loadEnum(justificationKey, justification);
  }

  // constructors
  public FormattedLineItem() {//for easyCursor getObject
  //see initializers
  }

  public FormattedLineItem(EasyCursor ezp) {
    this();
    load(ezp);
  }

  public FormattedLineItem(String name, String value, char filler, ColumnJustification just) {
    this();
    this.name           = name;
    this.value          = value;
    this.filler         = filler;
    this.justification  = just;
  }

  public FormattedLineItem(String name, char filler) {
    this(name,"",filler,centered);
  }

  public FormattedLineItem(String name, String value, char filler) {
    this(name, value, filler, justified);
  }

  // default filler = '.', default just = justified
  public FormattedLineItem(String name, String value) {
    this(name, value, '.', justified);
  }

  public FormattedLineItem(String name) {
    this(name, "", '.', plain);
  }

  public static final FormattedLineItem winger(String name) {
    return new FormattedLineItem(name, "", ' ', winged);
  }

  public static final FormattedLineItem blankline() {
    return new FormattedLineItem("", "", ' ', plain);
  }

  public static final FormattedLineItem pair(String name,String value) {
    return new FormattedLineItem(name, value);
  }

  public String toSpam(){//4debug
    return this.justification.Image()+":"+this.name+", *["+this.filler+"],"+this.value;
  }

}

