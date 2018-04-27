package pers.hal42.ext;

import pers.hal42.math.MathX;

import java.util.Iterator;
import java.util.function.Function;

/**
 * Created by andyh on 4/3/17.
 * <p>
 * recordType max or min of a series  pushed at it
 * <p>
 * An example of one of the domains where C++ is vastly superior to java. Java engineers left out basic ordering functions from their Number classes.
 *
 */
public class Extremer<Scalar extends Number> {
  public Scalar extremum;
  protected boolean negatory = false;
  int location = ~0;//default for debug
  protected boolean preferLatter = false;
  protected boolean started = false;

  public Extremer(boolean negatory, boolean preferLatter, Scalar guard) {
    this.negatory = negatory;
    this.preferLatter = preferLatter;
    extremum=guard;
    //but DONT start, so can have an absurd init value for those who are careless abour checking 'started' before using 'extremum'
  }

  public Extremer(boolean negatory, Scalar guard) {
    this(negatory, false, guard);
  }

  /** set a value for those who forget to check whether any was inspected */
  public Extremer(Scalar guard) {
    this(false, guard);
  }

  public Extremer(boolean negatory, boolean preferLatter) {
    this.negatory = negatory;
    this.preferLatter = preferLatter;
  }

  public Extremer(boolean negatory) {
    this(negatory, false);
  }

  public Extremer(){
    this(false, false);
  }

  /**
   * @return the extreme value if any values were presented, else @param ifNone
   */
  public Scalar getExtremum(Scalar ifNone) {
    return started ? extremum : ifNone;
  }

  /**
   * @return the extreme value if any values were presented, else whatever construction debris might exist.
   */
  public Scalar getExtremum() {
    return extremum;
  }

  /** @returns whether an extreme value was found */
  public boolean exists() {
    return started;
  }

  /**
   * @return whether the @param value is the new extremum.
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
    location = ~0;
  }

  @Override
  public String toString() {
    return started ? String.valueOf(extremum) : "None";
  }

  public <T> void inspectArray(T[] array, Function<T, Scalar> extractor) {
    for (T item : array) {
      Scalar itssize = extractor.apply(item);
      inspect(itssize);
    }
  }

  public <T> void inspectAll(Iterator<T> it, Function<T, Scalar> extractor) {
    it.forEachRemaining(item -> inspect(extractor.apply(item)));
  }

} // class Extremer
