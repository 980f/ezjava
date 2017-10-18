package pers.hal42.text;

/** an object with reversable String representation */
public interface Packable {
  void parse(String packed);

  String packed();

  /** convert to string and back again */
  default void canonicize() {
    parse(packed());
  }
}
