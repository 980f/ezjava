package pers.hal42.stream;

import pers.hal42.lang.*;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.util.Counter;
import pers.hal42.util.PrintFork;

import java.io.*;
import java.util.*;

public class LogFile implements AtExit, Comparable {

  private StringFIFO bafifo = new StringFIFO(); // for buffering output
  private String name = "";
  private PrintStream ps = null;

  public static PrintStream defaultBackupStream = System.out;
  public PrintStream backupStream = defaultBackupStream;
  public static final FileZipperList list = new FileZipperList();
  public static final long DEFAULTMAXFILELENGTH = 1048576L * 10L; /* 10 MB */
  public long maxFileLength = DEFAULTMAXFILELENGTH;
  public boolean perDay = false;
  public static final long DEFAULTMAXQUEUELENGTH = 5000; /* 5 secs */
  public long queueMaxageMS = DEFAULTMAXQUEUELENGTH;

  private static final Counter counter = new Counter();

  public static final String DEFAULTPATH = OS.TempRoot();
  public static final String DEFAULTARCHIVEPATH = OS.TempRoot()+"/backups/";
  private static String defaultPath = DEFAULTPATH;
  private static String defaultArchivePath = DEFAULTARCHIVEPATH;

  public static synchronized boolean setDefaultPaths() {
    return setPath(DEFAULTPATH, DEFAULTARCHIVEPATH);
  }

  // presume only used one time by one thread, but after the one time at loadtime
  public static synchronized boolean setPath(String defaultPath, String defaultArchivePath) {
    // +++ check to make sure the path is good.  If not, find one that is or create it!
    LogFile.defaultPath = defaultPath;
    LogFile.defaultArchivePath = defaultArchivePath;
    File f = new File(defaultPath);
    IOX.createDirs(f);
    f = new File(defaultArchivePath);
    IOX.createDirs(f);
    return true;
  }

  public static String getPath() {
    return defaultPath;
  }

  public static String getArchivePath() {
    return defaultPath;
  }

  public void finalize() {
    internalFlush();
    close();
  }

  // static list stuff
  private static final WeakSet lflist = new WeakSet();
  private static final Monitor listMonitor = new Monitor(LogFile.class.getName()+"List");
  public static int lflistSize() {
    return lflist.size();
  }
  public static LogFile [] listAll() {
    Vector v = new Vector(); // for sorting
    LogFile [] sortedList = new LogFile[0];
    try {
      listMonitor.getMonitor();
      for(Iterator i = lflist.iterator(); i.hasNext();) {
        Object o = i.next();
        v.add(o);
      }
      try {
        Collections.sort(v);
        sortedList = new LogFile[v.size()];
        v.toArray(sortedList);
      } catch(ClassCastException cce) {
        backupLogException("listAll2", cce);
      }
    } catch (ConcurrentModificationException cme) {
      backupLogException("listAll", cme);
    } catch (Exception e) {
      backupLogException("listAll3", e);
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
      backupLogException("registering", e);
    } finally {
      listMonitor.freeMonitor();
    }
  }

  public int compareTo(Object o) {
    int i = 0;
    try {
      i = StringX.compareStrings(filename, ((LogFile)o).filename);
    } catch (Exception e) {
      backupLogException("compareTo", e);
    }
    return i;
  }

  public static void backupLogException(String s, Throwable e) {
    PrintStream localps = defaultBackupStream;
    if(localps != null) {
      localps.println(""+s+": "+e);
      if(e != null) {
        e.printStackTrace(localps);
      }
    }
  }

  /**
   * Note that the filename should be a prefix only (no path, no extension; eg: "sinet").
   * Any path, 'uniquifier' and the ".log" will be applied automatically
   * eg: passing it "myprogram" results in:
   *     "c:\paymate.tmp\"+"myprogram" + (new .Date()) + ".log"
   */
  public LogFile(String filename, boolean overwrite, PrintStream backupPrintStream, long maxFileLength, long queueMaxageMS, boolean perDay) {
    try {
      ps = bafifo.getPrintStream();
      if(StringX.NonTrivial(filename) && filename.endsWith(dotlog)) {
        filename = StringX.left(filename, filename.length()-dotlog.length());
      }
      this.filename = new File(defaultPath, filename).getAbsolutePath();
      this.overwrite = overwrite;
      this.maxFileLength = maxFileLength;
      this.perDay = perDay;
      this.queueMaxageMS = queueMaxageMS;
      if(backupPrintStream != null) {
        this.backupStream = backupPrintStream;
      }
      reader = bafifo.getBufferedReader();
      openFileIfNeeded(); // gets things started!
      register(this);
      OnExit(this);
    } catch (Exception e) {
      backupLogException("Error creating logstream", e);
    }
  }

  /**
   * Defaults to using System.out (or whatever) as a backup printstream
   */
  public LogFile(String filename, boolean overwrite) {
    this(filename, overwrite, null, DEFAULTMAXFILELENGTH, DEFAULTMAXQUEUELENGTH, false);
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
  public PrintFork getPrintFork(boolean register) {
    return getPrintFork(filename, register);
  }
  public PrintFork getPrintFork(String name, int defaultLevel, boolean register) {
    try {
      logFileMon.getMonitor();
      if(pf == null) {
        pf = new LogFilePrintFork(name, getPrintStream(), defaultLevel, register);
      }
    } catch (Exception e) {
      backupLogException("getPrintFork", e);
    } finally {
      logFileMon.freeMonitor();
      return pf;
    }
  }
  public PrintFork getPrintFork(String name, boolean register) {
    try {
      logFileMon.getMonitor();
      if(pf == null) {
        pf = new LogFilePrintFork(name, getPrintStream(), register);
      }
    } catch (Exception e) {
      backupLogException("getPrintFork2", e);
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
        mypf=PrintFork.New(filename, new PrintStream(new NullOutputStream())) ; // a dummy to prevent null pointer exceptions
      }
    } catch (Exception e) {
      backupLogException("makePrintFork", e);
    } finally {
      return mypf;
    }
  }

