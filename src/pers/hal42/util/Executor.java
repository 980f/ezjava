package pers.hal42.util;


import pers.hal42.lang.StringX;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.text.TextList;
import pers.hal42.thread.ThreadX;
import pers.hal42.timer.Ticks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * run a shell command, recording its output and displaying a progress bar on the debug console
 */
public class Executor {
  public int displayUpdateSeconds;
  public int timeoutSeconds;
  public TextList msgs; //need to use a PrintStream
  public boolean verbose;
  protected static final ErrorLogStream dbg = ErrorLogStream.getForClass(Executor.class);
  public Executor(int displayUpdateSeconds, int timeoutSeconds, TextList msgs, boolean verbose) {
    this.displayUpdateSeconds = displayUpdateSeconds;
    this.timeoutSeconds = timeoutSeconds;
    this.msgs = msgs;
    this.verbose = verbose;
  }

  private void pStream(BufferedReader in, TextList msgs) { // hehe
    try {
      while (in.ready()) {
        msgs.add(in.readLine());
      }
    } catch (IOException ioe) {
      dbg.Push("pStream");
      dbg.Caught(ioe);
      dbg.Exit();
    }
  }

  public void runFiles(String wildCommand, TextList fileset) {
    String commandLine = wildCommand; //in case there are no substitutions...
    int F2 = wildCommand.indexOf(2);  //^PB on my editor.
    for (int i = fileset.size(); i-- > 0; ) {
      if (F2 >= 0) {//the first of PFM's substitution functions:
        commandLine = wildCommand.substring(0, F2) + fileset.itemAt(i) + wildCommand.substring(F2 + 1);
      }
      int retcode = runProcess(commandLine, "run :" + fileset.itemAt(i), null);
      //report retcode to the nonexistent reporting stream...
    }
  }

  public int runProcess(String commandline, String startMsg, String primer) {
    // these are while-running fields
    BufferedReader in;
    BufferedReader err;
    PrintStream out;
    boolean done;
    if (commandline == null) {
      return -2;
    }
//    if(startMsg==null){
//      return -3;
//    }
    if (msgs == null) {
      // create one for our local use if you won't give us one
      msgs = new TextList(100, 10);
    }
    if (startMsg != null) {
      dbg.VERBOSE(startMsg);
    }

    //    dbg.Message(new TextList("Executing: " +  commandline, 78, false).asParagraph(" ", null));
    commandline = commandline.trim();
    try {
      Process process = Runtime.getRuntime().exec(commandline);

      in = new BufferedReader(new InputStreamReader(process.getInputStream()));
      err = new BufferedReader(new InputStreamReader(process.getErrorStream()));
      out = new PrintStream(process.getOutputStream());
      boolean kill = (timeoutSeconds > 0);
      if (displayUpdateSeconds == 0) {
        displayUpdateSeconds = 1;
      }
      int numDots = (kill ? ((timeoutSeconds * 1000) / (displayUpdateSeconds * 1000)) : 20);
      done = false;
      // ------ testing !!!!!!!
      if (StringX.NonTrivial(primer)) {
        try {
          out.write(primer.getBytes());
          out.flush();
        } catch (Exception ex) {
          dbg.Caught(ex);
        }
      }

      if ((timeoutSeconds < 1) && (displayUpdateSeconds < 1)) {
        // don't watch the process; trust that it will finish
        try {
          process.waitFor();
        } catch (InterruptedException ie) {
          Thread.interrupted(); // clears interrupted bits
          // who cares; just carry on
        }
      } else {
        int counter = numDots;
        int lastSize = 0;
        while (!done && !(kill && !(counter > 0))) {
          // these have to be run regardless of whether it is verbose or not!!!! (for getmacid)
          pStream(in, msgs);
          pStream(err, msgs);
          if (verbose) {
            while (msgs.size() > lastSize) {
              dbg.VERBOSE(msgs.itemAt(lastSize++));
            }
          }
          out.println(""); // slap?

          ThreadX.sleepFor(Ticks.forSeconds(displayUpdateSeconds));

          try {
            process.exitValue();  // throws if process is not complete; how I tell it isn't
            done = true;
          } catch (IllegalThreadStateException itse) {
          }
          counter--;
          if (!done && !kill && (counter == 0)) {
            counter = numDots;// cycle around
          }
        }
      }
      pStream(in, msgs);  // these must be used always for getmacid
      pStream(err, msgs);

      if (!done) {
        process.destroy();
        dbg.ERROR(kill ? "Process took too long and was terminated!" : "User killed process.");
      }

      int x = -1;
      try {
        x = done ? process.exitValue() : -1;
      } catch (IllegalThreadStateException itse2) {
        // who cares
      }
      if (x != 0) {
        dbg.WARNING("Process exited with value " + x);
      }
      return x;
    } catch (Exception ioe) {
      dbg.ERROR("Could not start process [" + commandline + "] due to exception:");
      dbg.Caught(ioe);
      // just quit
      return -1;
    }
  }

