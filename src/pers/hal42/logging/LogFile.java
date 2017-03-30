package pers.hal42.util;

import pers.hal42.lang.Monitor;
import pers.hal42.logging.ErrorLogStream;

import java.io.*;
import java.util.*;

public class LogFile extends Thread implements AtExit, Comparable  {

  private StringFIFO bafifo = new StringFIFO(); // for buffering output
  private String name = "";
  private PrintStream ps = null;

  public PrintStream backupStream = System.out;
  public static final FileZipperList list = new FileZipperList();
  public static final long DEFAULTMAXFILELENGTH = 1048576L * 10L; /* 10 MB */
  public long maxFileLength = DEFAULTMAXFILELENGTH;
  public boolean perDay = false;
  public static final long DEFAULTMAXQUEUELENGTH = 5000; /* 5 secs */
  public long queueMaxageMS = DEFAULTMAXQUEUELENGTH;

  private static final Counter counter = new Counter();

  public static final String DEFAULTPATH = OS.TempRoot();
  private static String defaultPath = DEFAULTPATH;
  // presume only used one time by one thread
  public static boolean setPath(String defaultPath) {
    // +++ check to make sure the path is good.  If not, find one that is or create it!
    LogFile.defaultPath = defaultPath;
    return true;
  }
  public static String getPath() {
    return defaultPath;
  }

  public void finalize() {
    internalFlush();
    close();
  }

  // static list stuff
  private static final WeakSet lflist = new WeakSet();
  private static final Monitor listMonitor = new Monitor(LogFile.class.getName()+"List");
  public static final LogFile [] listAll() {
    Vector v = new Vector(); // for sorting
    LogFile [] sortedList = new LogFile[0];
    try {
      listMonitor.getMonitor();
      for(Iterator i = lflist.iterator(); i.hasNext();) {
        v.add(i.next());
      }
      Collections.sort(v);
      sortedList = new LogFile[v.size()];
      v.toArray(sortedList);
    } catch (ConcurrentModificationException cme) {
      ErrorLogStream.Debug.Caught(cme);
      //dbg.Caught(cme);
    } catch (Exception e) {
      ErrorLogStream.Debug.Caught(e);
      //dbg.Caught(e);
    } finally {
      listMonitor.freeMonitor();
      return sortedList;
    }
  }

  private static final void register(LogFile logFile) {
    try {
      listMonitor.getMonitor();
      lflist.add(logFile);
    } catch (Exception e) {
      ErrorLogStream.Debug.Caught(e);
      //dbg.Caught(e);
    } finally {
      listMonitor.freeMonitor();
    }
  }

  public int compareTo(Object o) {
    int i = 0;
    try {
      i = getName().compareTo(((LogFile)o).getName());
    } catch (Exception e) {
      /// +++ bitch
    }
    return i;
  }

  /**
   * Note that the filename should be a prefix only (no path, no extension; eg: "sinet").
   * Any path, 'uniquifier' and the ".log" will be applied automatically
   * eg: passing it "myprogram" results in:
   *     "/tmp/"+"myprogram" + (new Date()) + ".log"
   */
  public LogFile(String filename, boolean overwrite, PrintStream backupPrintStream, long maxFileLength, long queueMaxageMS, boolean perDay) {
    super(LogFile.class.getName()+"_"+counter.incr());
    ps = bafifo.getPrintStream();
    this.filename = new File(defaultPath, filename).getAbsolutePath();
    this.overwrite = overwrite;
    this.maxFileLength = maxFileLength;
    this.perDay = perDay;
    this.queueMaxageMS = queueMaxageMS;
    if(backupPrintStream != null) {
      this.backupStream = backupPrintStream;
    }
    reader = bafifo.getBufferedReader();
    register(this);
    setDaemon(true);
    OnExit(this);
    start();
  }

  /**
   * Defaults perDay to FALSE (only rolls over based on size)
   */
  public LogFile(String filename, boolean overwrite, PrintStream backupPrintStream, long maxFileLength, long queueMaxageMS) {
    this(filename, overwrite, backupPrintStream, maxFileLength, queueMaxageMS, false);
  }

  /**
   * Defaults queueMaxageMS to DEFAULTMAXQUEUELENGTH (5s).
   */
  public LogFile(String filename, boolean overwrite, PrintStream backupPrintStream, long maxFileLength) {
    this(filename, overwrite, backupPrintStream, maxFileLength, DEFAULTMAXQUEUELENGTH);
  }

