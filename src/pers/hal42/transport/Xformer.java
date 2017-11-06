package pers.hal42.transport;

import pers.hal42.lang.ReflectX;

import java.lang.reflect.Method;

public class Xformer {
  public final Method packer;
  public final Method parser;

  public Xformer(Class claz) {
    packer = getPacker(claz);
    parser = getParser(claz);
  }

  public Object pack(Object next) {
    try {
      return packer.invoke(next);
    } catch (Exception e) {
      return null;
    }
  }

  /** @returns whether @param fclaz suppoers packing */
  public static boolean IsPacker(Class fclaz) {
    return fclaz.getAnnotation(Xform.class) != null;
  }

  public static Method getParser(Class claz) {
    return ReflectX.methodFor(claz, Xform.class, Xform::parser);
  }

  public static Method getPacker(Class claz) {
    return ReflectX.methodFor(claz, Xform.class, Xform::packer);
  }

  /** @returns packed representation of obj, from its class as explicitly given */
  public static String Packed(Object obj, Class claz) {
    if (claz == null) {
      claz = obj.getClass();
    }
    return (String) ReflectX.doMethodWithReturn(obj, Xformer.getPacker(claz));
  }

  /** */
  public static void UnpackInto(Object obj, Class claz, String packed) {
    if (claz == null) {
      claz = obj.getClass();
    }
    ReflectX.doMethod(obj, ReflectX.methodFor(claz, Xform.class, Xform::parser), packed);
  }


  /** @returns best guess as to institution type given a character */
  @Xform(parser = true)
  public <T> T fromChar(char packedcase, T[] values) {
    Character asObject = packedcase;
    for (T it : values) {
      if (asObject.equals(pack(it))) {
        return it;
      }
    }
    return null;
  }
}
