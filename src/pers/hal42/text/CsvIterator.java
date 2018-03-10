package pers.hal42.text;

/** adds quoteing to SimpleCsviterator */

public class CsvIterator extends SimpleCsvIterator {

  public CsvIterator(char comma, String line) {
    super(false, comma, line);
  }

  public CsvIterator(String line) {
    this(',', line);
  }

  @Override
  public boolean hasNext() {
    return super.hasNext();
  }

  @Override
  public String next() {
    String part = super.next();
    if (part == null) {
      return "";
    }
    if (part.length() == 0) {
      return "";
    }
    if (part.charAt(0) == '"') {
      while (part.charAt(part.length() - 1) != '"') {
        if (hasNext()) {
          part += next();//maydo: stringbuffer if we get here often.
        } else {
          part += '"';//inefficient but I'm in a hurry to code this.
          break;
        }
      }
      part = part.substring(1, part.length() - 1);//remove quotes.
    }
    return part;
  }
}
