package pers.hal42.util;

import java.util.EmptyStackException;
import java.util.Stack;


public class StringStack {

  protected Stack<String> myStack;
  public final static String stringForEmpty = "StackEmpty";

  public String push(String item) {
    myStack.push(item);
    return item; //???no spec on this!
  }

  public String pop() {
    try {
      return myStack.pop();
    } catch (EmptyStackException ignored) {
      return stringForEmpty;
    }
  }

  public String top() {
    try {
      return myStack.peek();
    } catch (EmptyStackException ignored) {
      return stringForEmpty;
    }
  }

  public boolean isEmpty() {
    return myStack.empty();
  }

  public void Clear() {
    myStack.clear();
  }

  public int search(String o) {
    return myStack.search(o);
  }

  private String name = "unconstructed";

  public String name() {
    return name;
  }

  public StringStack(String name) {
    myStack = new Stack<>();
    this.name = name;
//    register(this);
  }

  public String toString() {
    return toString("/",true);
  }

 /** @returns a concatenation of stack entries from bottom to top, works nicely for file pathnames. */
  public String toString(String separator,boolean prefix) {
    StringBuilder sb = new StringBuilder(30*myStack.size());//guesstimate 29 bytes for typical string in the stack, a totally off the wall number.
    for (String item:myStack ) {
      if(prefix){
        sb.append(separator);
      }
      sb.append(item);
      if(!prefix) {
        sb.append(separator);
      }
    }
    return sb.toString();
  }
}

//  /**
//   * The registry is used to track the push and pops of ErrorLogStream.
//   * Since the StringStacks shouldn't get created over and over, for now we can make references in a list instead of using WeakSet.
//   */
//  private static LogSwitch dumpLevel;
//
//  public static void setDebug(LogLevelEnum lle){
//    if(dumpLevel==null){
//      dumpLevel = LogSwitch.getFor(StringStack.class);
//    }
//    dumpLevel.setLevel(lle);
//  }
//  protected static WeakSet<StringStack> registry = null;
//  private static void register(StringStack thisone) {
//    if(registry == null) {
//      registry = new WeakSet(100, 10);
//    }
//    registry.add(thisone);
//  }
//  public static TextList dumpStackList() {
//    TextList ret = new TextList();
//
//    try {
//      boolean names = false;
//      boolean details = false;
//      boolean empties = false;
//      if(dumpLevel == null) {
//        ret.add("StringStack.dumpLevel is null!");
//      } else {
//        switch(dumpLevel.Level()) {
//          case VERBOSE:  empties = true;
//          case WARNING:  details = true;
//          case ERROR:    names   = true;
//            break;
//            // eventually will list ones with detected stack errors ...
//        }
//      }
//      if(names){
//        ret.add("stack count:"+registry.size());
//      }
//
//      for (Object aRegistry : registry) {
//        StringStack stack = (StringStack) aRegistry;
//        if (names) {
//          int j = stack.myStack.size();
//          if (empties || j > 0) {
//            String line = stack.name() + "[" + j + "]";
//            if (details) {
//              line += ":";
//              for (; j-- > 0; ) {
//                try {
//                  String str = (String) stack.myStack.elementAt(j);
//                  line += str + (j == 0 ? "." : ",");
//                } catch (Exception ex) {
//                  ret.add(ex.getMessage());
//                }
//              }
//            }
//            ret.add(line);
//          }
//        }
//      }
//    } catch(Exception e) {
//      ret.add("StackList.dumpStackList: Excepted trying to generate stacklist output: " + e);
//    } finally {
//      return ret;
//    }
//  }
//}
//