  /**
   * Defaults maxFileLength to DEFAULTMAXFILELENGTH (10MB).
   */
  public LogFile(String filename, boolean overwrite, PrintStream backupPrintStream) {
    this(filename, overwrite, backupPrintStream, DEFAULTMAXFILELENGTH);
  }

  /**
   * Defaults to using System.out as a backup printstream
   */
  public LogFile(String filename, boolean overwrite) {
    this(filename, overwrite, null);
  }

  protected static final Monitor logFileMon = new Monitor(LogFile.class.getName() + "_PrintForkCreator");
  protected PrintFork pf = null;
  public PrintFork getPrintFork() {
    return getPrintFork(ErrorLogStream.WARNING);
  }
  public PrintFork getPrintFork(int defaultLevel) {
    return getPrintFork(filename, defaultLevel);
  }
  public PrintFork getPrintFork(String name) {
    return getPrintFork(name, ErrorLogStream.WARNING);
  }
  public PrintFork getPrintFork(String name, int defaultLevel) {
    return getPrintFork(name, defaultLevel, false /* don't register! */);
  }
  public PrintFork getPrintFork(int defaultLevel, boolean register) {
    return getPrintFork(filename, defaultLevel, register);
  }
  public PrintFork getPrintFork(String name, int defaultLevel, boolean register) {
    try {
      logFileMon.getMonitor();
      if(pf == null) {
        pf = new LogFilePrintFork(name, getPrintStream(), defaultLevel, register);
      }
    } catch (Exception e) {
      // ??? +++
    } finally {
      logFileMon.freeMonitor();
      return pf;
    }
  }

  public static final PrintFork makePrintFork(String filename, boolean compressed) {
    return makePrintFork(filename, ErrorLogStream.WARNING, compressed);
  }

  public static final PrintFork makePrintFork(String filename, int defaultLevel, boolean compressed) {
    LogFile fpf = null;
    PrintFork mypf=null;
    try {
      fpf = new LogFile(filename, false);// ==append
      if(fpf != null) {
        mypf=fpf.getPrintFork(defaultLevel);
      } else {
        mypf=new PrintFork(filename, new PrintStream(new NullOutputStream())) ; // a dummy to prevent null pointer exceptions
      }
    } catch (Exception e) {
      // +++ bitch
    } finally {
      return mypf;
    }
  }

  public static final void flushAll() {
    LogFile[] list = listAll();
    for(int i = list.length; i-->0;) {
      list[i].AtExit();
    }
  }

  public void flush() {
    flushHint = true;
  }

  public PrintStream getPrintStream() {
    return ps;
  }

  private String filename = null;
  private String longFilename = null;
  private File file = null;
  private FileOutputStream fos = null;
  private BufferedReader reader = null;
  private PrintWriter pw = null;
  private boolean overwrite = false;

  public boolean flushHint = false;

  private Calendar compCal = Calendar.getInstance();

  public static final long allPending() {
    LogFile [] lfs = listAll();
    long counter = 0;
    for(int i = lfs.length; i-->0;) {
      counter += lfs[i].pending();
    }
    return counter;
  }

  public long pending() {
    return bafifo.Size();
  }

  public String filename() {
    return longFilename;
  }

  public String status() {
    return "" /* handles nulls */ + filename() + " [" + pending() + /*" lines pending" + */ "]";
  }

  private boolean keepRunning;
  private boolean done = false;

  public void AtExit(){
    // need to flush, then suicide.
    keepRunning=false;
    this.interrupt();
    try {
      this.join(); // waits for the thread to die //+_+ needs timeout
    } catch (Exception e) {
      // stub
    }
    //should flush then die without further commands.
    //internalFlush(); // --- testing
    close();
    done = true;
    list.cleanup();
  }

  public boolean IsDown() {
    return done && (list.cleanup()==0);
  }

