package pers.hal42.transport;

import java.lang.annotation.*;

/** marks a static method that either takes a char and returns an Enum or vice versa */
@Documented
@Retention(RetentionPolicy.RUNTIME)
//on a method it identifies which method, on a type that the type has such methods
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Xform {
  //public static Enum whatever(char ch)
  boolean parser() default false;

  //public static char whatever(Enum item)
  boolean packer() default false;
}
