package pers.hal42.transport;

import pers.hal42.logging.ErrorLogStream;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import static pers.hal42.transport.JsonStorable.filenameTag;

public class JsonOptions {
  protected static ErrorLogStream dbg = ErrorLogStream.getForClass(JsonOptions.class);//use base here, not the extension

  /** root of the options DOM, typically named for the file it is loaded from */
  @Storable.Ignore
  public Storable node;
  /** how forcefully to map the DOM to the object */
  @Storable.Ignore
  public Storable.Rules rules;

  protected JsonOptions() {
    node = JsonStorable.StandardRoot(getClass());
    rules = Storable.Rules.Master;
  }

  /** apply DOM to object */
  public void applyNode() {
    node.applyTo(this, rules);
  }

  public void applyNode(Storable newRoot) {
    node = newRoot;
    applyNode();
  }

  public void load() {
    final JsonStorable optsloader = new JsonStorable(true);
    if (optsloader.loadFile(node.name)) {
      optsloader.parse(node);
      node.child(filenameTag).setValue(node.name);
      applyNode();
    }
  }

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
}
