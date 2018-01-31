package pers.hal42.transport;

import pers.hal42.lang.Finally;
import pers.hal42.lang.LinearMap;
import pers.hal42.lang.StringX;
import pers.hal42.logging.ErrorLogStream;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Properties;

import static pers.hal42.lang.StringX.TrivialDefault;

/**
 * a storable node which is associated with a file.
 * Extend from this as the concrete class name is used to find the file.
 * It is intended for you to have your stored members in your extension of this class.
 */
@Storable.Stored  //someday inheritance might work with this
public class JsonOptions {
  /**
   * root of the options DOM, typically named for the file it is loaded from
   */
  transient  //don't want unexpected recursions
  public Storable node;
  /**
   * how forcefully to map the DOM to the object
   */
  transient  //don't save, and especially don't load, the rules.
  public Storable.Rules rules;

  protected static final ErrorLogStream dbg = ErrorLogStream.getForClass(JsonOptions.class);//use base here, not the extension

  protected JsonOptions() {
    this("");
  }

  /**
   * create and initialize from random file @param altfilename
   */
  public JsonOptions(String altfilename) {//public for test access, normal use is to derive from this class.
    this(StringX.NonTrivial(altfilename) ? Storable.Groot(altfilename) : null);
  }

  public JsonOptions(Storable existing) {
    if (existing != null) {
      node = existing;
    } else {
      node = JsonStorable.StandardRoot(getClass());
    }
    rules = Storable.Rules.Master;
    //#_# while it might seem like a nice idea to invoke load() here, the derived object's own fields don't yet exist and as such cannot therefore set defaults like they are supposed to. so no "load(altfilename)";
  }

  /**
   * apply DOM to object
   */
  public void applyNode() {
    node.applyTo(this, rules);
  }

  public void applyNode(Storable newRoot) {
    node = newRoot;
    applyNode();
  }

  /**
   * loads=parses file and applies it to the data members of extension classes
   */
  public void load(String filename) {
    updateDOM(); //bring in defaults from object construction.
    final JsonStorable optsloader = new JsonStorable(true);
    if (optsloader.loadFile(JsonStorable.Filenamer(node, filename))) {
      try (Finally pop = dbg.Push(optsloader.filename)) {//adds filename to trace messages
        optsloader.parse(node);
        applyNode();
      }
    }
  }

  /**
   * load from default file, named for extended class.
   */
  public void load() {
    load(null);
  }

  /**
   * if @param filename is null then writes over file was loaded from, or default for extended class.
   */
  public void save(String filename) {
    // apply object to DOM
    updateDOM();
    try (PrintStream ps = new PrintStream(JsonStorable.Filenamer(node, filename))) {//#using TWR to expedite the file closing.
      JsonStorable.SaveOptions(node, ps, 0);
    } catch (FileNotFoundException e) {
      dbg.ERROR("saving options to file got {0}", e);
    }
  }

  /**
   * applies fields of extended class to the text represenation, especially useful when saving data.
   */
  public void updateDOM() {
    node.apply(this, rules);
  }

  /**
   * when you are too busy to make a variable to receive a value:
   */
  public long byName(String name, long ell) {
    Storable child = node.child(name);
    child.setDefault(ell);
    return (long) child.getIntValue();
  }

  /**
   * when you are too busy to make a variable to receive a value:
   */
  public double byName(String name, double ell) {
    Storable child = node.child(name);
    child.setDefault(ell);
    return child.getValue();
  }

  /**
   * when you are too busy to make a variable to receive a value:
   */
  public boolean byName(String name, boolean ell) {
    Storable child = node.child(name);
    child.setDefault(ell);
    return child.getTruth();
  }

  /**
   * set backing store AND related field of this object. Initially this is very picky about what it is willing to update, add more logic in setSelf as the need arises.
   */
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public boolean setByName(String name, String value, boolean autocreate) {
    Storable field = node.findChild(name, false);
    if (field != null) {
      field.setValue(value);
      field.setSelf(this, rules); //propagates changes to watchers,
      return true;
    } else {
      return false;
    }
  }

  /**
   * iteroperate with legacy properties representations
   */
  public void putInto(PropertyCursor pc) {
    node.applyTo(pc);
  }

  /**
   * iteroperate with legacy properties representations
   */
  public void putInto(Properties props, boolean stringify) {
    node.applyTo(props, stringify);
  }


  /**
   * a value that depends upon the particular system it is running on.
   * This first pass uses a hardcoded idiosyncratic key and only supports String values.
   * A default is set in the constructor so that all references easily share that.
   */
  public static class SystemSensitive {
    protected String defawlt;
    protected LinearMap<String, String> map = new LinearMap<>(5);//3 production systems and 2 developers.

    public SystemSensitive(String defawlt) {
      this.defawlt = TrivialDefault(defawlt);
    }

    /**
     * @returns system indexed value
     */
    public String value() {
      String item = map.get(System.getenv().get("servername"));
      if (item == null) {
        item = map.nth(0);
      }
      if (item == null) {
        return defawlt;
      } else {
        return item;
      }
    }

    /**
     * @returns system indexed value
     */
    @Override
    public String toString() {
      return value();
    }
  }
}
