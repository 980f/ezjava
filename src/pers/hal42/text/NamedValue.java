package pers.hal42.text;

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

  public void splitAt(int splitter, String pair) {
    if (pair == null) {
      key = null;
      value = null;
    } else if (splitter == BadIndex || splitter >= pair.length() - 1) {
      key = pair;
      value = null;
    } else {
      key = pair.substring(0, splitter);
      value = pair.substring(splitter + 1);
    }
  }

  public void split(String pair, char cutter) {
    splitAt(StringX.cutPoint(pair, cutter), pair);
  }

  public int getValue(int def) {
    return StringX.NonTrivial(value) ? StringX.parseInt(value) : def;
  }

  public double getReal(double def) {
    return StringX.parseDouble(value, def);
  }
}
