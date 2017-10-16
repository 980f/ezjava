package pers.hal42.database;

import pers.hal42.lang.ReflectX;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.text.Packable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ListIterator;
import java.util.function.Predicate;

/**
 * Created by Andy on 7/12/2017.
 * <p>
 * create objects from table by annotation assisted reflection.
 * todo:0 use an annotation on the class to supply 'local' option.
 */
public class TableFiller {
  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(TableFiller.class);

  /**
   * parse result set row into a new object and append to @param list. @param ref is the class of the type and reflection looks for @DbColumn annotations to decide what to match. At the moment ref must have a no-args constructor.
   * @param refinclude if not null determines whether the newly constructed-from-rs object should be added to the list. This is useful when such a decision is beyond implementing via a 'where' clause on the query that sourced the resultSet.
   *
   * @returns the number of added fields. If the value is negative then it is the complement of how many were added before an exception halted the process.
   */
  public static <T> int addToList(ResultSet rs, Class<? extends T> ref, boolean local, ListIterator<T> list, Predicate<T> refinclude) {
    int successes = 0;
    DbColumn all = ReflectX.getAnnotation(ref, DbColumn.class);
    try {
      do {//for each row in result set, which has already been 'nexted' once.
        T obj = ref.newInstance();
        ReflectX.FieldWalker fields = new ReflectX.FieldWalker(ref, local);//todo:1 this line is expensive enough that we should make a rewindable list.
        fields.forEachRemaining(field -> {
            DbColumn colAttr = field.getAnnotation(DbColumn.class);
            if (colAttr != null || (all != null && !ReflectX.ignorable(field))) {
              try {
                field.setAccessible(true);//be aggressive, but only useful if we are doing local walking.
                String name = field.getName();
                Class fclaz = (field.getType());
                //can't switch on class types <sad/>
                if (fclaz == String.class) {
                  field.set(obj, rs.getString(name));
                } else if (fclaz == boolean.class) {
                  field.setBoolean(obj, rs.getBoolean(name));
                } else if (fclaz == double.class) {
                  field.setDouble(obj, rs.getDouble(name));
                } else if (fclaz == int.class) {
                  field.setInt(obj, rs.getInt(name));
                } else if (fclaz.isEnum() && colAttr != null) {
                  int enumMod = colAttr.size();
                  if (enumMod == 0) {//if field is integer then index into enum constants
                    int ordinal = rs.getInt(name);
                    field.set(obj, ReflectX.enumByOrdinal(fclaz, ordinal));
                  } else { //else if text then somehow the fclaz must give us a value.
                    //want a static method that takes a string and gives an fclaz.
                    field.set(obj, ReflectX.enumObject(fclaz, rs.getString(name)));
                  }
                } else if (ReflectX.isImplementorOf(fclaz, Packable.class)) {
                  Packable packer = (Packable) field.get(obj);
                  packer.parse(rs.getString(name));
                } else {
                  dbg.ERROR("Not yet parsing type {0} from result set (field named {1})", fclaz.getName(), name);
                }
              } catch (IllegalAccessException | SQLException e) {
                dbg.Caught(e);
              }
            }
          }
        );
        if (refinclude == null || refinclude.test(obj)) {
          list.add(obj);
          ++successes;
        }
      } while (rs.next());
      return successes;
    } catch (SQLException e) {
      dbg.Caught(e, "iterating to create objects by reflection");
      return ~successes;
    } catch (IllegalAccessException | InstantiationException e) {
      dbg.Caught(e);
      return ~successes;
    }
  }

  public static <T> void loadTable(String query, Class<? extends T> infoClass, boolean local, ListIterator<T> all, DBMacros jdbc) {
    loadTable(query, infoClass, local, null, all, jdbc);
  }

  public static <T> void loadTable(String query, Class<? extends T> infoClass, boolean local, Predicate<T> refinclude, ListIterator<T> all, DBMacros jdbc) {
    jdbc.doSimpleQuery(query, rs -> addToList(rs, infoClass, local, all, refinclude));
  }
}
