package pers.hal42.data;

/**
 * <p>Title: $Source: /cvs/src/net/paymate/data/PayInfo.java,v $</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: PayMate.net</p>
 * @author PayMate.net
 * @version $Revision: 1.2 $
 */

import pers.hal42.lang.StringX;

public class PayInfo {

  public String tt;
  public String pt;
  public String in;

  public PayInfo() {
    clear();
  }

  public PayInfo(String tt, String pt, String in) {
    super();
    this.tt = tt;
    this.pt = pt;
    this.in = in;
  }

  public PayInfo(String catted) {
    parse(catted);
  }

  public void clear() {
    tt = " ";
    pt = " ";
    in = "  ";
  }

  public boolean equals(Object o) {
    if((o != null) && (o instanceof PayInfo)) {
      PayInfo pi = (PayInfo)o;
      return StringX.equalStrings(tt, pi.tt) && StringX.equalStrings(pt, pi.pt) && StringX.equalStrings(in, pi.in);
    }
    return false;
  }

  private final String ensure(String catted) {
    return StringX.fill(catted, ' ', 4, false);
  }

  public void parse(String catted) {
    catted = ensure(catted);
    tt = StringX.subString(catted, 0, 1);
    pt = StringX.subString(catted, 1, 2);
    in = StringX.subString(catted, 2, 4);
  }

  public String cat() {
    return ensure(""+tt+pt+in); // ick
  }
}

