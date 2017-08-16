package pers.hal42.database;

import java.lang.annotation.*;

/**
 * Created by Andy on 7/11/2017.
 * <p>
 * tag field to be associated with a database.
 */

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DbColumn {
  //column type will be picked by reflection
  int size() default 0;

  boolean nullable() default true;
}
