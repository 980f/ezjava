package pers.hal42.ext;

import pers.hal42.math.MathX;

/**
 * Created by andyh on 4/3/17.
 * <p>
 * record max or min of a series  pushed at it
 * <p>
 * An example of one of the domains where C++ is vastly superior to java. Java engineers left out basic ordering functions from their Number classes.
 */
public class Extremer<Scalar extends Number> {
  public Scalar extremum;
  boolean negatory = false;
  int location = ~0;//default for debug
  boolean preferLatter = false;
  boolean started = false;

  public Extremer(Scalar guard){
    extremum=guard;
    //but DONT start, so can have an absurd init value for those who are careless abour checking 'started' before using 'extremum'
  }

  public Extremer(){

  }
  /**
   * @return the extreme value if any values were presented, else @param ifNone
   */
  public Scalar getExtremum(Scalar ifNone) {
    return started ? extremum : ifNone;
  }

  /**
   * @return whether the extremum was updated
   */
  public boolean inspect(Scalar value) {
    if (started) {
      int cmp = MathX.cmp(extremum, value);
      if (negatory) {
        if (preferLatter) {
          if (cmp < 0) {
            return false;
          }
        } else {
          if (cmp <= 0) {
            return false;
          }
        }
      } else {
        if (preferLatter) {
          if (cmp > 0) {
            return false;
          }
        } else {
          if (cmp >= 0) {
            return false;
          }
        }
      }
    } else {
      started = true;
    }
    extremum = value;
    return true;
  } // inspect

  public boolean inspect(int loc, Scalar value) {
    if (inspect(value)) {
      location = loc;
      return true;
    } else {
      return false;
    }
  } // inspect

  /**
   * to reuse
   */
  public void reset() {
    started = false;
//    extremum = 0;//for debug
    location = ~0;
  }

  @Override
  public String toString() {
    return started ? String.valueOf(extremum) : "None";
  }
} // class Extremer
