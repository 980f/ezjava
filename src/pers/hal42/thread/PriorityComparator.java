package pers.hal42.thread;

public class PriorityComparator<Qtype extends Comparable<? super Qtype>>  {

  /**
   * if object not comparable uses hashcode() (shouldn't be possible with java>~6).
   * a non null object is greater than a null object.
   */

  public int compareTo(Qtype older, Qtype newer) {
    try {//don't generify, we handle all exceptions sanely.
      return older.compareTo(newer);
    } catch (ClassCastException cce) {//if objects aren't 'comparable' use hashcodes.
      return older.hashCode() - newer.hashCode();
    } catch (NullPointerException npe) {//null is the lowest priority of all
      return (older != null) ? 1 : newer != null ? - 1 : 0;
    }
  }

}


