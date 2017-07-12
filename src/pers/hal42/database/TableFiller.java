package pers.hal42.database;

import pers.hal42.lang.ReflectX;
import pers.hal42.logging.ErrorLogStream;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ListIterator;

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
   *
   * @returns the number of added fields. If the value is negative then it is the complement of how many were added before an exception halted the process.
   */
  public static int addToList(ResultSet rs, Class ref, boolean local, ListIterator list) {
    int successes = 0;
    DbColumn all = ReflectX.getAnnotation(ref, DbColumn.class);
    try {
      while (rs.next()) {//for each row in result set
        Object obj = ref.newInstance();
        ReflectX.FieldWalker fields = new ReflectX.FieldWalker(ref, local);//this line is expensive enough that we should make a rewindable list.
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
                } else if (fclaz.isEnum()) {
                  int enumMod = colAttr.size();
                  if (enumMod == 0) {//if field is integer then index into enum constants
                    int ordinal = rs.getInt(name);
                    field.set(obj, ReflectX.enumByOrdinal(fclaz, ordinal));
                  } else { //else if text then somehow the fclaz must give us a value.
                    //want a static method that takes a string and gives an fclaz.
                    field.set(obj, ReflectX.enumObject(fclaz, rs.getString(name)));
                  }
                } else {
                  dbg.ERROR("Not yet parsing type {0} from result set (field named {1})", fclaz.getName(), name);
                }
              } catch (IllegalAccessException | SQLException e) {
                dbg.Caught(e);
              }
            }
          }
        );
        list.add(obj);
        ++successes;
      }
      return successes;
    } catch (SQLException e) {
      dbg.Caught(e, "iterating to create objects by reflection");
      return ~successes;
    } catch (IllegalAccessException | InstantiationException e) {
      dbg.Caught(e);
      return ~successes;
    }
  }

  public static void loadTable(String query, Class infoClass, boolean local, ListIterator all, DBMacros jdbc) {
    jdbc.doSimpleQuery(query, rs -> addToList(rs, infoClass, local, all));
  }
}
