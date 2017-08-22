package pers.hal42.transport;

import pers.hal42.lang.Finally;

public class TreeName implements Finally.Lambda {
  protected StringBuilder cursor = new StringBuilder(100);
  private final String Sep;
  private final int sepLength;
  private int lastdot = 0;
  private final Finally popper = new Finally(this);

  public TreeName(String slash) {
    Sep = slash;
    sepLength = Sep.length();
  }

  private void clip() {
    cursor.setLength(lastdot);
  }

  public String leaf(String child) {
    try {
      return cursor.append(child).toString();
    } finally {
      clip();
    }
  }

  /** @returns an autoclosable that will call pop, so that you can use try-with-resources to keep the conceptual name stack sane. */
  public Finally push(String more) {
    cursor.append(more).append(Sep);
    lastdot = cursor.length();
    return popper;
  }

  @Override //Finally
  public void pop() {
    lastdot = 1 + cursor.lastIndexOf(Sep, lastdot - sepLength - 1);
    clip();
  }
}
