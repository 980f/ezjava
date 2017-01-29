/**
 *
 * To define a class to be testable, define and fill in the following functions
 * for that class:
 *
 *   public static String Usage() {
 *     return "put the usage string here";
 *   }
 *
 *   public static void Test(String[] args) {
 *     // whatever
 *   }
 *
 * ----
 *
 * All of the public functions of the class except for main()
 * can be used by other classes (like gui stuff) to also run tests
 * (and maybe one day, automated testers!)
 *
 * ----
 *
 * To run this from the commandline (paymate.jar could be something else):
 * java -cp paymate.jar net.paymate.util.Tester
 *   or
 * java -cp paymate.jar net.paymate.util.Tester usage
 *   or
 * java -cp paymate.jar net.paymate.util.Tester usage net.paymate.connection.ConnectionClient
 *   or
 * java -cp paymate.jar net.paymate.util.Tester net.paymate.connection.ConnectionClient
 *
 * no arguments                 - lists options available for this class's main()
 * 'usage'                      - lists ALL testable classes and their parameters
 * 'usage <classname>'          - lists the parameters for that class
 * '<classname> [<parameters>]' - tests the class
 *
 * ----
 *
 * NOTE:
 *  I tried creating objects of this as static members of classes and
 * then having them put themselves in a list when they were created.
 * However, the objects won't get created until the classes they are in
 * are needed somewhere else and are loaded!  So, basically, that won't work!
 *
 */

package pers.hal42.util;

import java.util.Arrays;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

// +++ possibly deal with threading issues?
// +++ hopefully we would never call this with two separate threads

public class Tester {

  protected static final ErrorLogStream dbg = new ErrorLogStream(Tester.class.getName());

  public static final String getUsage(String className) {
    return runMethod(className, usageMethodName, usageArgs, null, false);
  }

  public static final void doTest(String className, String[] args) {
    runMethod(className, testMethodName, testArgs, args, true);
  }

  private static final String [] USAGE = {
    "",
    Tester.class.getName() + " usage:",
    "To run this from the commandline (ezjava.jar could be something else):",
    "   java -cp ezjava.jar pers.hal42.util.Tester <parameters>",
    "parameters:",
    "   <none>                        - list options for this class's main()",
    "   usage <class1> [<class2> ...] - list parameters for specific classes",
    "   <classname> [<params>]        - test a class",
    "NOTE: when typing a classname, 'pers.hal42.' is not required; it is assumed."
  };

  /**
   * +++ Do more in here later (do switches and turn off console logging, and make it log plain, etc.)
   */
  public static final void main(String[] args) {
    int numArgs = args.length;
    ErrorLogStream.Console(ErrorLogStream.VERBOSE);
    if(numArgs == 0) {
      dbg.logArray(dbg.VERBOSE, null, USAGE);
    } else {
      if(usageMethodName.equalsIgnoreCase(args[0])) {
        if(numArgs > 1) {
          // just do specific classes
          System.out.println("");
          for(int i = 1; i < numArgs; i++) {
            System.out.println(args[i] + ": " + getUsage(makeClassName(args[i])));
            System.out.println("----");
          }
        } else {
          dbg.logArray(dbg.ERROR,null, USAGE);
        }
      } else {
        // test a class
        doTest(makeClassName(args[0]), shaveNargs(args, 1));
      }
    }
  }

  /**
   * shaves the first n arguments from the front of the array
   * (basically makes another array with the first n removed)
   * serves as a utility function that main() classes can use to pass limited args with
   */

  public static final String [] shaveNargs(String[] args, int n) {
    int newLen = args.length - n;
    String[] newArgs = new String[newLen];
    System.arraycopy(args, n, newArgs, 0, newLen);
    return newArgs;
  }

  public static final String makeClassName(String unknown) {
    return unknown.startsWith(classPrefix) ? unknown : classPrefix + unknown;
  }

  // as parameters to class.getMethod()
  private static final String testArg[] = {};
  private static final Class testArgs[] = {testArg.getClass()};
  private static final Class usageArgs[] = {};
  // one-case strings
  private static final String classPrefix = "net.paymate.";
  private static final String usageMethodName = "Usage";
  private static final String testMethodName = "Test";
  // local utility functions
  private static final boolean validClass(Class c) {
    return validObject(c, "Class");
  }
  private static final boolean validMethod(Method m, String name) {
    return validObject(m, "Method " + name);
  }
  private static final boolean validObject(Object o, String type) {
    boolean valid = (o != null);
    if(!valid) {
      dbg.ERROR(type + " not found!");
    }
    return valid;
  }
  private static final Method getMethod(Class c, String methodName, Class[] parameterTypes) {
    Method m = null;
    if(validClass(c)) {
      try {
        m = c.getMethod(methodName, parameterTypes);
      } catch (Exception e) {
        // not really anything to do
      }
    }
    return m;
  }
  private static final String runMethod(String className, String methodName, Class[] classargs, String[] args, boolean debugArgsNoReturn) {
    String s = null;
    try {
      Class c = Class.forName(className);
      Method m = getMethod(c, methodName, classargs);
      if(validMethod(m, methodName)) {
        if(debugArgsNoReturn) {
          Object oargs[] = {args};
          m.invoke(null, oargs);
        } else {
          s = (String) m.invoke(null, null);
        }
      } else {
        s = className + " or " + className + "." + methodName + "()' not found!";
      }
    } catch (Exception e) {
      dbg.Caught(e);
    }
    return s;
  }
}