  /**
   * +++ make this accept a character on the commandline (System.in) that will kill it without killing the whole thread/process so that errors will get printed
   * <p>
   * +++ divide this up some!!!!
   * <p>
   * +++ make some of the display stuff parameterized? (in constructor?)
   */

  public static int runProcess(String commandline, String startMsg, int displayUpdateSeconds, int timeoutSeconds, TextList msgs) {
    return runProcess(commandline, startMsg, displayUpdateSeconds, timeoutSeconds, msgs, false, null);
  }

  public static int runProcessSilently(String commandline, int timeoutSeconds, TextList msgs) {
    dbg.WARNING("runProcessSilently:" + commandline);
    return runProcess(commandline, commandline, 0, timeoutSeconds, msgs, false, null);
  }


  /*
  if timeoutSeconds == 0 and updateSeconds == 0,
  the process will not be watched at all  and there won't be any output to the screen until it's waiter
  if displayUpdateSeconds == 0, the display will not be shown
  if timeoutSeconds == 0, the process won't be killed; will run until waiter
  for display purposes, it's best to make displayUpdateSeconds == 1
  it's also best if they are both even, or if displayUpdateSeconds == 1
  */

  public static int ezExec(String commandline, int timeoutSeconds) {
    dbg.WARNING("ezExec:" + commandline);
    return runProcess(commandline, "timeout:" + timeoutSeconds, 0, timeoutSeconds, null, false, null);
  }

  public static int runProcess(String commandline, String startMsg, int displayUpdateSeconds, int timeoutSeconds, TextList msgs, boolean verbose, String primer) {
    Executor legacy = new Executor(displayUpdateSeconds, timeoutSeconds, msgs, verbose);
    return legacy.runProcess(commandline, startMsg, primer);
  }
}


/*
 import java.io.InputStream;
 import org.jcrontab.log.Log;

//
// This class executes a native command
//
 public class NativeExec {
//
//   main method
//   @param args String[] the params passed from the console
//
     public static void main(String args[]) {
         if (args.length < 1) {
             System.out.println("java org.jcrontab.NativeExec <cmd>");
             System.exit(1);
         }
         String[] cmd = null;

         try {
   //with this variable will be waiter the swithcing
             String osName = System.getProperty("os.name" );

       //only will work with Windows NT
             if( osName.equals( "Windows NT" ) ) {
                 if (cmd == null) cmd = new String[ args.length + 2];
                 cmd[0] = "cmd.exe" ;
                 cmd[1] = "/C" ;
                 for (int i = 0; i<args.length; i++)
                     cmd[i+2] = args[i];
             }
       //only will work with Windows 95
             else if( osName.equals( "Windows 95" ) ) {
                 if (cmd == null) cmd = new String[args.length + 2];
                 cmd[0] = "command.com" ;
                 cmd[1] = "/C" ;
                 for (int i = 0; i<args.length; i++)
                     cmd[i+2] = args[i];
             }
       //only will work with Windows 2000
       else if( osName.equals( "Windows 2000" ) ) {
                 if (cmd == null) cmd = new String[args.length + 2];
                 cmd[0] = "cmd.exe" ;
                 cmd[1] = "/C" ;

                 for (int i = 0; i<args.length; i++)
                     cmd[i+2] = args[i];
             }
       //only will work with Windows XP
       else if( osName.equals( "Windows XP" ) ) {
                 if (cmd == null) cmd = new String[args.length + 2];
                 cmd[0] = "cmd.exe" ;
                 cmd[1] = "/C" ;

                 for (int i = 0; i<args.length; i++)
                     cmd[i+2] = args[i];
             }
       //only will work with Linux
       else if( osName.equals( "Linux" ) ) {
                 if (cmd == null) cmd = new String[args.length];
                 cmd = args;
             }
       //will work with the rest
             else  {
                 if (cmd == null) cmd = new String[args.length];
                 cmd = args;
             }

             Runtime rt = Runtime.getRuntime();
           // Executes the command
             Process proc = rt.exec(cmd);
             // any error message?
             StreamGobbler errorGobbler = new
                 StreamGobbler(proc.getErrorStream(), "ERROR");

             // any output?
             StreamGobbler outputGobbler = new
                 StreamGobbler(proc.getInputStream(), "OUTPUT");

             // kick them off
             errorGobbler.start();
             outputGobbler.start();

             // any error???
             int exitVal = proc.waitFor();
             System.out.println("ExitValue: " + exitVal);
         } catch (Throwable t) {
             Log.error(t.toString(), t);
           }
     }
 }
*/
