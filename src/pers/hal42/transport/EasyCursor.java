package pers.hal42.transport;

import pers.hal42.lang.*;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.text.TextList;
import pers.hal42.text.TextListIterator;
import pers.hal42.util.StringStack;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

/**
 * hierarchy traverser, over property set that it is derived from
 */
public class EasyCursor extends EasyProperties implements AutoCloseable {
  protected String context = "";
  protected StringStack tree = new StringStack(ReflectX.shortClassName(this));
  public final static char SEP = '.';
  static final ErrorLogStream dbg = ErrorLogStream.getForClass(EasyCursor.class);
  final static String nullString = "(null)";

  public EasyCursor() {
    super();
  }

  public EasyCursor(Properties rhs) {
    super(rhs);
  }

  public EasyCursor(String context, String from) {
    super(from);
    setKey(context);
  }

  public EasyCursor(String from) {
    super(from);
    //    this.fromString(from, false /* not needed */);
  }

  public EasyCursor setKey(String newcontext) {
    if (newcontext != null) {//but it is OK to be trivial
      if (tree.isEmpty()) {
        context = newcontext;
      } else {//ez way to replace last segment of context:
        pop().push(newcontext);
      }
    }
    return this;
  }

  /**
   * @return this
   * push, even if @param is trivial, in which case we are on same branch, so that pop() will still work as expected.
   */
  public EasyCursor push(String more) {
    tree.push(context);
    if (StringX.NonTrivial(more)) {//double covered
      context += makeKey(more);
    }
    return this;
  }

  public EasyCursor push(int index) {
    return push(String.valueOf(index));
  }

  public EasyCursor pop() {//users should put this in finally clauses if exceptions can throw
    this.context = tree.isEmpty() ? "" : tree.pop();
    return this;
  }

  public EasyCursor Reset() {
    tree.Clear();
    context = "";
    return this;
  }

  public String preFix() {//not the same as what was set with setKey...
    return context != null ? context : "";
  }

  public String fullKey(String key) {
    boolean rooted = StringX.NonTrivial(key) && (key.charAt(0) == SEP);
    return (!rooted && StringX.NonTrivial(context)) ? (context + key) : key;
  }

  /**
   * overloading the get and set property is all we need to overload as they are
   * religiously used within all other gets and sets.
   */
  public Object setProperty(String key, String newValue) {
    // remove property, java.uti.Properties doesn't allow for null properties.
    return (newValue == null) ? remove(key) : super.setProperty(fullKey(key), newValue);
  }

  public String getProperty(String key) {
    return StringX.NonTrivial(key) ? super.getProperty(fullKey(key)) : "";
  }

  /**
   * @returns whether key is actually present.
   * this deals with possible exceptions in HashTable
   */
  public synchronized boolean containsKey(String key) {
    return StringX.NonTrivial(key) && super.containsKey(fullKey(key));
  }

  /**
   * sets either a string rendition of the object, or if the object is an isEasy then we create a branch
   * a slick way of getting recursion
   */
  public <T> EasyProperties setObject(String key, T obj, EasyHelper<T> helper) {
    if (obj != null) {
      if (obj instanceof isEasy) {
        try (EasyCursor popper = push(key)) {
          ((isEasy) obj).save(this);
          return this;
        }
      } else if (helper != null) {
        try (EasyCursor popper = push(key)) {
          helper.helpsave(this, obj);
          return this;
        }
      } else {//it better have a useful string representation:
        return super.setObject(key, obj);
      }
    } else {
      return this;
    }
  }

  public EasyProperties setObject(String key, Object obj) {
    return setObject(key, obj, null);
  }

