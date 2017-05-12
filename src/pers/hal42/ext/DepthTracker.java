package pers.hal42.ext;

/**
 * track stack depth, using 'try-with-resources'
 */


public class DepthTracker {
  /**
   * greatest depth of nesting
   */
  public Extremer<Integer> maxDepth=new Extremer<>();

  /** @returns present depth */
  public int asInt(){
    return nested.asInt();
  }

  public int max(){
    return maxDepth.extremum;
  }
  /**
   * number of unmatched braces at end of parsing
   */
  public CountedLock nested=new CountedLock();

  public static int main(String argv[]) {
    DepthTracker testee = new DepthTracker();

    try (CountedLock arf = testee.open()) {
      System.out.println("first level " + testee.maxDepth.getExtremum(~0));
      try (CountedLock arf2 = testee.open()) {
        System.out.println("second level " + testee.maxDepth.getExtremum(~0));
        try (CountedLock arf3 = testee.open()) {
          System.out.println("third level " + testee.maxDepth.getExtremum(~0));
        } finally {
          System.out.println("third exit " + arf2.asInt());
        }

      } finally {
        System.out.println("second exit " + arf.asInt());
      }
    } finally {
      System.out.println("first exit " + testee.nested.asInt());
    }
    return testee.maxDepth.getExtremum(~0);
  }

  public void reset() {
    maxDepth.reset();
    nested.setto(0);
  }

  public CountedLock open() {
    nested.open();
    maxDepth.inspect(nested.asInt());
    return nested;
  }


  public CountedLock open(int loc) {
    nested.open();
    maxDepth.inspect(loc, nested.asInt());
    return nested;
  }

}
