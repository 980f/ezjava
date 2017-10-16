package pers.hal42.database;

import java.lang.annotation.*;

/**
 * Created by Andy on 7/11/2017.
 * <p>
 * tag field to be associated with a database.
 * If a class is tagged then all of its non-transient public fields are treated as if they had @DbColumn with no args.
 */

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface DbColumn {
  //column type will be picked by reflection

  /** size is sometimes relevant */
  int size() default 0;

  /** whether column is allowed to be null */
  boolean nullable() default true;

  /** NB: 'null' was not deemed a constant by java 1.8 */
  String def() default "";
}
