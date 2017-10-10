package pers.hal42.text;

import pers.hal42.lang.Bool;
import pers.hal42.lang.StringX;

import static pers.hal42.lang.Index.BadIndex;

public class NamedValue {
  public String key;
  public String value;

  public boolean isKeyed() {
    return StringX.NonTrivial(key);
  }

  public boolean hasValue() {
    return StringX.NonTrivial(value);
  }

  public boolean looksGood() {
    return isKeyed() && hasValue();
  }

  /** constructor for most common case of simple commandline arg */
  public NamedValue(String pair) {
    split(pair, '=');
  }

  public void split(String pair, char cutter) {
    splitAt(StringX.cutPoint(pair, cutter), pair);
  }

  public NamedValue() {
    this(null);
  }

  public void splitAt(int splitter, String pair) {
    if (pair == null) {
      key = null;
      value = null;
    } else if (splitter == BadIndex || splitter >= pair.length()) {
      key = pair;
      value = null;
    } else {
      key = pair.substring(0, splitter);
      value = pair.substring(splitter + 1);
    }
  }

  public int getValue(int def) {
    return StringX.NonTrivial(value) ? StringX.parseInt(value) : def;
  }

  public double getReal(double def) {
    return StringX.parseDouble(value, def);
  }

  public boolean getValue(boolean b) {
    return StringX.NonTrivial(value) ? Bool.For(value) : b;
  }
}
