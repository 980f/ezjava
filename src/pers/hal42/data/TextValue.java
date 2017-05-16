package pers.hal42.data;

/**
 * TextValue, a database column attribute
 */
public class TextValue extends Value {
  protected String content;

  public TextValue(String defawlt) {
    content = defawlt;
  }

  public TextValue() {
    this("DEFAULT"); //--- just for initial testing
  }

  public ContentType charType() {
    return ContentType.alphanum;
  }

  public String Image() {
    return String.valueOf(content);
  }

  public boolean setto(String image) {
    content = image; //+_+
    return true; //this promiscuous type accepts anything superficially textual
  }

  public void Clear() {
    content = "";
  }

}
