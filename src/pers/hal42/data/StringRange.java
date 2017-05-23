package pers.hal42.data;

public class StringRange extends ObjectRange<String> {
//  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(StringRange.class);

  protected StringRange(String one, String two, boolean sorted) {
    super(one, two, sorted);
  }

//  public Comparable filter(String input) {
//    return input;
//  }

  public static StringRange New(String one, String two) {
    return new StringRange(one, two, false);
  }

  public static StringRange NewSorted(String one, String two) {
    return new StringRange(one, two, true);
  }

}
