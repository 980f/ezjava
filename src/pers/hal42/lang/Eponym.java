package pers.hal42.lang;

import java.lang.reflect.Field;

/**
 * this class wasn't worth the effort of writing it.
 * Someday java will provide a ".field" 10:50function and life will be so much better.
 */
public class Eponym {
  public String name;

  /**
   * if not already set then set name from fieldname of object containing this within @param parent
   *
   * @returns whether name is nonTrivial, not whether it was altered herein.
   */
  public boolean eponomize(Object grandParent) {
    if (StringX.NonTrivial(name)) {
      return true;
    }

    Field[] myNamer = ReflectX.findParentage(grandParent, this);
    if (myNamer.length > 1) {
      name = myNamer[1].getName();
      return true;
    }
    return false;
  }
}
