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
      if (part.charAt(part.length() - 1) != '"') {
        final StringBuilder sb = new StringBuilder(part);
        sb.deleteCharAt(0);
        while (hasNext()) {
          sb.append(comma).append(next());
          if (sb.charAt(sb.length() - 1) == '"') {
            sb.deleteCharAt(sb.length() - 1);
            break;
          }
        }
        part = sb.toString();
      } else {
        if (part.length() > 2) {
          part = part.substring(1, part.length() - 1);//remove quotes.
        }
      }
    }
    return part;
  }
}
