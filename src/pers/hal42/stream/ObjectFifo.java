package pers.hal42.stream;

import java.util.Vector;

public class ObjectFifo {
  protected Vector fifo=null;

  public ObjectFifo() {
    this(100);
  }

  public ObjectFifo(int presize) {
    fifo=new Vector(presize, presize);
  }

  public synchronized Object [] snapshot(){
    return fifo.toArray();
  }

  public synchronized Object next(){
    int size=fifo.size();
    if(size-->0){
      Object gotten=fifo.elementAt(size);
      fifo.removeElementAt(size);
      return gotten;
    } else {
      return null;
    }
  }

  public synchronized int put(Object obj){
    fifo.insertElementAt(obj, 0);
    return fifo.size();
  }

  public synchronized int atFront(Object obj){
    fifo.add(obj);
    return fifo.size();
  }

  public synchronized void Clear(){
    fifo.setSize(0);
  }

/////////////////////
  protected synchronized boolean remove(Object obj){
    return fifo.remove(obj);
    //does NOT interrupt. agent does NOT need to wake up to service a removal of
    //something that it hasn't looked at yet.
  }

  /**
   * remove anything satisfying obj.equals(fifoObj)
   */
  public synchronized int removeAny(Object obj){
    int count=0;
    while(fifo.remove(obj)){
      ++count;
    }
    return count;
  }

  /**
   * This exists for reporting purposes only.  DO NOT use it to iterate!
   */
  public final int Size() {
    return fifo.size();
  }

  public boolean isEmpty(){
    return fifo.size()<=0;
  }

}

