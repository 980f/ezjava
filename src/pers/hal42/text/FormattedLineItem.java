package pers.hal42.text;


import pers.hal42.lang.StringX;
import pers.hal42.transport.EasyCursor;
import pers.hal42.transport.isEasy;

/**
 * TODO:2
 * +++ ColumnJustification and/or TextColumn rework to:
 * Justification.Left fill right side with spacer
 * Justification.right fill left side with spacer
 * Justification.centered fill both sides, if number of fill chars is oddput the exgtra one on {right|left}
 * Justification.Paired fill between given pair of items, if item don't fit create two lines one .left and one .right.
 * and not yet implemented:
 * Justification.Full spread the fill between words where word is defined by Character.isWhitespace().
 */
public class FormattedLineItem implements isEasy {//carries info, doesn't actually DO any formatting
  // data
  public String name = "";
  public String value = "";
  public char filler = ' ';
  public ColumnJustification justification = ColumnJustification.PLAIN;
  // keys
  private static final String nameKey = "name";
  private static final String valueKey = "value";
  private static final String fillerKey = "fill";
  private static final String justificationKey = "just";

  //raison d'etre

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
    this.name = name;
    this.value = value;
    this.filler = filler;
    this.justification = just;
  }

  public FormattedLineItem(String name, char filler) {
    this(name, "", filler, ColumnJustification.CENTERED);
  }

  public FormattedLineItem(String name, String value, char filler) {
    this(name, value, filler, ColumnJustification.JUSTIFIED);
  }

  // default filler = '.', default just = justified
  public FormattedLineItem(String name, String value) {
    this(name, value, '.', ColumnJustification.JUSTIFIED);
  }

  public FormattedLineItem(String name) {
    this(name, "", '.', ColumnJustification.PLAIN);
  }

  /**
   * convert to string now that we know the width of the presentation device
   */
  public String formatted(int width) {
    switch (justification) {
      default:
      case PLAIN:
        return name + StringX.TrivialDefault(value, "");
      case JUSTIFIED:
        return Fstring.justified(width, name, value, (filler == 0) ? '.' : filler);
      case CENTERED:
        return Fstring.centered(name, width, filler);
      case WINGED:
        return Fstring.winged(name, width);
    }
  }

  /**
   * @return number of interesting chars.
   */
  public int meat() {
    switch (justification) {
      default:
      case CENTERED:
      case PLAIN:
        return name.length();
      case JUSTIFIED:
        return name.length() + value.length();
      case WINGED:
        return name.length() + 2 + 2;//wings include spaces around centered text/
    }
  }

  /**
   * can double without the line wrapping.
   */
  public boolean canDouble(int fullwidth) {
    return meat() <= fullwidth / 2;
  }

  /**
   * @param fullwidth is width of device for normalfont
   * @param doubleFont and normalFont are the control characters to wrap the line with
   */
  public String doubleWide(int fullwidth, char doubleFont, char normalFont) {
    return String.valueOf(doubleFont) + formatted(fullwidth / 2) + String.valueOf(normalFont);
  }

  public void save(EasyCursor ezp) {
    ezp.setString(nameKey, name);
    ezp.setString(valueKey, value);
    ezp.setChar(fillerKey, filler);
    ezp.saveEnum(justificationKey, justification);
  }

  public void load(EasyCursor ezp) {
    name = ezp.getString(nameKey);
    value = ezp.getString(valueKey);
    filler = ezp.getChar(fillerKey, ' ');//spaces were coming back as nulls when going to properties
    ezp.loadEnum(justificationKey, justification);
  }

  public String toSpam() {//4debug
    return this.justification.toString() + ":" + this.name + ", *[" + this.filler + "]," + this.value;
  }

  public static FormattedLineItem winger(String name) {
    return new FormattedLineItem(name, "", ' ', ColumnJustification.WINGED);
  }

  public static FormattedLineItem blankline() {
    return new FormattedLineItem("", "", ' ', ColumnJustification.PLAIN);
  }

  public static FormattedLineItem pair(String name, String value) {
    return new FormattedLineItem(name, value);
  }

}

