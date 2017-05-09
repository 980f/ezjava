package pers.hal42.data;

/**
 * Title:        $Source: /cvs/src/net/paymate/data/LongRange.java,v $
 * Description:
 * Copyright:    Copyright (c) 200?
 * Company:      PayMate.net
 * @author PayMate.net
 * @version $Revision: 1.2 $
 */

import java.lang.Long;
public class LongRange extends ObjectRange {

  public LongRange() {
    setBoth(0,0);
  }

  public LongRange setBoth(long one, long two) {
    return (LongRange) setBoth(new Long(one), new Long(two));
  }

  public long low() {
    return ((Long)one).longValue();
  }

  public long high() {
    return ((Long)two).longValue();
  }

}