  public static final void ExitAll() {
    LogFile[] list = listAll();
    for(int i = list.length; i-->0;) {
      list[i].AtExit();
    }
  }

  public static final void flushAll() {
    LogFile[] list = listAll();
    for(int i = list.length; i-->0;) {
      list[i].flush();
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

  public String name() {
    return ""+longFilename();
  }

  public String longFilename() {
    return longFilename;
  }

  public String status() {
    return StringX.replace(longFilename, defaultPath+"/", "") + " [" + pending() + /*" lines pending" + */ "]";
  }

  private boolean keepRunning = true;
  private boolean done = false;

  // +++ mutex this !!!
  public void AtExit(){
    // need to flush, then suicide.
    keepRunning=false;
    close();  //should flush then die without further commands.
    done = true;
    list.cleanup();
  }

  public boolean IsDown() {
    return done && (list.cleanup()==0);
  }

  private static final String dotlog = ".log";

  private int day = -1;

  public static void runThemAll() {
    try {
      LogFile [ ] list = listAll();
      for(int i = list.length; i-->0;) {
        LogFile lf = list[i];
        try {
          lf.runThisOne();
        } catch (Exception ex) {
          backupLogException("runThemAll["+i+"]", ex);
        }
//        Thread.yield();
      }
    } catch (Exception ex) {
      backupLogException("runThemAll2", ex);
    }
  }

  public void runThisOne() {
    try {
      if(keepRunning) {
        try {
          closeFileIfShould();
          openFileIfNeeded();
          try {
            if (pw != null) {
              flushAllIfShould();
            }
          } catch (Exception ex) {
            backupLogException("Exception logging", ex);
            String temp = null;
            try {
              temp = reader.readLine();
            } catch (Exception e2) {
              backupLogException("Exception reading log buffer: ", e2);
            }
            if (temp != null) {
              pw.println(temp);
              pw.println("Logger thread excepted, flushing...");
              pw.println(ex);
              internalFlush();
              pw.println("Flushed, logging stopped.");
            }
          }
        } catch (Exception ez) {
          backupLogException("Exception logging", ez);
        }
      }
    } catch (Throwable t) {
      backupLogException("LogFile: " + filename + " had throwable in runloop!: ", t);
    }
  }

  private void flushAllIfShould() {
    internalFlush();
    bafifo.lastWrite = DateX.utcNow();
    flushHint = false;
  }

  // if the file isn't opened, open it
  private void openFileIfNeeded() {
    try {
      if (file == null) {
        day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        longFilename = filename + dotlog; // +++ put ".log" somewhere where we can set it per-log
        if (!overwrite) {
          file = new File(longFilename);
          if (file.exists()) {
            String newFilename =
                StringX.replace(filename, defaultPath + "/",
                                defaultArchivePath + "/") +
                DateX.timeStampNow() + dotlog; // +++ put ".log" somewhere where we can set it per-log
            if (file.renameTo(new File(newFilename))) { // +++ cause it to move to a different backup location
              list.add(FileZipper.backgroundZipFile(newFilename));
            }
          }
        }
        fos = new FileOutputStream(longFilename, !overwrite);
        pw = new PrintWriter(fos);
      }
    } catch (Exception e) {
      backupLogException("Exception opening log file: ", e);
    }
  }

  // check to see if the day just rolled over or if the file length is too long
  private void closeFileIfShould() {
    compCal.setTime(DateX.Now());
    if((file != null) && ((file.length() > maxFileLength) || (perDay && (compCal.get(Calendar.DAY_OF_YEAR) != day)))) {
      close();
    }
  }

  private void close() {
    try {
      // close it all up!
      internalFlush();
      if(pw != null) {
        pw.flush();
        pw.close();
        pw = null;
      }
    } catch (Exception e) {
      backupLogException("Exception closing pw: ", e);
    }
    try {
      if(fos!= null) {
        fos.flush();
        IOX.Close(fos);
        fos = null;
      }
    } catch (Exception e) {
      backupLogException("Exception closing fos: ", e);
    }
    try {
      if(file != null) {
        file = null;
      }
    } catch (Exception e) {
      backupLogException("Exception closing log file: ", e);
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
        backupLogException("Exception flushing/reading", e);
      }
      if (next != null) {
        try {
          pw.println(next);
          writes.add(next.length());
        } catch (Exception e) {
          backupLogException("Exception flushing/writing", e);
        }
      }
    } while (next != null);
    try {
      pw.flush();
      IOX.Flush(fos);
    } catch (Exception e) {
      backupLogException("Exception flushing/flushing (pw"+(pw==null?"=":"!")+"=null) for '" + filename + "'", e);
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

  public LogFilePrintFork(String name, PrintStream primary, boolean register) {
    super(name, primary, PrintFork.DEFAULT_LEVEL, register);
  }

  public LogFilePrintFork(String name, PrintStream primary, int startLevel) {
    super(name, primary, startLevel);
  }

  public LogFilePrintFork(String name, PrintStream primary) {
    super(name, primary);
  }


  public void println(String s, int printLevel){
    if(!registered) { //  if it is registered, it will be getting the header already.
     String prefix = DateX.timeStampNowYearless() + LogSwitch.letter(printLevel) + Thread.currentThread() + ":";
      super.println(prefix + s, printLevel);
    } else {
      super.println(s, printLevel);
    }
  }
}

//$Id: LogFile.java,v 1.14 2004/03/13 01:29:31 mattm Exp $
