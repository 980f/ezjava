package pers.hal42.text;

/**
 * Created by Andy on 6/27/2017.
 * <p>
 * does the 'record where extraneous comma will go and delete it after an iteration which adds comma prefixed fields without being able to tell which is the first' dance.
 * Made to work with try-with-resources.
 */
public class ListWrapper implements AutoCloseable {
  public StringBuilder s;//sb is final, damn them.
  public String comma = ",";
  protected int excessCommaAt = -1;
  private String closer = null;

  public ListWrapper(StringBuilder sb) {
    this.s = sb;
  }

  public ListWrapper(StringBuilder sb, String withOpenInit) {
    this.s = sb;
    open(withOpenInit);
  }

  public ListWrapper(StringBuilder sb, String opener, String closer) {
    this.s = sb;
    wrapWith(opener, closer);
  }

  public void wrapWith(String opener, String closer) {
    open(opener);
    this.closer = closer;
  }

  /**
   * append @param withOpenInit and record where the next char will be appended.
   * This method can be called in a try-with-resources since it @returns this.
   */
  public ListWrapper open(String withOpenInit) {
    s.append(withOpenInit);
    excessCommaAt = s.length();
    return this;
  }

  public ListWrapper append(String field) {
    addComma();
    s.append(field);
    return this;
  }

  /**
   * append @param withCloseInIt after deleting the leading comma
   * this will work ok with Autoclosing
   */
  public void closeWith(String withCloseInIt) {
    if (excessCommaAt >= 0) {
      s.deleteCharAt(excessCommaAt);
      excessCommaAt = -1;
      closer = null;
    }
    s.append(withCloseInIt);
  }

  /**
   * add a close paren and remove comma only if opening paren was recorded.
   */
  @Override
  public void close() {
    if (excessCommaAt >= 0) {
      s.append(closer != null ? closer : ")");
      s.deleteCharAt(excessCommaAt);
      excessCommaAt = -1;
      closer = null;
    }
  }

  public void addComma() {
    s.append(comma);
  }
}
