package pers.hal42.text;

import pers.hal42.transport.Xform;

/** an object with reversable String representation */
@Xform()
public interface Packable {
  void parse(String packed);

  String packed();

  /** convert to string and back again */
  default void canonicize() {
    parse(packed());
  }
}
