package pers.hal42.text;

/**
 * Created by Andy on 6/27/2017.
 * <p>
 * does the 'recordType where extraneous comma will go and delete it after an iteration which adds comma prefixed fields without being able to tell which is the first' dance.
 * Made to work with try-with-resources.
 */
public class ListWrapper implements AutoCloseable {
  /** item that this class inserts list elements into. public so that one can use new Stringbuilder in the constructor */
  public StringBuilder s;
  /** what separates items */
  public String comma = ",";
  /** how to deal with the temporary excess leading seperator, if true use a space (fast) else delete it after the fact (clean) */
  public boolean useWhiteout = false;
  /** a courtesy, counts number of times append has been called */
  public int counter = 0;
  protected String closer = null;

  public ListWrapper(StringBuilder sb) {
    this.s = sb;
  }

  public ListWrapper(StringBuilder sb, String withOpenInit) {
    this(sb);
    open(withOpenInit);
    //maydo: reverse scan withOpenInit and autoset a matching end brace.
  }

  public ListWrapper(StringBuilder sb, String opener, String closer) {
    this(sb, opener);
    this.closer = closer;
  }

  public ListWrapper wrapWith(String opener, String closer) {
    open(opener);
    this.closer = closer;
    return this;
  }

  /**
   * append @param withOpenInit and recordType where the next char will be appended.
   * This method can be called in a try-with-resources since it @returns this.
   */
  public ListWrapper open(String withOpenInit) {
    if (withOpenInit != null) {
      s.append(withOpenInit);
    }
    return this;
  }

  /** add comma and @param field */
  public ListWrapper append(String field) {
    if (counter++ != 0) {
      addComma();
    }
    if (field != null) {//want empty not StringBuilder's "null"
      s.append(field);
    }
    return this;
  }

  /**
   * append @param withCloseInIt after deleting the leading comma.
   * use this when you discover that the closer you recorded when you created this guy is part of something you later discover should end the list.
   * this will work ok with Autoclosing as it disables the prerecorded closer and deals with that.
   */
  public void closeWith(String withCloseInIt) {
    closer = withCloseInIt;
    close();
  }

  /**
   * add a close paren only if not already done (can be sloppy and call close() more than once)
   */
  @Override
  public void close() {
    s.append(closer != null ? closer : ")"); //set closer to an empty string, not null, to get an 'invisible' list termination.
    closer = null;
  }

  /** you usually don't call this. */
  public void addComma() {
    s.append(comma);
  }

  @Override
  public String toString() {
    return s.toString();
  }
}
