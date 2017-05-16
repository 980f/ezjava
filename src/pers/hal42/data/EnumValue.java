package pers.hal42.data;

/**
 * todo: serious reworok, java Enum isn't much like the original TrueEnum hierarchy.
 */

public class EnumValue extends Value {
  Enum content;

  /**
   * @param design is called that as its value may be discarded, we are mostly interested in its underlying type
   */
  public EnumValue(Enum design) {
    content = design;
  }

  public ContentType charType() {
    return ContentType.select;
  }

  public Enum Content() {
    return content;
  }

  public Enum Value() {
    return content;
  }

  public String ImageFor(int digit) {//for keyboard picking
    return content.getClass().getEnumConstants()[digit].toString();
  }

  public String Image() {
    return content.name();
  }

  public boolean setto(String image) {
    //find a sibling enum by name
//    content=content.getClass().setto(image);
    return false;
  }

  public boolean setto(int token) {
    content = content.getClass().getEnumConstants()[token];
    return true;
  }

  public int asInt() {
    return content.ordinal();
  }

  public void Clear() {
    content = null;
  }

}

//$Id: EnumValue.java,v 1.8 2003/07/27 05:34:57 mattm Exp $
