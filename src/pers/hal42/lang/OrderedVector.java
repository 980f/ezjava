package pers.hal42.lang;

import java.util.Comparator;
import java.util.Vector;

public class OrderedVector {
  private Vector storage;
  Comparator ordering;
  Class filter=null;
  boolean unique=false;

  public synchronized Object [] snapshot(){
    return storage.toArray();
  }

  //NOT sync'ed. user must be robust against spontaneous changes!
  public int length(){
    return storage.size();
  }

  public synchronized boolean insert(Object arf){
    if(ObjectX.typeMatch(arf,filter)){//chagne to "if arf can be cast to filter..."
      if(unique){
        return VectorX.uniqueInsert(storage,arf,ordering);
      } else {
        VectorX.orderedInsert(storage,arf,ordering);
        return true;
      }
    }
    return false;
  }

  public synchronized Object itemAt(int i){
    if(i>=0 && i<storage.size()){
      return storage.elementAt(i);
    } else {
      try {
        return filter.newInstance();
      }
      catch (Exception ex) {//npe, IllegalAccessException
        return null;
      }
    }
  }

  private OrderedVector(Comparator ordering,int prealloc,Class filter,boolean unique) {
    storage=new Vector(prealloc);
    this.ordering= ordering!=null? ordering : new NormalCompare();//swallow exceptions on ordering
    this.filter=filter; //null is ok
    this.unique=unique;
  }

  public static OrderedVector New(Comparator ordering,Class filter,boolean unique,int prealloc){
    return new OrderedVector(ordering,prealloc,filter,unique);
  }
  public static OrderedVector New(Comparator ordering,Class filter,int prealloc){
    return new OrderedVector(ordering,prealloc,filter,false);
  }
  public static OrderedVector New(Comparator ordering,Class filter,boolean unique){
    return new OrderedVector(ordering,0,filter,unique);
  }
  public static OrderedVector New(Class filter,boolean unique){
    return new OrderedVector(null,0,filter,unique);
  }
  public static OrderedVector New(Class filter){
    return new OrderedVector(null,0,filter,false);
  }

}

