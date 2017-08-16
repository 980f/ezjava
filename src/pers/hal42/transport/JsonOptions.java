package pers.hal42.transport;

import pers.hal42.logging.ErrorLogStream;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Properties;

/**
 * a storable node which is associated with a file.
 * Extend from this as the concrete class name is used to find the file.
 * It is intended for you to have your stored members in your extension of this class.
 */
@Storable.Stored  //someday inheritance might work with this
public class JsonOptions {
  /** root of the options DOM, typically named for the file it is loaded from */
  transient  //don't want unexpected recursions
  public Storable node;
  /** how forcefully to map the DOM to the object */
  transient  //don't save, and especially don't load, the rules.
  public Storable.Rules rules;
  protected static final ErrorLogStream dbg = ErrorLogStream.getForClass(JsonOptions.class);//use base here, not the extension

  protected JsonOptions() {
    node = JsonStorable.StandardRoot(getClass());
    rules = Storable.Rules.Master;
  }

  /**
   * create and initialize from random file @param altfilename
   */
  public JsonOptions(String altfilename) {//public for test access, normal use is to derive from this class.
    node = new Storable(altfilename);
    rules = Storable.Rules.Master;
    load(altfilename);
  }

  /** apply DOM to object */
  public void applyNode() {
    node.applyTo(this, rules);
  }

  public void applyNode(Storable newRoot) {
    node = newRoot;
    applyNode();
  }

  public void load(String filename) {
    final JsonStorable optsloader = new JsonStorable(true);
    if (optsloader.loadFile(JsonStorable.Filenamer(node, filename))) {
      optsloader.parse(node);
      applyNode();
    }
  }

  /** load from default file, named for extended class. */
  public void load() {
    load(null);
  }

  /** if @param filename is null then writes over file was loaded from, or default for extended class. */
  public void save(String filename) {
    // apply object to DOM
    updateDOM();
    try {
      JsonStorable.SaveOptions(node, new PrintStream(JsonStorable.Filenamer(node, filename)), 0);
    } catch (FileNotFoundException e) {
      dbg.ERROR("saving options to file got {0}", e);
    }
  }

  public void updateDOM() {
    node.apply(this, rules);
  }

  /** when you are too busy to make a variable to receive a value: */
  public long byName(String name, long ell) {
    Storable child = node.child(name);
    child.setDefault(ell);
    return (long) child.getValue();
  }

  /** when you are too busy to make a variable to receive a value: */
  public double byName(String name, double ell) {
    Storable child = node.child(name);
    child.setDefault(ell);
    return child.getValue();
  }

  /** when you are too busy to make a variable to receive a value: */
  public boolean byName(String name, boolean ell) {
    Storable child = node.child(name);
    child.setDefault(ell);
    return child.getValue() != 0.0;
  }

  public void putInto(PropertyCursor pc) {
    node.applyTo(pc);
  }

  public void putInto(Properties props, boolean stringify) {
    node.applyTo(props, stringify);
  }
}
