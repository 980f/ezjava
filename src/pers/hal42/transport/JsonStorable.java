package pers.hal42.transport;

import org.jetbrains.annotations.NotNull;
import pers.hal42.ext.Span;
import pers.hal42.lang.ByteArray;
import pers.hal42.logging.ErrorLogStream;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by Andy on 5/8/2017.
 */
public class JsonStorable extends PushedJSONParser {
  static ErrorLogStream dbg = ErrorLogStream.getForClass(JsonStorable.class);
  public Storable root;
  public String filename;

  public Path path;
  byte[] content;
  boolean cached;

  /**
   * record name early, for debug, could wait until makeChild
   */
  String name;

  protected void recordName() {
    name = getString(super.name);
  }

  @NotNull
  protected String getString(Span span) {
    return new String(ByteArray.subString(content, span.lowest, span.highest));
  }

  public boolean cache() {
    if (!cached) {
      try {
        content = Files.readAllBytes(path);
        cached = content.length > 0;
      } catch (IOException e) {
        dbg.Caught("reading json as bytes", e);
      }
    }
    return cached;
  }

  /**
   * read file and parse it permissivley to a Storable. Try to guess node types, but not aggressively
   */
  public boolean loadFile(String filename) {
    path = FileSystems.getDefault().getPath(filename);
    return cache();
  }

  protected Storable makeChild(Storable parent) {
    Storable kid = parent.makeChild(haveName ? name : null);//ternary in case we forgot to erase previous one
    String value = super.value.isValid() ? getString(super.value) : null;
    kid.setValue(value);
    return kid;
  }

  /**
   * file might have been truncated
   */
  protected void cleanup(Storable node) {
    if (value.isValid() || super.name.isValid()) {
      makeChild(node);
    }
  }

  /** the return value here is inspected by the BeginWad clause's while.
   * The initial call should get a false if the file is well formed, a true suggests a truncated file.
   * Inspecting the parser.stats member for depth also would help decipher truncation.
   * */
  public boolean parse(Storable parent) {
    try// (stats.depthTracker.open())
     {
      if (root == null) {
        root = parent;//wild stab at averting NPE's. often is the expected thing.
      }
      if (cached) {
        for (byte b : content) {
          switch (nextitem((char) b)) {
          case Continue:
            continue;//like the case says ;)
          case Illegal://not much is illegal.
            //todo: dioagnostic report
            break;
          case Done://terminate children all the way up.
            return false;//end wad because we are at end of file.
//        break;
          case BeginWad: {
            //save present item as a wad
            Storable kid = makeChild(parent);
            while (parse(kid)) {
            }
            return true;
          }
//        break;
          case EndItem:
            makeChild(parent);
            return true;//might be more children
          case EndWad:
            //if there is an item pending add it to wad, this will be the case if the last item of a wad doesn't have a gratuitous comma after it.
            cleanup(parent);
            return false;//no more children
          }
        }
        //no more content
        //were we in the middle of an item?
        cleanup(parent);
      } else {
        return false;
      }
      return false;
    } catch( Exception e){
      dbg.Caught("parsing",e);
      return true;
    }
  }
}

