package pers.hal42.logging;

import org.jetbrains.annotations.NotNull;
import pers.hal42.lang.*;
import pers.hal42.math.Accumulator;
import pers.hal42.stream.StringFIFO;
import pers.hal42.thread.Counter;
import pers.hal42.thread.ThreadX;
import pers.hal42.timer.StopWatch;
import pers.hal42.util.FileZipper;
import pers.hal42.util.PrintFork;

import java.io.*;
import java.util.Calendar;
import java.util.Vector;

import static java.util.Collections.sort;

public class LogFile extends Thread implements AtExit, Comparable<LogFile> {

  public PrintStream backupStream = System.out;
  public long maxFileLength = DEFAULTMAXFILELENGTH;
  public boolean perDay = false;
  public long queueMaxageMS = DEFAULTMAXQUEUELENGTH;
  public boolean flushHint = false;
  protected PrintFork pf = null;
  private int day = -1;
  private StringFIFO bafifo = new StringFIFO(); // for buffering output
  private String name = "";
  private PrintStream ps = null;
  private String filebase = null;
  private String longFilename = null;
  private File file = null;
  private FileOutputStream fos = null;
  private BufferedReader reader = null;
  private PrintWriter pw = null;
  private boolean overwrite = false;
  private Calendar compCal = Calendar.getInstance();
  private boolean keepRunning;
  private boolean done = false;
  public static final FileZipperList list = new FileZipperList();
  public static final long DEFAULTMAXFILELENGTH = 1048576L * 10L; /* 10 MB */
  public static final long DEFAULTMAXQUEUELENGTH = 5000; /* 5 secs */
  public static final String DEFAULTPATH = OS.TempRoot();
  private static String defaultPath = DEFAULTPATH;
  public static final Accumulator writes = new Accumulator();
  public static final Accumulator writeTimes = new Accumulator();
  public final static String DotLog = ".log";
  protected static final Monitor logFileMon = new Monitor(LogFile.class.getName() + "_PrintForkCreator");
  private static final Counter counter = new Counter();
  // static list stuff
  private static final WeakSet<LogFile> lflist = new WeakSet<>();
  private static final Monitor listMonitor = new Monitor(LogFile.class.getName() + "List");

