package pers.hal42.lang;

/** cppext/index.h -> java
 * 
 *  sanity checked int, for use as a 0-based array index. 
 *  The C implementation used an unsigned which saved a lot of compares<0
 *  
 * Created by andyh on 4/3/17.
 */
public class Index {
  

/* unsigned is used for all index operations.
  For prior uses of int typically the only negative index value is a marker, -1.
  It happens that if you view -1 as an unsigned it is the maximum possible value. That has the advantage of replacing the signed integer dance:
  index>=0 && index < quantity with a simple index<quantity, less runtime code.

  The only risk here is that someone might use -1 as a quantity value indicating that there is not even a container to have a quantity of items in. Just don't do that, return a quantity of 0 for 'not a valid question', that will almost always yield the expected behavior.
 */
  /** the magic value, it is all ones */
  public static final int BadIndex=~0;
//  //marker for bad difference of indexes:
//  constexpr unsigned BadLength=~0U; //legacy

  /** we default indexes to ~0, and return that as 'invalid index'*/
  boolean isValid(int index){
    return index!=BadIndex;
  }

  /** marker class for an integer used as an array index or related value. Leave all in header file as we need to strongly urge the compiler to inline all this code  */
  
    public Index( int raw){
      this.raw=raw;
    }

  public int raw;

  public boolean isOdd()   {
      return isValid()&& ((raw&1)!=0);//lsb === is odd.
    }

  public int setto( int ord)   {
      return raw=ord;
    }

  public   int get()   {
      return raw;
    }

  public   boolean isValid()   {
      return isValid(raw);
    }

  public  void clear()   {
    raw=BadIndex;
  }

  /** @return whether this is less than @param limit */
  public  boolean in( Index limit)   {
    return isValid() && limit.isValid() && raw<limit.raw;
  }

  /** @return whether this contains @param other */
  public  boolean has(Index other)   {
    return isValid() && other.isValid() && raw>other.raw;
  }

  /** maydo: convert negatives to canonical ~0*/
  public   int inc( int other)   {
    return raw+=other;
  }

  /** decrement IF valid */
  public   int dec( int other)   {
    if(isValid()){
      return raw-=other;
    } else {
      return BadIndex;
    }
  }

  /** maydo: convert negatives to canonical ~0*/
  public   int postinc()   {
    return raw++;
  }
  /** maydo: convert negatives to canonical ~0*/
  public   int inc()   {
    return ++raw;
  }


}
