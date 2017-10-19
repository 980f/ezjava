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
}
