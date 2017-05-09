package pers.hal42.stream;
import java.util.*;

abstract public class BaseSortFile implements Comparator {
  protected boolean descending;
  protected Comparator secondCriterion;

  protected BaseSortFile(boolean descending) {
    this.descending=descending;
    this.secondCriterion=null;
  }

  public Comparator setNextCritierion(Comparator nextone){
    this.secondCriterion=nextone;//someday chain if class supports it.
    return this;
  }

  protected int chain(int first,Object o1, Object o2){
    return first!=0? first : (secondCriterion!=null? secondCriterion.compare(o1,o2):0);
  }

  abstract public int compare(Object o1, Object o2);

  public boolean equals(Object obj){
    return this==obj || 0==compare(this,obj);
  }

}
