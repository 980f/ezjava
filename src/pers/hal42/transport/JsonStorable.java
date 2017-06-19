package pers.hal42.transport;

import org.jetbrains.annotations.NotNull;
import pers.hal42.ext.CountedLock;
import pers.hal42.ext.Span;
import pers.hal42.lang.ByteArray;
import pers.hal42.lang.ReflectX;
import pers.hal42.lang.StringX;
import pers.hal42.logging.ErrorLogStream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Vector;

/**
 * Created by Andy on 5/8/2017.
 * <p>
 * mate JSON object persistance stuff to a file.
 */
public class JsonStorable extends PushedJSONParser {
  static ErrorLogStream dbg = ErrorLogStream.getForClass(JsonStorable.class);
  private static final String filenameTag = "#filename";
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
  int illegalsCount = 0;

  public JsonStorable(boolean equalsAndSemi) {
    super(equalsAndSemi);
  }

  /** called by parser when it discovers the end of a value */
  protected Storable makeChild(Storable parent) {
    ++stats.totalNodes;
    Storable kid = parent.makeChild(haveName ? name : null);//ternary in case we forgot to erase previous parse results
    String value = super.token.isValid() ? getString(super.token) : null;
    if (value != null) {
      ++stats.totalScalar;//<=totalNodes.
    }
    kid.setValue(value);
    itemCompleted();//to prevent duplication or propagation of values over nulls.
    name = "";
    return kid;
  }

  /** alter one member of the file */
  public static IOException patchFile(Path optsFile, String path, String image) {
    Storable workspace=FromFile(optsFile);
    if(workspace!=null){
      Storable child=workspace.findChild(path,true);
      child.setValue(image);
      try {
        PrintStream ps = new PrintStream(new File(optsFile.toString()));
        SaveOptions(workspace,ps,0);
        return null;
      } catch (FileNotFoundException e) {
        return e;
      }
    } else {
      return new NoSuchFileException(optsFile.toString());
    }
  }

  public static Storable FromFile(Path optsfile){
    Storable root = new Storable(optsfile.toString());//name in file gets lost
    final JsonStorable optsloader = new JsonStorable(true);
    if (optsloader.loadFile(optsfile.toString())) {//todo: path versions of this method
      optsloader.parse(root);
      root.child(filenameTag).setValue(optsfile.toString());
    }
    //todo:2 either dbg the stats or print them to a '#attribute field
    return root;
  }

  /**
   * @returns newly created Storable with values loaded from file named for @param claz
   */
  public static Storable ClassOptions(Class claz) {
    String optsfile = Filename(claz);
    Storable root = new Storable(optsfile);//named 4 debug
    final JsonStorable optsloader = new JsonStorable(true);
    if (optsloader.loadFile(optsfile)) {
      optsloader.parse(root);
      root.child(filenameTag).setValue(optsfile);
    }
    //todo:2 either dbg the stats or print them to a '#attribute field
    return root;
  }

  /** read file content if not already done. */
  public boolean cache() {
    if (!cached) {
      try {
        content = Files.readAllBytes(path);
        cached = content.length > 0;
      } catch (IOException e) {
        dbg.ERROR("reading json as bytes threw {0}", e.getMessage());
      }
    }
    return cached;
  }

  /**
   * writes pretty-printed json formatted data on @param ps. @param tablevel is the number of tabs to put at the start of each line of text, must be >=0.
   * before calling this you probably need to node.apply(Object obj) to update your storable from its related live object.
   */
  public static void SaveOptions(Storable node, PrintStream ps, int tablevel) {
    if (node != null && ps != null && tablevel >= 0) {
      Printer print = new Printer(ps, tablevel);
      print.printValue(node);
    }
  }

  protected void recordName() {
    name = getString(super.name);
  }

  @NotNull
  protected String getString(Span span) {
    return new String(ByteArray.subString(content, span.lowest, span.highest));
  }

  /** @returns canonical filename for options file for @param claz */
  public static String Filename(Class claz) {
    String optsfile = ReflectX.justClassName(claz); //wouold prefer outer class name, but at least outer.inner, inner alone wasn't usually unique.
    optsfile += ".json";
    return optsfile;
  }

  /**
   * just read the file, don't parse
   */
  public boolean loadFile(String filename) {
    this.filename = filename; //record for debug
    path = FileSystems.getDefault().getPath(filename);
    return cache();
  }

  public static String Filenamer(Storable root, String filename) {
    if (root == null) {
      return null;
    }
    Storable tagged = root.existingChild(filenameTag);
    if (tagged != null) {
      return tagged.getImage();
    }
    return StringX.NonTrivial(root.name) ? root.name : filename;
  }

