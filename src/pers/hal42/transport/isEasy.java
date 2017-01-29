package pers.hal42.util;

/** if a class implements "isEasy" then it has load and save from properties functions, i.e. portable public access via text */

public interface isEasy {//for eventual reflective invocation of load and save
  public void save(EasyCursor ezc);
  public void load(EasyCursor ezc);
}
/*when we get class<->properties via reflection then this interface will be emptied,
remaining as permission to do the translations.
another option at that time is to have 'beforeSave()' and 'afterLoad()' members for preparing
and interpreting the fields ...but this ain't C++ so we won't bother ourselves with that.

any isEasy class that needs to combine info from its fields must either alwasy do that
dynamically, or have another class that uses an easy class to do such work.

TODO:
+++ Since defining a set of attributes indicating fields that can be saved and loaded by reflection is difficult,
have a field which is a private static array of strings each of which is a field name.
private final static String EasyPropertyList[]={//this name is reserved for this functionality in any class which implements the "IsEasy" interface:
  "firstFieldName",
  "anotherFieldName",
};


*/


///////
//////  WIP here on down --  THINK I'M GIVING UP ON THIS FOR NOW
//////  Was a dynamic EasyCursor load(er) and save(er)
///////

/*
  private static final String classDesignator = "class";
  public static final EasyCursor PropertiesFromObject(Object o, String oName) {
    Class c = o.getClass();
    EasyCursor ezp = new EasyCursor();
    ezp.setString(classDesignator, c.getName());
    // handle the entire class hierarchy and the public data members
    propsTxObj(o, oName, c, ezp, true);
    return ezp;
  }

  public Object PropertiesToObject(String oName) {
    Object o = null;
    try {
      dbg.Enter("PropertiesToObject");
      o = (Object)(Class.forName(getString(classDesignator)).newInstance());
    } catch (Exception e) {
      dbg.Caught(e);
    } finally {
      dbg.Exit();
    }
    if(o!=null) {
      Class c = o.getClass();
      // handle the entire class hierarchy and the public data members
      propsTxObj(o, oName, c, this, false);
    }
    return o;
  }

  private static final void propsTxObj(Object o, String oName, Class c, EasyCursor ezp, boolean setProps) {
    Class s = c.getSuperclass();
    if((s != null) && (s != Object.class)) {//alh: just check if super "isEasy"
//actually it is offensive to go up the hierarchy. it would make more sense
to check all members and process members that are "isEasy".
      // Go up the class hierarchy using the same name (class.superclass())
      dbg.Message(c.getName() + "==>" + s.getName());
      propsTxObj(o, oName, s, ezp, setProps);
    }
    doFields(o, oName, c, ezp, setProps);
  }

  private static  final void doFields(Object o, String oName, Class c, EasyCursor ezp, boolean setProps) {
    // +++ handle arrays
    // find all of the members (public data)
    Field[] field = c.getFields();
    for(int i = field.length; i-->0;) {
      Field f = field[i];
      String fName = oName + "." + f.getName();
      // find each member's date type
      Class type = f.getType();
      dbg.Message("class type detected: " + type.getName());
      // <sigh>  I wish switches could use classes or objects </sigh>
      try {
        if(type.equals(Byte.class)) {
          //
        } else if(type.equals(Character.class)) {
          //
        } else if(type.equals(Double.class)) {
          //
        } else if(type.equals(Float.class)) {
          //
        } else if(type.equals(Short.class)) {
          //
        } else if(type.equals(boolean.class)) {
          if(setProps) {
            ezp.setBoolean(fName, f.getBoolean(o));
          } else {
            f.setBoolean(o, ezp.getBoolean(fName));
          }
        } else if(type.equals(int.class)) {
          if(setProps) {
            ezp.setInt(fName, f.getInt(o));
          } else {
            f.setInt(o, ezp.getInt(fName));
          }
        } else if(type.equals(long.class)) {
          if(setProps) {
            ezp.setLong(fName, f.getLong(o));
          } else {
            f.setLong(o, ezp.getLong(fName));
          }
        } else if(type.equals(String.class)) {
          if(setProps) {
            ezp.setString(fName, (String)f.get(o));
          } else {
            f.set(o, ezp.getString(fName));
          }
        } else if(type.equals(TrueEnum.class)) {
          TrueEnum te = (TrueEnum)f.get(o);
          dbg.Message("IN TRUEENUM HANDLER !!");  //  for testing
          if(setProps) {
            ezp.saveEnum(fName, te);
          } else {
            ezp.loadEnum(fName, te);
          }
        } else if(type.equals(TextList.class)) {
          // +++ generalize this later!
          // +++ and make a function in TextList for it?
          TextList tl = (TextList)f.get(o);
          if(setProps) {
            int size = tl.size();
            ezp.setInt(fName, size);
            for(int j = tl.size(); j-->0; ) {
              String name = fName + j;
              ezp.setString(name, tl.itemAt(j));
            }
          } else {
            int size = ezp.getInt(fName);
            tl.setSize(size);
            for(int j = tl.size(); j-->0; ) {
              String name = fName + j;
              tl.setElementAt(ezp.getString(name), j);
            }
          }
        } else { // it must be a more complex object, right?
          // for objects, call this function recursively
          propsTxObj(f.get(o), fName, type, ezp, setProps);
        }
      } catch (IllegalAccessException e) {
        dbg.Caught(e);
        dbg.Message("(while trying to " + (setProps ? "set" : "get") + " field " + fName + ")");
      }
    }
  }
*/


