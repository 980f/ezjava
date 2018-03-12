package pers.hal42.text;

import pers.hal42.lang.Bool;

import static java.text.MessageFormat.format;
import static pers.hal42.lang.Index.BadIndex;
import static pers.hal42.lang.StringX.*;

/** key=value pair, with parsing to create them. */
public class CommandArgument {
  public String key;
  public String value;
  public char cutter;

  /** constructor for most common case of simple commandline arg */
  public CommandArgument(String pair, char cutter) {
    split(pair, cutter);
  }

  public boolean hasValue() {
    return NonTrivial(value);
  }

  /** constructor for most common case of simple commandline arg */
  public CommandArgument(String pair) {
    this(pair, '=');
  }

  public boolean hasKey() {
    return NonTrivial(key);
  }

  /** @returns whether both the key and value seem to be nontrivial */
  public boolean looksGood() {
    return hasKey() && hasValue();
  }

  /** split @param pair at char cutter. If cutter not present then the value is null and the key is pair.trim(). */
  public void split(String pair, char cutter) {
    this.cutter = cutter;
    splitAt(cutPoint(pair, cutter), pair);
  }

  public CommandArgument() {
    this(null);
  }

  /**
   * split at designated index, trim() each piece.
   *
   * @returns this
   */
  public CommandArgument splitAt(int splitter, String pair) {
    if (pair == null) {
      key = null;
      value = null;
    } else if (splitter == BadIndex || splitter >= pair.length()) {
      key = pair.trim();
      value = null;
    } else {
      key = pair.substring(0, splitter).trim();
      value = pair.substring(splitter + 1).trim();
    }
    return this;
  }

  /** @returns either the value member parsed, or if the value isn't parsable @param def */
  public int getValue(int def) {
    return NonTrivial(value) ? parseInt(value) : def;
  }

  /** @returns either the value member parsed, or if the value isn't parsable @param def */
  public double getReal(double def) {
    return parseDouble(value, def);
  }

  /** @returns either the value member parsed, or if the value isn't parsable @param def */
  public boolean getValue(boolean b) {
    return NonTrivial(value) ? Bool.For(value) : b;
  }

  /**
   * @returns either the value member parsed, or if the value isn't parsable @param def
   */
  public String getText(String def) {
    return TrivialDefault(value, def);
  }

  /**
   * @returns canonical image of name and value "whatever=..." or just 'whatever' for value free items
   */
  @Override
  public String toString() {
    return hasValue() ? format("{0}{1}{2}", key, cutter, value) : String.valueOf(key);
  }

}
