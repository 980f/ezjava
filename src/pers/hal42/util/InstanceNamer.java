package pers.hal42.util;

import pers.hal42.lang.StringX;

public class InstanceNamer {

  private String prefix;
  private Counter count = new Counter(); // Counter already has mutexing/synchronizing

  public String Next(){
    return prefix+Ascii.bracket(count.incr());
  }

  public InstanceNamer(String prefix) {
    this.prefix=StringX.OnTrivial(prefix,"Instance");
  }
}

