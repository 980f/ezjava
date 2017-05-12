package pers.hal42.transport;

import org.jetbrains.annotations.NotNull;
import pers.hal42.ext.CountedLock;
import pers.hal42.ext.Span;
import pers.hal42.lang.ByteArray;
import pers.hal42.lang.StringX;
import pers.hal42.logging.ErrorLogStream;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Vector;

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
        dbg.Caught(e, "reading json as bytes");
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
    if (value != null) {
      ++stats.totalScalar;//<=totalNodes.
    }
    kid.setValue(value);
    itemCompleted();//to prevent duplication or propagation of values over nulls.
    name = "!error";//this should stand out if we carelessly use it.
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
      dbg.VERBOSE("Json File Depth now {0}/{1}", popper.asInt(), stats.depthTracker.maxDepth);
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
                dbg.ERROR("Illegal character at row {0}:column {1}, will pretend it's not there. (1-based coords)", d.row, d.column);
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
      dbg.Caught(e, "parsing");
      return true;
    }
  }

  public static Storable ClassOptions(Class claz) {
    String optsfile = claz.getSimpleName();
    Storable root = new Storable(optsfile);//named 4 debug
    optsfile += ".json";
    final JsonStorable optsloader = new JsonStorable(true);
    if (optsloader.loadFile(optsfile)) {
      optsloader.parse(root);
      root.child("#filename").setValue(optsfile);
    }
    //todo: either dbg the stats or print them to a '#attribute field
    return root;
  }

  public static class Printer {


    PrintStream ps;
    int tablevel;

    public Printer(final PrintStream ps, final int tablevel) {
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

    boolean printValue(Storable node,int last) {
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
          ps.print(node.getTruth()?"true":"false");
          break;
        default:
        case Unclassified:
          if (printName(node)) {
            ps.print("!Unknown"); //fix your own code whenever you see one of these
          } else {
            return false;
          }
          //else the node disappears
          break;
        case Numeric: {
          printName(node);
          double number = node.getValue();
          //todo:1 output nans as keyword
          if ((long) number == number) {
            ps.print((long) number);
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
      if(node.ordinal()<last) {
        ps.println(',');
      }
      return true;
    } /* printValue */

    void printWad(Vector<Storable> wad) {
      ps.println('{');
      ++tablevel;
      int last=wad.size()-1;
      wad.forEach(node -> printValue(node,last));
      --tablevel;
      indent();
      ps.print('}');
    } /* printWad */

  }
  /**
   * before calling this you probably need to node.apply(Object obj)
   */
  public static void SaveOptions(Storable node, PrintStream ps, int tablevel) {
    if (node != null && ps != null && tablevel >= 0) {
      Printer print=new Printer(ps,tablevel);
      print.printValue(node,node.numChildren()-1);
    }
  }

}

