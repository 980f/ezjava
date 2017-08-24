package pers.hal42.transport;

import pers.hal42.lang.ReflectX;
import pers.hal42.logging.ErrorLogStream;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Predicate;

/**
 * uses reflection to decide which fiels of a record to include in writing to a comma separated file.
 * NB: the default comma is actually a tab character.
 * as of writing this header comment we only write such files, no parsing.
 *
 * @see pers.hal42.stream.CSVOutStream for a simpler form of CSV output.
 */
public class CsvTransport {
  public final Class recordType;
  private final ErrorLogStream dbg = ErrorLogStream.getForClass(CsvTransport.class);
  private final Field[] fieldCache;
  private String format;
  private final boolean[] filter;


  /** constructor caches items needed by LineWriter to print efficiently */
  public CsvTransport(Class recordType, String comma, Predicate<Class> transportable) {
    this.recordType = recordType;

    fieldCache = this.recordType.getFields();
    //worst case allocation
    filter = new boolean[fieldCache.length];

    for (int i = fieldCache.length; i-- > 0; ) {
      final Field field = fieldCache[i];
      if (ignorable(field)) {
        filter[i] = false;
        continue;
      }
      if (ReflectX.isNative(field)) {
        filter[i] = true;
        continue;
      }
      //else caller criterion
      filter[i] = transportable.test(field.getType());
    }
  }

  public CsvTransport(Class recordType, String comma) {
    this(recordType, comma, CsvTransport::Common);
  }

  /**
   * default predicate for transportable class. accessibility tested elsewise.
   *
   * @returns whether class has a non-trivial toString().
   */
  @SuppressWarnings("unchecked")
  public static boolean Common(Class fclaz) {
    try {
      if (ReflectX.isImplementorOf(fclaz, Number.class)) {
        return true;
      }
      final Method the_toString = fclaz.getMethod("toString");
      Class declaredin = the_toString.getDeclaringClass();
      return declaredin != Object.class;
    } catch (Exception e) {
      return false;
    }
  }

  /** @returns whether field is public, non transient, and not annotated Ignore */
  public static boolean ignorable(Field field) {
    if (field.isAnnotationPresent(ReflectX.Ignore.class)) {
      return true;
    }
    int modifiers = field.getModifiers();
    return Modifier.isTransient(modifiers) || !Modifier.isPublic(modifiers);
  }

  public class LineWriter {
    public int linecount = 0;
    final BufferedWriter writer;
    /** output header info every this many lines, 0 for once at start, ~0 for never */
    final int headerStride;
    private final String comma;

    public LineWriter(BufferedWriter writer, String comma, int headerStride) {
      this.writer = writer;
      this.comma = comma;
      this.headerStride = headerStride;
      if (headerStride == 0) {
        headerLine();
      }
    }

    public LineWriter(BufferedWriter writer, int headerStride) {
      this(writer, "\t", headerStride);
    }

    public LineWriter(BufferedWriter writer, String comma) {
      this(writer, comma, ~0);
    }

    public LineWriter(BufferedWriter writer) {
      this(writer, "\t", ~0);
    }

    public void headerLine() {
      try {
        for (int i = 0; i < fieldCache.length; ++i) {
          if (filter[i]) {
            String column = fieldCache[i].getName();
            if (i != 0) {
              writer.write(comma);
            }
            writer.write(column);
          }
        }
      } catch (IOException e) {
        dbg.Caught(e);
      }
    }

    public boolean write(Object record) {
      if (record == null) {
        return false;
      }
      if (!ReflectX.isImplementorOf(record.getClass(), recordType)) {
        return false;
      }
      try {
        if (headerStride > 0) {
          if (linecount % headerStride == 0) {
            headerLine();
          }
        }
        for (int i = 0; i < fieldCache.length; ++i) {
          if (filter[i]) {
            try {
              Object obj = fieldCache[i].get(record);
              if (obj != null) {
                if (i != 0) {
                  writer.write(comma);
                }
                writer.write(String.valueOf(obj));
              }
            } catch (IllegalAccessException e) {
              filter[i] = false;//note and continue, should fix filter function used in constructor
            }
          }
        }
        writer.newLine();
        return true;
      } catch (IOException e) {
        dbg.Caught(e);
        return false;
      }
    }
  }
}