  /**
   * Note that the filebase should be a prefix only (no path, no extension; eg: "sinet").
   * Any path, 'uniquifier' and the ".log" will be applied automatically
   * eg: passing it "myprogram" results in:
   * "/tmp/"+"myprogram" + (new Date()) + ".log"
   */
  public LogFile(String filename, boolean overwrite, PrintStream backupPrintStream, long maxFileLength, long queueMaxageMS, boolean perDay) {
    super(LogFile.class.getName() + "_" + counter.incr());
    ps = bafifo.getPrintStream();

    if (filename.endsWith(DotLog)) {
      filename = filename.substring(0, filename.length() - DotLog.length());
    }

    this.filebase = new File(defaultPath, filename).getAbsolutePath();
    this.overwrite = overwrite;
    this.maxFileLength = maxFileLength;
    this.perDay = perDay;
    this.queueMaxageMS = queueMaxageMS;
    if (backupPrintStream != null) {
      this.backupStream = backupPrintStream;
    }
    reader = bafifo.getBufferedReader();
    try {
      beOpen();//close timing hole between invoking start() and first lines logged.
    } catch (FileNotFoundException e) {
      e.printStackTrace();//can't use ErrorLogStream, too semi-circular (it is not fully initialized when we get here)
    }
    register(this);
    setDaemon(true);
    //todo:00 Main.OnExit(this);
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

  public void finalize() {
    internalFlush();
    close();
  }

  @Override
  public int compareTo(@NotNull LogFile logFile) {
    try {
      return getName().compareTo(logFile.getName());
    } catch (Exception e) {
      return 0;
    }
  }

  public PrintFork getPrintFork() {
    return getPrintFork(LogLevelEnum.WARNING);
  }

  public PrintFork getPrintFork(LogLevelEnum defaultLevel) {
    return getPrintFork(filebase, defaultLevel);
  }

  public PrintFork getPrintFork(int defaultLevel) {
    return getPrintFork(filebase, defaultLevel);
  }

  public PrintFork getPrintFork(String name) {
    return getPrintFork(name, LogLevelEnum.WARNING);
  }

  public PrintFork getPrintFork(String name, LogLevelEnum defaultLevel) {
    return getPrintFork(name, defaultLevel, false /* don't register! */);
  }

  public PrintFork getPrintFork(String name, int defaultLevel) {
    return getPrintFork(name, defaultLevel, false /* don't register! */);
  }

  public PrintFork getPrintFork(String name, int defaultLevel, boolean register) {
    try (Monitor free = logFileMon.getMonitor()) {
      if (pf == null) {
        pf = new LogFilePrintFork(name, getPrintStream(), defaultLevel, register);
      }
      return pf;
    }
  }

  public PrintFork getPrintFork(String name, LogLevelEnum defaultLevel, boolean register) {
    return getPrintFork(name, defaultLevel.level, register);
  }

  public PrintFork getPrintFork(int defaultLevel, boolean register) {
    return getPrintFork(filebase, defaultLevel, register);
  }

  public PrintFork getPrintFork(LogLevelEnum defaultLevel, boolean register) {
    return getPrintFork(filebase, defaultLevel.level, register);
  }

  public void flush() {
    flushHint = true;
  }

  public PrintStream getPrintStream() {
    return ps;
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

  public void AtExit() {
    // need to flush, then suicide.
    keepRunning = false;
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
    return done && (list.cleanup() == 0);
  }

  public void run() {
    keepRunning = true;
    setPriority(Thread.MIN_PRIORITY);
    while (keepRunning) {
      try {
        Thread.yield();
        ThreadX.sleepFor(10);  // --- testing to be sure that we give the OS time to do its stuff
        // check to see if the hour just rolled over or if the file length is too long
        compCal.setTime(DateX.Now());
        //noinspection MagicConstant   //intellij bug, DayOfYear is not a day of the week.
        if ((file != null) && ((file.length() > maxFileLength) || (perDay && (compCal.get(Calendar.DAY_OF_YEAR) != day)))) {
          close();
        }
        try {
          // if the file isn't opened, push it
          beOpen();
        } catch (Exception e) {
          backupStream.println("Exception opening log file: " + e);
        }
        try {
          if (pw != null) {
            // do one line at a time, unless ...
            String lr = reader.readLine();
            if (lr != null) {
              pw.println(lr);
              writes.add(lr.length());
            }
            // unless it is time to do more ...
            if (((System.currentTimeMillis() - bafifo.lastWrite) > queueMaxageMS) || flushHint) {
              // bump the thread priority and flush the buffer
              Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
              internalFlush();
              bafifo.lastWrite = System.currentTimeMillis();
              flushHint = false;
              if (keepRunning && (queueMaxageMS > 0)) {
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
          if (temp != null) {
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
      }
    }
    backupStream.println("LogFile:" + filebase + " leaving run loop (keepRunning=" + keepRunning + ").");
  }

  protected void beOpen() throws FileNotFoundException {
    if (file == null) {
      day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
      longFilename = filebase + DotLog;
      if (!overwrite) {
        file = new File(longFilename);
        if (file.exists()) {
          String newFilename = filebase + DateX.timeStampNow() + DotLog;
          if (file.renameTo(new File(newFilename))) {
            list.add(FileZipper.backgroundZipFile(newFilename));
          }
        }
      }
      fos = new FileOutputStream(longFilename, !overwrite);
      pw = new PrintWriter(fos);
    }
  }

  private void close() {
    try {
      internalFlush();
      //# don't use IOX.Close, to avoid circular package dependencies
      if (pw != null) {
        pw.flush();
        pw.close();
        pw = null;
      }
      if (fos != null) {
        fos.flush();
        fos.close();
        fos = null;
      }
      if (file != null) {
        file = null;
      }
    } catch (Exception e) {
      backupStream.println("Exception closing log file: " + e);
    }
  }

  /**
   * Flush the log recordType queue.
   */
  public void internalFlush() {
    StopWatch sw = new StopWatch();
    String next = null;
    do {
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
      backupStream.println("Exception flushing/flushing (pw" + (pw == null ? "=" : "!") + "=null) for '" + filebase + "': " + e);
      e.printStackTrace(backupStream);
    } finally {
      writeTimes.add(sw.Stop());  // counts how long each flush takes
    }
  }


  // presume only used one time by one thread
  public static boolean setPath(String defaultPath) {
    // +++ check to make sure the path is good.  If not, find one that is or create it!
    LogFile.defaultPath = defaultPath;
    return true;
  }

  public static String getPath() {
    return defaultPath;
  }

  public static LogFile[] listAll() {
    Vector<LogFile> v = new Vector<>(); // for sorting
    LogFile[] sortedList = new LogFile[0];
    //noinspection TryWithIdenticalCatches
    try (Monitor free = listMonitor.getMonitor()) {
      v.addAll(lflist);
      sort(v);
      sortedList = new LogFile[v.size()];
      v.toArray(sortedList);
      return sortedList;
    }
  }

  private static void register(LogFile logFile) {
    try (Monitor free = listMonitor.getMonitor()) {
      lflist.add(logFile);
    }
  }

  public static PrintFork makePrintFork(String filename, boolean compressed) {
    return makePrintFork(filename, LogLevelEnum.WARNING.level, compressed);
  }

  public static PrintFork makePrintFork(String filename, int defaultLevel, boolean compressed) {
    try {
      LogFile fpf = new LogFile(filename, false);
      return fpf.getPrintFork(defaultLevel);
    } catch (Exception e) {
      e.printStackTrace();//# probably can't use ErrorLogStream as this gets called during setting that up.
      return null;
    }
  }

  public static void flushAll() {
    for (LogFile logFile : listAll()) {
      logFile.AtExit();
    }
  }

  public static long allPending() {
    long counter = 0;
    for (LogFile logFile : listAll()) {
      counter += logFile.pending();
    }
    return counter;
  }

}

class FileZipperList extends Vector<FileZipper> {
  public boolean add(FileZipper fz) {
    // first, add it, then look for stale ones
    super.insertElementAt(fz, 0);
    cleanup();
    return true;
  }

  public FileZipper itemAt(int i) {
    return elementAt(i);
  }

  public int cleanup() {
    removeIf(FileZipper::IsDown);
    return size();
  }
}

class LogFilePrintFork extends PrintFork {
  public LogFilePrintFork(String name, PrintStream primary, int startLevel, boolean register) {
    super(name, primary, startLevel, register);
    onStart();
  }
  public LogFilePrintFork(String name, PrintStream primary, int startLevel) {
    super(name, primary, startLevel);
    onStart();
  }

  public LogFilePrintFork(String name, PrintStream primary) {
    super(name, primary);
    onStart();
  }

  void onStart() {

  }

  public void println(String s, int printLevel) {
    if (!registered) { //  if it is registered, it will be getting the header already.
      String prefix = DateX.timeStampNow() + LogSwitch.letter(printLevel) + Thread.currentThread() + ":";
      super.println(prefix + s, printLevel);
    } else {
      super.println(s, printLevel);
    }
  }
}
