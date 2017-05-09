package pers.hal42.lang;


import java.util.BitSet;

public class BitSetIterator {
  int ptr = 0;
  BitSet bits;

  public BitSetIterator(byte[] bytes) {
    bits = new BitSet(bytes.length * 8);
    for (int i = 0, bit=0; i < bytes.length; i++) {
      for(int mask = 128; mask > 0; mask /= 2) {
        if ((bytes[i] & mask) > 0) {
          bits.set(bit);
        }
        bit++;
      }
    }
  }

  public BitSetIterator rewind(){
    ptr=0;
    return this;
  }

  private boolean isValid(int i) {
    return i < bits.size();
  }

  public boolean haveMore(){
    return isValid(ptr);
  }

  public boolean next(){
    try {
      return peek();
    }
    finally {
      ++ptr;
    }
  }

  public boolean peek(){
    return isValid(ptr)&& bits.get(ptr);
  }

}

