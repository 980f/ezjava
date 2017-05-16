package pers.hal42.data;

/**
 * Title:        $Source: /cvs/src/net/paymate/data/LongRange.java,v $
 * Description:
 * Copyright:    Copyright (c) 200?
 * Company:      PayMate.net
 *
 * @author PayMate.net
 * @version $Revision: 1.2 $
 */

public class LongRange extends ObjectRange<Long> {

  public LongRange() {
    setBoth(0, 0);
  }

  public LongRange setBoth(long one, long two) {
    return (LongRange) setBoth(new Long(one), new Long(two));
  }

  public long low() {
    return (long) one;
  }

  public long high() {
    return (Long) two;
  }

}