  /**
   * tries to get an object. only works if the @param act isEasy or can be reprodiced from a property string.
   * at present the Class must have a public null constructor since I can't find out how java lets you see if a class supports an interface without having an object of that class
   */
  public <T> T getObject(String key, Class<? extends T> act, EasyHelper<T> helper) {
    try {
      T newone;
      if (ReflectX.isImplementorOf(act, isEasy.class)) {
        newone = act.newInstance();//+_+ just because a class is easy doesn't mean that its empty constructor is accessible
        try (EasyCursor popper = push(key)) {
          ((isEasy) newone).load(this);
          return newone;
        }
      } else if (helper != null) {
        newone = act.newInstance();
        try (EasyCursor popper = push(key)) {
          return helper.helpload(this, newone);
        }
      } else {
        return super.getObject(key, act);
      }
    } catch (Exception ex) {
      dbg.Caught(ex, "EasyCursor.getObject: {0}", act.getName());
      return null;
    }
  }

  /**
   * the remainder is an object, usually called while internal cursor is at root of storage
   */
  public Object getObject(Class act) {
    try {
      Object newone = act.newInstance();
      if (newone instanceof isEasy) {
        ((isEasy) newone).load(this);
      }
      return newone;
    } catch (Exception ex) {
      return null;
    }
  }

  /**
   * @return all keys below this branch, with branch prefix removed
   */
  public TextList branchKeys() {
    String prefix = preFix();     //FUE
    if (StringX.NonTrivial(prefix)) {
      TextList branch = new TextList();
      int clipper = prefix.length(); //FUE
      //propertyNames gives full length names
      for (Enumeration enump = super.propertyNames(); enump.hasMoreElements(); ) {
        String name = (String) enump.nextElement();
        if (name.startsWith(prefix)) {
          branch.add(StringX.subString(name, clipper));
        }
      }
      return branch;
    } else {
      return allKeys();
    }
  }

  /**
   * @return the current level's keys
   */
  public TextList topKeys() {
    TextList branch = branchKeys();
    //clip
    for (int i = branch.size(); i-- > 0; ) {
      String item = branch.itemAt(i);
      if (StringX.NonTrivial(item)) {
        int cutter = item.indexOf(SEP);
        if (cutter >= 0) {
          branch.set(i, item.substring(0, cutter));
        }
      }
    }
    return branch.uniqueify();
  }
//  public <T> T getObject(String key, Class<? extends T> act) {
//    return getObject(key, act, null);
//  }

  /**
   * @param branchcontext if null is current node, else we copy starting with that subnode.
   * @return an EasyProperties that is a subset of this one
   */
  public EasyCursor EasyExtract(String branchcontext) {//makes a real copy
    EasyCursor subset = new EasyCursor();
    //for each property, if it starts with context then cut that off and stick in subset
    if (branchcontext != null) {
      push(branchcontext);
    }
    try {
      TextList branch = branchKeys();
      for (int i = branch.size(); i-- > 0; ) {
        String subkey = branch.itemAt(i);
        subset.setString(subkey, getProperty(subkey));
      }
      return subset;
    } finally {
      if (branchcontext != null) {
        pop();
      }
    }
  }

  /**
   * takes a current subtree of @param subtree and makes such a tree on this.
   *
   * @return this.
   */
  public EasyCursor EasyInsert(String branchcontext, EasyCursor subtree) {
    if (branchcontext != null) {
      push(branchcontext);
    }
    try {
      TextList branch = subtree.branchKeys();
      for (int i = branch.size(); i-- > 0; ) {
        String subkey = branch.itemAt(i);
        setString(subkey, getProperty(subkey));
      }
      return this;
    } finally {
      if (branchcontext != null) {
        pop();
      }
    }
  }

  public String toSpam() {
    return preFix() + " has " + super.propertyList().size() + " items, defs has " + (defaults != null ? defaults.size() : 0);
  }

  /**
   * save a vector of objects.
   * the objects must be of class isEasy or stringConstructable (@see EasyPropeties.getObject)
   * or a later load will fail
   */

  public <T> void setVector(Vector<T> v, EasyHelper<T> helper) {
    int i = VectorX.size(v);
    setInt("size", i);//even if zero.
    while (i-- > 0) {
      try {
        setObject(Integer.toString(i), v.elementAt(i), helper);
      } catch (Exception e) {
        //continue; //try to finish iteration.
      }
    }
  }

