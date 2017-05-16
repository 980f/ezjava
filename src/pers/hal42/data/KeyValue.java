package pers.hal42.data;

/**
 * a letter used as an enum
 */
public class KeyValue extends Value {
  Character content;

  public KeyValue() {
    Clear();
  }

  public int asInt() {
    return Character.digit(content, 10);
  }

  public ContentType charType() {
    return ContentType.select;
  }

  public String Image() {
    return content.toString();
  }

  public boolean setto(String image) {
    content = image.charAt(0);
    return true; //so long as we are 'arbitrary'
  }

  public void Clear() {
    content = '0';
  }

}
