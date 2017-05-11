package pers.hal42.thread;

import pers.hal42.stream.ObjectFifo;

import java.util.Collections;
import java.util.Comparator;

public class PrioritizedQueue extends ObjectFifo {
  private Comparator ordering;

  public synchronized int put(Object obj){ //insertion sort
    if(obj instanceof Comparable){
//    try {
//      listlock.getMonitor();//we would do this if worried that comparator were defective
      int location=Collections.binarySearch(fifo,obj,ordering);
      //if of equal priority to something present then newest goes after first one
      //if nothing equal then goes whereever the search leaves it
      fifo.insertElementAt(obj,location<0?~location:location+1);
      return Size();
//    }
//    finally {
//      listlock.freeMonitor();
//    }
    }
    else {
      return super.put(obj);
    }
  }

  public PrioritizedQueue() {
    this(PriorityComparator.Normal());
  }

  public PrioritizedQueue(Comparator ordering) {
    this.ordering=ordering;
  }

}