  public <T> void setVector(Vector<T> v) {
    setVector(v, null);
  }

  /**
   * save @param v vector content under branch @param key, using @param helper to convert objects to text.
   */
  public <T> EasyCursor saveVector(String key, Vector<T> v, EasyHelper<T> helper) {
    try (EasyCursor popper = push(key)) {
      setVector(v, helper);
      return this;
    }
  }
  /////////////////////////////////////
  // isEasy utilities

  public <T> EasyCursor saveVector(String key, Vector<T> v) {
    return saveVector(key, v, null);
  }

  /**
   * @return a new vector created from data saved by saveVector()
   */
  public <T> Vector<T> getVector(Class<? extends T> act, EasyHelper<T> helper) {
    int i = getInt("size");
    if (i < 0) {//vector was not present in original save
      return new Vector<>(0); //create an empty one.
    }
    Vector<T> v = new Vector<>(i);
    v.setSize(i);
    while (i-- > 0) {
      try {
        v.set(i, getObject(Integer.toString(i), act, helper));
      } catch (Exception any) {
//        continue;
      }
    }
    return v;
  }

  public <T> Vector<T> getVector(Class<? extends T> act) {
    return getVector(act, null);
  }

  public <T> Vector<T> loadVector(String key, Class<? extends T> act, EasyHelper<T> helper) {
    try (EasyCursor popper = push(key)) {
      return getVector(act, helper);
    }
  }

  public <T> Vector<T> loadVector(String key, Class<? extends T> act) {
    return loadVector(key, act, null);
  }

  public TextList getTextList(String key) {
    return new TextList(loadVector(key, String.class));
  }

  /**
   * save a array with text labels instead of integer indexes.
   * the complementary load will only get the same array back if:
   * you use the same set of labels AND they are unique.
   * also @see saveVector for array content type restrictions
   */
  public EasyCursor saveArray(String key, String[] labeltmp, Object[] v) {
    int i = v.length;
    TextList label = labeltmp == null ? new TextList() : TextList.CreateFrom(labeltmp);
    try (EasyCursor popper = push(key)) {
      while (i-- > 0) {
        setObject(StringX.OnTrivial(label.itemAt(i), Integer.toString(i)), v[i]);
      }
      return this;
    }
  }

  /**
   * save an array, using integer labels.
   */
  public EasyCursor saveArray(String key, Object[] v) {
    return saveArray(key, null, v);
  }

  public void setTextList(String key, TextList tl) {
    this.saveVector(key, tl.Vector(), null);
  }

  /**
   * save a hashTable, using the keys as labels
   * the map's keys must be convertible to and from strings
   */
  public EasyCursor saveMap(String tablename, Map table) {
    if (!table.isEmpty()) {
      try (EasyCursor popper = push(tablename)) {
        for (Object key : table.keySet()) {
          try {
            setObject(String.valueOf(key), table.get(key));
          } catch (Exception ignored) {
            //silently omit defectively keyed or non-savable entries.
          }
        }
      }
    }
    return this;
  }

  /**
   * @param act is type of object which can be constructed from properties
   * @return map
   */
  public <T> Map<String, T> getMap(String mapname, Class<? extends T> act) {
    Map<String, T> table = new LinearMap<>();
    try (EasyCursor popper = push(mapname)) {
      //for each item on branch its key becomes the hashtable key, we make an object out of its value.
      TextList keys = topKeys();
      for (String key : keys.Vector()) {
        try {
          table.put(key, getObject(key, act));
        } catch (Exception ex) {
//          continue; //recover as much as possible.
        }
      }
      return table;
    }
  }

