package pers.hal42.util;

import pers.hal42.lang.StringX;
import pers.hal42.thread.Counter;

import static pers.hal42.text.Ascii.bracket;

public class InstanceNamer {

  private String prefix;
  private Counter count = new Counter(); // Counter already has mutexing/synchronizing

  public String Next(){
    return prefix+ bracket(count.incr());
  }

  public InstanceNamer(String prefix) {
    this.prefix=StringX.OnTrivial(prefix,"Instance");
  }
}

