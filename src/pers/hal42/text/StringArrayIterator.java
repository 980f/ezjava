package pers.hal42.text;

public class StringArrayIterator implements StringIterator {

  int pointer = 0;
  String things[];

  public StringArrayIterator(String ... things) {
    this.things = things;
  }

  @Override
  public boolean hasNext() {
    return pointer < things.length;
  }

  @Override
  public String next() {
    return things[pointer++];
  }

  void rewind() {
    pointer = 0;
  }

  boolean unget() {
    if (pointer > 0) {
      --pointer;
      return true;
    }
    return false;
  }
}