  public void run() {
    keepRunning = true;
    int day = -1;
    setPriority(Thread.MIN_PRIORITY);
    while (keepRunning) {
      try {
        // these next few lines were at the bottom, but need to be here
        Thread.yield();
        ThreadX.sleepFor(10);  // --- testing to be sure that we give the OS time to do its stuff
        // check to see if the hour just rolled over or if the file length is too long
        compCal.setTime(Safe.Now());
        if((file != null) && ((file.length() > maxFileLength) || (perDay && (compCal.get(Calendar.DAY_OF_YEAR) != day)))) {
          close();
        }
        try {
          // if the file isn't opened, open it
          if(file == null) {
            day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
            longFilename = filename+".log"; // +++ put ".log" somewhere where we can set it per-log
            if(!overwrite) {
              file = new File(longFilename);
              if(file.exists()) {
                String newFilename = filename+Safe.timeStampNow()+".log"; // +++ put ".log" somewhere where we can set it per-log
                if(file.renameTo(new File(newFilename))) {
                  list.add(FileZipper.backgroundZipFile(newFilename));
                }
              }
            }
            fos = new FileOutputStream(longFilename, !overwrite);
            pw = new PrintWriter(fos);
          }
        } catch (Exception e) {
          backupStream.println("Exception opening log file: " + e);
        }
        try {
          if(pw!=null){
            // do one line at a time, unless ...
            String lr = reader.readLine();
            if(lr !=null) {
              pw.println(lr);
              writes.add(lr.length());
            }
            // unless it is time to do more ...
            if (((System.currentTimeMillis() - bafifo.lastWrite) > queueMaxageMS) || flushHint) {
              // +++ bump the thread priority and flush the buffer
              Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
              // This record goes with the time out of sync, hope noone will notice
              //pw.println("!! Log queue age limit exceeded, flushing ... !!");
              internalFlush();
              bafifo.lastWrite = System.currentTimeMillis();
              flushHint = false;
              if(keepRunning && (queueMaxageMS > 0)) {
                Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
              }
            }
          }
        } catch (Exception ex) {
          backupStream.println("Exception logging: " + ex);
          String temp = null;
          try {
            temp = reader.readLine();
          } catch (Exception e2) {
            backupStream.println("Exception reading log buffer: " + e2);
          }
          if(temp != null) {
            pw.println(temp);
            pw.println("Logger thread excepted, flushing...");
            pw.println(ex);
            internalFlush();
            pw.println("Flushed, logging stopped.");
          }
        }
      } catch (Exception ez) {
        // this is mostly for catching interruptedExceptions and looping
        Thread.interrupted(); // clears interrupted bits
        continue;
      }
    }
    backupStream.println("LogFile:"+filename+" leaving run loop (keepRunning="+keepRunning+").");
  }

  private void close() {
    try {
      // close it all up!
      internalFlush();// --- testing
      if(pw != null) {
        pw.flush();
        pw.close();
        pw = null;
      }
      if(fos!= null) {
        fos.flush();
        fos.close();
        fos = null;
      }
      if(file != null) {
        file = null;
      }
    } catch (Exception e) {
      backupStream.println("Exception closing log file: " + e);
    }
  }

  public static final Accumulator writes = new Accumulator();
  public static final Accumulator writeTimes = new Accumulator();

  /**
   * Flush the log record queue.
   */
  public void internalFlush() {
    String next = null;
    StopWatch sw = new StopWatch();
    do {
      next = null;
      try {
        next = reader.readLine();
      } catch (Exception e) {
        backupStream.println("Exception flushing/reading: " + e);
      }
      if (next != null) {
        try {
          pw.println(next);
          writes.add(next.length());
        } catch (Exception e) {
          backupStream.println("Exception flushing/writing: " + e);
        }
      }
    } while (next != null);
    try {
      pw.flush();
      fos.flush();
    } catch (Exception e) {
      backupStream.println("Exception flushing/flushing (pw"+(pw==null?"=":"!")+"=null) for '" + filename + "': " + e);
      e.printStackTrace(backupStream);
    } finally {
      writeTimes.add(sw.Stop());  // counts how long each flush takes
    }
  }

}

class FileZipperList extends Vector {
  public void add(FileZipper fz) {
    // first, add it, then look for stale ones
    super.insertElementAt(fz,0);
    cleanup();
  }
  public FileZipper itemAt(int i) {
    return (FileZipper)elementAt(i);
  }
  public int cleanup() {
    for(int i = size(); i-->0;) {
      FileZipper fz = itemAt(i);
      if(fz.IsDown()) {
        remove(i);
      }
    }
    return size();
  }
}

class LogFilePrintFork extends PrintFork {
  public LogFilePrintFork(String name, PrintStream primary, int startLevel, boolean register) {
    super(name, primary, startLevel, register);
  }

  public LogFilePrintFork(String name, PrintStream primary, int startLevel) {
    super(name, primary, startLevel);
  }

  public LogFilePrintFork(String name, PrintStream primary) {
    super(name, primary);
  }


  public void println(String s, int printLevel){
    if(!registered) { //  if it is registered, it will be getting the header already.
     String prefix = Safe.timeStampNow() + LogSwitch.letter(printLevel) + Thread.currentThread() + ":";
      super.println(prefix + s, printLevel);
    } else {
      super.println(s, printLevel);
    }
  }
}
