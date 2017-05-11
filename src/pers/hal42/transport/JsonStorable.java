package pers.hal42.transport;

import org.jetbrains.annotations.NotNull;
import pers.hal42.ext.CountedLock;
import pers.hal42.ext.Span;
import pers.hal42.lang.ByteArray;
import pers.hal42.logging.ErrorLogStream;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;

/**
 * Created by Andy on 5/8/2017.
 */
public class JsonStorable extends PushedJSONParser {
  static ErrorLogStream dbg = ErrorLogStream.getForClass(JsonStorable.class);
  public Storable root;
  public String filename;
  public boolean specialRootTreatment = true;//@see BeginWad clause of parse().

  //extend if you want access to these.
  protected Path path;
  protected byte[] content;
  protected boolean cached;

  /**
   * record name early, for debug, could wait until makeChild
   */
  String name = "";

  public JsonStorable(boolean equalsAndSemi) {
    super(equalsAndSemi);
  }

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
   * just read the file
   */
  public boolean loadFile(String filename) {
    this.filename = filename; //record for debug
    path = FileSystems.getDefault().getPath(filename);
    return cache();
  }

  protected Storable makeChild(Storable parent) {
    ++stats.totalNodes;
    Storable kid = parent.makeChild(haveName ? name : null);//ternary in case we forgot to erase previous one
    String value = super.value.isValid() ? getString(super.value) : null;
    if(value!=null){
      ++stats.totalScalar;//<=totalNodes.
    }
    kid.setValue(value);
    itemCompleted();//to prevent duplication or propagation of values over nulls.
    name="!error";//this should stand out if we carelessly use it.
    return kid;
  }

  /**
   * file might have been truncated
   */
  protected void cleanup(Storable node) {
    if (value.isValid() || (haveName && super.name.isValid())) {
      makeChild(node);
    }
  }

  int illegalsCount = 0;

  /**
   * the return value here is inspected by the BeginWad clause's while.
   * The initial call should get a false if the file is well formed, a true suggests a truncated file.
   * Inspecting the parser.stats member for depth also would help decipher truncation.
   */
  public boolean parse(Storable parent) {
    try (CountedLock popper = stats.depthTracker.open(d.location)) {
      dbg.VERBOSE(MessageFormat.format("Json File Depth now {0}/{1}", popper.asInt(), stats.depthTracker.maxDepth));
      if (root == null) {
        root = parent;//wild stab at averting NPE's. often is the expected thing.
      }
      if (cached) {
        while (d.location < content.length) {//todo: use a bytearraystream
          char b = (char) content[d.location];
          switch (nextitem(b)) {
            case Continue:
              continue;//like the case says ;)
            case Illegal://not much is illegal.
              if (++illegalsCount < 100) {//give up complaining after a while, so as to speed up failure.
                dbg.ERROR(MessageFormat.format("Illegal character at row {0}:column {1}, will pretend it's not there. (1-based coords)", d.row, d.column));
              }
              break;
            case BeginWad: {
              Storable kid;
              if (parent == root && specialRootTreatment) {//#same object, not equivalent.
                //ignore 'begin wad' on root node, so that a standard json file (one value) can parse into an already existing node.
                kid = parent;
              } else {//this is a wad type child node
                //save present item as a wad
                kid = makeChild(parent);
              }
              while (parse(kid)) {//recurse
                //nothing to do here.
              }
              return true;
            }
            case EndItem:
              makeChild(parent);
              return true;//might be more children
            case Done://terminate children all the way up.
              return false;//end wad because we are at end of file.
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
    } catch (Exception e) {
      dbg.Caught("parsing", e);
      return true;
    }
  }
}

