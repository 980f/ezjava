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
        do {
          if (hasNext()) {
            sb.append(comma).append(next());
          } else {
            sb.append('"');
            break;
          }
        } while (sb.charAt(sb.length() - 1) != '"');
        part = sb.toString();
      }
      part = part.substring(1, part.length() - 1);//remove quotes.
    }
    return part;
  }
}
