package pers.hal42.database;

/** Base class for stepping through a resultSet & filling a table object. */

import pers.hal42.lang.ReflectX;
import pers.hal42.lang.StringX;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.math.Accumulator;
import pers.hal42.text.TextList;
import pers.hal42.timer.StopWatch;
import pers.hal42.transport.EasyProperties;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Hashtable;

public abstract class Query {

  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(Query.class);

  private ResultSet rs = null;
  private Statement stmt = null;

  // single stuff
  private boolean loadedone = false;
  private boolean showedone = false;

  public Query(Statement stmt) {
    this.stmt = stmt;
    if(stmt == null) {
      dbg.WARNING("Query(): stmt == NULL. (Not necessarily an error.)");
    }
    rs = DBMacros.getResultSet(stmt);
  }

  public boolean rewind() {
    boolean ret = false;
    try {
      if(rs != null) {
        if(rs.first()) {
          rs.previous(); // previous will return false if it is before the first row, but ignore
          ret = rs.isBeforeFirst();
        }
      }
    } catch (Exception ex) {
      dbg.Caught(ex);
    } finally {
      return ret;
    }
  }

  protected ResultSet rs() {
    return rs;
  }

  public boolean next() {
    return next(null, null);
  }
  // a timed next()
  public boolean next(StopWatch nexttimer, StopWatch loadTimer) {
    boolean ret = false;
    if(DBMacros.next(rs, nexttimer)) {
      if(loadTimer != null) {
        loadTimer.Start();
      }
      fromResultSet(rs);
      if(loadTimer != null) {
        loadTimer.Stop();
      }
      ret = true;
    } else if((rs == null) && loadedone && !showedone) {
      showedone = true;
      ret = true;
    }
    return ret;
  }

  public boolean hasMoreRows() {
    boolean ret = false;
    try {
      if(rs!=null) {
        if(rs.getType() != ResultSet.TYPE_FORWARD_ONLY) {
          ret = /*rs.isLast() ||*/ rs.isAfterLast();
        } else {
          ret = false; // need a MAYBE! (this shouldn't work, but does)
        }
      } else {
        if(loadedone) {
          ret = false;//!showedone;
        } else {
          dbg.ERROR("hasMoreRows() rs is NULL!");
        }
      }
    } catch (Exception t) {
      dbg.ERROR("hasMoreRows() Exception checking for more records!");
      dbg.Caught(t);
    }
    return !ret;
  }

  public static final Accumulator fromResultSetGet = new Accumulator();
  public static final Accumulator fromResultSetSet = new Accumulator();
  public static final Accumulator fromResultSetFields = new Accumulator();
  public static final Accumulator fromResultSetName = new Accumulator();

  protected void fromResultSet(ResultSet rs) {
    if(rs != null) {
      Object record = this;
      // create the object from a record in the resultset
      StopWatch sw = new StopWatch();
      Field[] field = getFieldsFor(record.getClass());//record.getClass().getFields();
      fromResultSetFields.add(sw.Stop());
      for(int i = field.length; i-->0;) {
        Field f = field[i];
//        if(f.getType().equals(String.class)) { // we only fill strings!
        sw.Start();
          String fName = f.getName();
          fromResultSetName.add(sw.Stop());
          try {
            sw.Start();
            String value = DBMacros.getStringFromRS(fName, rs);
            fromResultSetGet.add(sw.Stop());
            sw.Start();
            f.set(record, value);
            fromResultSetSet.add(sw.Stop());
            dbg.VERBOSE("fromResultSet: attempted to set " + ReflectX.shortClassName(record) + "." + fName + "=" + value);
          } catch(Exception t) {
            dbg.ERROR("fromResultSet: Error mapping value for field " + fName + ".");
          }
//        }
      }
      loadedone = true;
    }
  }

  public EasyProperties toProperties() {
    // create a list of all of the PUBLIC STRING fields and their values from the object
    Object record = this;
    Field[] field = getFieldsFor(record.getClass());//record.getClass().getFields(); // only gets publics
    EasyProperties ezp = new EasyProperties();
    for(int i = field.length; i-->0;) {
      Field f = field[i];
//      if(f.getType().equals(String.class)) {
        String fName = f.getName();
        String fValue = "";
        try {
          fValue = (String) f.get(record);
        } catch(Exception t) {
          dbg.Caught("toProperties: Error getting value for field " + fName + ".", t);
        }
        try {
          ezp.setString(fName.toLowerCase(), StringX.TrivialDefault(fValue, ""));
        } catch(Exception t) {
          dbg.Caught("toProperties: Error setting ezp value for field " + fName + ".", t);
        }
//      }
    }
    return ezp;
  }

  // used for debugging purposes only (like for certification testing)
  // now also used for copying TxnRow (20020912)
  public void fromProperties(EasyProperties ezp) {
    if(ezp == null) {
      // donutn
    } else {
      // create a list of all of the fields and their values from the object
      Field[] field = getFieldsFor(getClass());//getClass().getFields();
      for (int i = field.length; i-- > 0; ) {
        Field f = field[i];
//        if (f.getType() == String.class) {
          String fName = f.getName();
          String fValue = "";
          try {
            fValue = ezp.getString(fName);
            f.set(this, fValue);
          } catch (Exception t) {
            dbg.Caught("fromProperties: Error setting value for field: [" +
                       fName + "=" + fValue + "]", t);
          }
//        }
      }
      loadedone = true;
    }
  }

  /**
   * null is acceptable here
   */
  public void setAllTo(String value) {
    Object record = this;
    Field[] field = getFieldsFor(record.getClass());// record.getClass().getFields();
    for(int i = field.length; i-->0;) {
      Field f = field[i];
//      if(f.getType() == String.class) {
        String fName = f.getName();
        String fValue = "";
        try {
          f.set(record, value);
        } catch(Exception t) {
          dbg.ERROR("setAllTo: Error setting value for field " + fName + ".");
        }
//      }
    }
  }

  private static final Hashtable<Class,Field[]> allclassesfields = new Hashtable<>();
  private synchronized static Field [ ] getFieldsFor(Class klass) {
    Field [ ] ret = null;
    try {
      // look it up to see if we already have it
      Object o = allclassesfields.get(klass);
      if(o == null) {
        // if we don't, create it, then return it
        Field[] allfields = klass.getFields();
        Field[] goodfields = new Field[allfields.length];
        int goods = 0;
        TextList forReporting = new TextList();
        for(int i = allfields.length; i-- > 0; ) {
          Field f = allfields[i];
//          if(f.getType() == String.class) {
          if(f.getType().equals(String.class)) {
            goodfields[goods++] = f;
            forReporting.add(f.getName());
          }
        }
        // copy only the good fields into a sparse array
        Field[] toret = new Field[goods];
        System.arraycopy(goodfields, 0, toret, 0, goods);
        // set it in the hashishtable
        allclassesfields.put(klass, toret);
        ret = toret; // don't set the reference until we got passed all exceptions
        dbg.ERROR("Generated Field Cache for class " + klass.getName() + ": " +
                  forReporting.asParagraph(","));
      } else {
        // if we do, return it
        ret = (Field [ ])o;
        dbg.VERBOSE("found field cache for class " + klass.getName());
      }
    } catch (Exception e) {
      dbg.Caught(e);
    } finally {
      return ret;
    }
  }

  public void close() {
    DBMacros.closeStmt(stmt);
  }

  public void finalize() {
    close();
  }

  // uses reflection to output the contents for humans
  public String toString() {
    return toProperties().asParagraph();
  }

}
