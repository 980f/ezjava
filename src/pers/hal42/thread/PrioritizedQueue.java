package pers.hal42.thread;

import pers.hal42.stream.ObjectFifo;

import java.util.Collections;
import java.util.Comparator;

public class PrioritizedQueue<Qtype> extends ObjectFifo<Qtype> {
  private final Comparator<Qtype> ordering;

  public PrioritizedQueue() {
    this(Comparator.comparingInt(Object::hashCode));
  }

  public PrioritizedQueue(Comparator<Qtype> ordering) {
    this.ordering = ordering;
  }

  public int put(Qtype obj) { //insertion sort
    synchronized (fifo) {
      int location = Collections.binarySearch(fifo, obj, ordering);
      //if of equal priority to something present then newest goes after first one
      //if nothing equal then goes whereever the search leaves it
      fifo.insertElementAt(obj, location < 0 ? ~location : location + 1);
      return Size();
    }
  }

}

