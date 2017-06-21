package pers.hal42.transport;

import pers.hal42.logging.ErrorLogStream;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import static pers.hal42.transport.JsonStorable.filenameTag;

public class JsonOptions {
  protected static ErrorLogStream dbg = ErrorLogStream.getForClass(JsonOptions.class);//use base here, not the extension

  public Storable node;

  protected JsonOptions() {
    node = JsonStorable.StandardRoot(getClass());
//    node.applyTo(this, Storable.Rules.Deep);
  }

  public void load() {
    final JsonStorable optsloader = new JsonStorable(true);
    if (optsloader.loadFile(node.name)) {
      optsloader.parse(node);
      node.child(filenameTag).setValue(node.name);
    }
  }

  public void save(String filename) {
    node.apply(this, Storable.Rules.Master);
    try {
      JsonStorable.SaveOptions(node, new PrintStream(JsonStorable.Filenamer(node, filename)), 0);
    } catch (FileNotFoundException e) {
      dbg.ERROR("saving options to file got {0}", e);
    }
  }
}