  /**
   * file might have been truncated, try to accept a final item even if commas and endbraces were clipped off.
   */
  protected void cleanup(Storable node) {
    if (token.isValid() || (haveName && super.name.isValid())) {
      makeChild(node);
    }
  }

  /**
   * the return value here is inspected by the BeginWad clause's while.
   * The initial call should get a false if the file is well formed, a true suggests a truncated file.
   * Inspecting the parser.stats member for depth also would help decipher truncation.
   */
  public boolean parse(Storable parent) {
    try (CountedLock popper = stats.depthTracker.open(d.location)) {
//      dbg.VERBOSE("Json File Depth now {0}/{1}", popper.asInt(), stats.depthTracker.maxDepth);
      if (root == null) {
        root = parent;//wild stab at averting NPE's. often is the expected thing.
      }
      if (cached) {
        while (d.location < content.length) {//todo: use a bytearraystream
          byte b = content[d.location];
          switch (nextitem(b)) {
          case Continue:
            continue;//like the case says ;)
          case Illegal://not much is illegal.
            if (++illegalsCount < 100) {//give up complaining after a while, so as to speed up failure.
              dbg.ERROR("Illegal character at row {0}:column {1}, will pretend it's not there. (1-based coords)", d.row, d.column);
            }
            break;
          case BeginWad: {
            Storable kid;
            if (parent == root && specialRootTreatment) {//#same object, not just equivalent.
              specialRootTreatment = false;//only do once else we flatten the input file.
              //ignore 'begin wad' on root node, so that a standard json file (one value) can parse into an already existing node.
              itemCompleted();//erase dregs, name is ignored
              kid = parent;
            } else {//this is a wad type child node
              //save present item as a wad
              kid = makeChild(parent);
              kid.setType(Storable.Type.Wad);
            }
            //noinspection StatementWithEmptyBody
            while (parse(kid)) {//recurse
              //nothing to do here.
            }
            return true;
          }
          case EndItem:
            if(haveName|| token.isValid()) {
              makeChild(parent);
            } else {
              //todo:1 to filter this out need a flag 'lastItem was an endWad'
              dbg.VERBOSE("Ignoring null item, probably gratuitous comma or comma after end brace");
            }
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
      dbg.Caught(e, "parsing");
      return true;
    }
  }

  public static class Printer {
    PrintStream ps;
    int tablevel;

    public Printer(final PrintStream ps, int tablevel) {
      this.ps = ps;
      this.tablevel = tablevel;
    }

    void indent() {
      for (int tabs = tablevel; tabs-- > 0; ) {
        ps.append('\t'); //or we could use spaces
      }
    }

    void printText(String p, boolean forceQuote) {
      if (forceQuote) {//todo: or of string contains any of the parse separators or a space or ...
        ps.append('"');
      }
      ps.append(p);
      if (forceQuote) {
        ps.append('"');
      }
    } // printText

    boolean printName(Storable node) {
      indent();
      if (StringX.NonTrivial(node.name)) {
        printText(node.name, true); //forcing quotes here, may not need to
        ps.append(':');
        return true;
      } else {
        return false;
      }
    }

    boolean printValue(Storable node) {
      if (node == null) { //COA
        return false;
      }
      if (node.isTrivial()) { //drop autocreated but unused nodes.
        return false;
      }
//this c++ functionality must be done by caller, until we register watchers and all that jazz.      node.preSave(); //when not trivial this flushes cached representations

      switch (node.type) {
      case Null:
        printName(node);
        ps.print("null"); //fix code whenever you see one of these
        break;
      case Boolean:
        printName(node);
        ps.print(java.lang.Boolean.toString(node.getTruth()));//  node.getImage());
        break;
      default:
      case Unclassified:
        printName(node);
        printText(node.getImage(), true); //always quote
        break;
      case Numeric: {
        printName(node);
        double number = node.getValue();
        //todo:1 output nans as keyword
        if ((long) number == number) {//if is an integer value
          ps.print((long) number);//print nicely
        } else {
          ps.print(number);
        }
      }
      break;
      case Textual:
        printName(node);
        printText(node.getImage(), true); //always quote
        break;
      case Wad:
        printName(node);
        printWad(node.wad);
        break;
      } /* switch */
      int which=node.ordinal();
      if (which>=0 && node.parent!=null && node.parent.numChildren()>which+1) {//not the last child of a wad, not the top level node.
        ps.println(',');
      }
      return true;
    } /* printValue */

    void printWad(Vector<Storable> wad) {
      ps.println('{');
      ++tablevel;
      int last = wad.size() - 1;
      wad.forEach(this::printValue);
      --tablevel;
      indent();
      ps.print('}');
    } /* printWad */

  }

}

