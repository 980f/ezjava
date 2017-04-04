package pers.hal42.ext;

/**
 * Created by andyh on 4/3/17.
 */
public class CountDown {
  int counter;


  public CountDown(int count){
    counter=count;
  }

  public int setto(int quantity){
    return counter=quantity;
  }

  public boolean done()  {
    return counter==0;
  }

  /** sometimes you go back one */
  public int increment() {
    return ++counter;
  }

  /** decrements counter unless it is already zero, @returns whether it is now zero */
  public boolean decrement() {
    if(counter>0){
      --counter;
      return true;
    } else {
      return false;
    }
  }

  /** test, decrements if not already zero and @returns whether it just became zero */
  public boolean last() {
    return counter > 0 && decrement();
  }


  public boolean hasNext() {
    return counter>0;
  }
  /**
   * @return a reference to the element of the @param array associated with this count value.
   * Note then when counting is done this will get you the 0th item repeatedly, not an exception. */
  public <T> T next(T []array){
    return array[counter];
  }
}