  public TextList toSpam(TextList tl) {
    if (tl == null) {
      tl = new TextList();
    }
    TextList keys = branchKeys();
    keys.sort();
    for (int i = 0; i < keys.size(); i++) {
      String name = keys.itemAt(i);
      tl.add(name, getString(name));
    }
    return tl;
  }
//  /**
//   * save an array, using a trueEnum to label the entries
//   */
//  public EasyCursor saveEnumeratedArray(String key, Object v[], TrueEnum indexer) {
//    return saveArray(key, indexer.getText(), v);
//  }
//  /**
//   * make a vector (yes, I know the name says array)
//   * of @param key name of vector
//   *
//   * @param act     Class of objects in array/vector
//   * @param indexer is a representative of the class that will be used to index vector.
//   */
//  public Vector loadEasyEnumeratedVector(String key, Class act, TrueEnum indexer) {
//    Vector v = null;
//    int size = indexer.numValues();
//        try (EasyCursor popper=push(key)){
//      v = new Vector(size);
//      v.setSize(size);
//      for (int i = size; i-- > 0; ) {
//        try {
//          v.set(i, getObject(indexer.TextFor(i), act));
//        } catch (Exception e) {
//          //+++ insert empty instance? if no then vector has a null ...
//          //...but anything we do here puts a requirement on the class contained, that otherwise may not be required
//          continue; //try to finish iteration.
//        }
//      }
//  return v;
//    }
//  }
//  public EasyCursor saveEasyEnumeratedArray(String key, Object v[], TrueEnum indexer) {
//    int i = v.length;
//    if (i > indexer.numValues()) {//invalid vector
//      i = indexer.numValues();
//      // or throw new ArrayIndexOutOfBoundsException();
//    }
//    push(key);
//    try {
//      while (i-- > 0) {
//        try {
//          setObject(indexer.TextFor(i), v[i]);
//        } catch (Exception e) {
//          continue; //try to finish iteration.
//        }
//      }
//    } finally {
//      return pop();
//    }
//  }

  /**
   * fill object from a subset named @param key via calling @param ezo's load()
   *
   * @return this
   */

  public EasyCursor getBlock(isEasy ezo, String key) {
    if (ezo != null) {
      try (EasyCursor popper = push(key)) {
        ezo.load(this);
      }
    }
    return this;
  }

  /**
   * build a subset named @param key filled via calling @param ezo's save()
   *
   * @return this
   */
  public EasyCursor setBlock(isEasy ezo, String key) {
    if (ezo != null) {
      try (EasyCursor popper = push(key)) {
        ezo.save(this);
      }
    }
    return this;
  }

  /**
   * this exists to let asParagraph dump only the items currently in view.
   */
  public Enumeration propertyNames() {
    return TextListIterator.New(branchKeys());
  }

  @Override
  public void close() {
    pop();
  }

  public static String makeKey(String morekey) {
    return StringX.NonTrivial(morekey) ? morekey + SEP : "";
  }

  public static EasyCursor New(InputStream is) {
    EasyCursor newone = new EasyCursor();
    newone.Load(is);
    return newone;
  }

  /**
   * make from url'd string made from a properties streamed output
   */
  public static EasyCursor fromUrl(String s) {
    EasyCursor newone = new EasyCursor();
    newone.fromURLdString(s, true);
    return newone;
  }

  public static EasyCursor makeFrom(isEasy object) {
    EasyCursor spammy = new EasyCursor();
    object.save(spammy);
    return spammy;
  }

  public static String spamFrom(isEasy object) {
    return makeFrom(object).asParagraph();
  }

  public static EasyCursor FromDisk(String filePath) {
    return FromDisk(new File(filePath));
  }

  public static EasyCursor FromDisk(File f) {
    EasyCursor fromdisk = new EasyCursor();
    try (FileInputStream is = new FileInputStream(f)) {
      fromdisk.Load(is);
    } catch (Exception ignored) {
      //will return an empty EasyCursor
    }
    return fromdisk;
  }

  public static String dump(isEasy ez) {
    return makeFrom(ez).asParagraph(OS.EOL);
  }
}


