package pers.hal42.data;

public class LongRange extends ObjectRange<Long> {

  public LongRange() {
    setBoth(0, 0);
  }

  public LongRange setBoth(long one, long two) {
    return (LongRange) setBoth(new Long(one), new Long(two));
  }

  public long low() {
    return one;
  }

  public long high() {
    return two;
  }

}
