package pers.hal42.util;

import java.util.*;

public class StringStack {
  private static LogSwitch dumpLevel;

  public static void setDebug(LogLevelEnum lle){
    if(dumpLevel==null){
      dumpLevel = LogSwitch.getFor(StringStack.class);
    }
    dumpLevel.setto(lle.Value());
  }

  protected Stack myStack;
  public final static String stringForEmpty="StackEmpty";

  public String push(String item){
    myStack.push(item);
    return item; //???no spec on this!
  }

  public String pop() {
    try {
      return (String) myStack.pop();
    } catch (EmptyStackException ignored){
      return stringForEmpty;
    }
  }

  public String peek() {
    try {
      return (String) myStack.peek();
    } catch (EmptyStackException ignored){
      return stringForEmpty;
    }
  }

  public boolean isEmpty(){
    return myStack.empty();
  }

  public void Clear(){
    myStack.clear();
  }

  public int search(String o){
    return myStack.search(o);
  }

  private String name = "unconstructed";
  public String name() {
    return name;
  }

  public StringStack(String name){
    myStack= new Stack();
    this.name = name;
    register(this);
  }

  public String toString() {
    return toString("/");
  }

  // --- not threadsafe
  public String toString(String separator) {
    StringBuffer sb = new StringBuffer(100);
    for(int i = myStack.size(); i-->0;) {
      sb.insert(0, myStack.elementAt(i));
      sb.insert(0, separator);
    }
    return String.valueOf(sb);
  }

  /**
   * The registry is used to track the push and pops of ErrorLogStream.
   * Since the StringStacks shouldn't get created over and over, for now we can make references in a list instead of using WeakSet.
   */
  protected static WeakSet registry = null;
  private static final void register(StringStack thisone) {
    if(registry == null) {
      registry = new WeakSet(100, 10);
    }
    registry.add(thisone);
  }
  public static final TextList dumpStackList() {
    TextList ret = new TextList();

    try {
      boolean names = false;
      boolean details = false;
      boolean empties = false;
      if(dumpLevel == null) {
        ret.add("StringStack.dumpLevel is null!");
      } else {
        switch(dumpLevel.Value()) {
          case LogLevelEnum.VERBOSE:  empties = true;
          case LogLevelEnum.WARNING:  details = true;
          case LogLevelEnum.ERROR:    names   = true;
            break;
            // eventually will list ones with detected stack errors ...
        }
      }
      if(names){
        ret.add("stack count:"+registry.size());
      }

      for(Iterator iter = registry.iterator(); iter.hasNext();) {
        StringStack stack = (StringStack)iter.next();
        if(names) {
          int j = stack.myStack.size();
          if( empties || j > 0 ) {
            String line = stack.name() + "[" + j + "]";
            if(details) {
              line += ":";
              for(;j-->0;) {
                try {
                  String str = (String)stack.myStack.elementAt(j);
                  line+=str+(j==0 ? "." : ",");
                } catch (Exception ex) {
                  ret.add(ex.getMessage());
                }
              }
            }
            ret.add(line);
          }
        }
      }
    } catch(Exception e) {
      ret.add("StackList.dumpStackList: Excepted trying to generate stacklist output: " + e);
    } finally {
      return ret;
    }
  }
}

