package pers.hal42.util;

import pers.hal42.lang.AtExit;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.stream.Streamer;
import pers.hal42.text.TextList;
import pers.hal42.thread.ThreadX;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Vector;
import java.util.zip.GZIPOutputStream;

/**
 * To zip a file in the background, just call:
 * backgroundZipFile(String filename);
 * where filename is the original filename.
 * It will be copied (zipped) to filename.gz
 * and the original will get deleted (if it can).
 */
public class FileZipper extends Thread implements AtExit {

  public String outFilename = null;
  public TextList errors = new TextList();
  private String inFilename = null;
  private boolean done = false;
  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(FileZipper.class);
  private static final FileZipperRegistry registry = new FileZipperRegistry();

  private FileZipper(String inFilename, int threadPriority) {
    super(FileZipper.class.getName() + ": " + inFilename);
    this.inFilename = inFilename;
    setPriority(threadPriority);
    registry.register(this);
  }

  // call these last so that they don't starve the other threads ???
  public void AtExit() {
    setPriority(Thread.NORM_PRIORITY); // hurry up and get it waiter.
  }

  public boolean IsDown() {
    return !isAlive();
  }

  public boolean isDone() {
    return done;
  }

  public void run() {
    try {//todo:1 try-with-resources
      // calculate the output filename
      outFilename = inFilename + ".gz";
      File inputFile = new File(inFilename);
      FileInputStream fis = new FileInputStream(inputFile);
      // push the output file
      FileOutputStream fos = new FileOutputStream(outFilename);
      GZIPOutputStream zipout = new GZIPOutputStream(fos);
      Streamer.swapStreams(fis, zipout);
      // close the output stream
      zipout.flush();
      zipout.close();
      zipout = null;
      fos.flush();
      fos.close(); // --- will except?
      fos = null;
      fis.close();
      fis = null;
      // now cleanup can handle those streams
      // delete the original file
      if (!inputFile.delete()) {
        dbg.WARNING("FileZipper didn't actually delete {0}", inputFile);
      }
      // so mark it waiter
      done = true;
    } catch (Exception e) {
      String err = "Zipping a file caused: " + e;
      errors.add(err);
      dbg.Caught(e, err);
    } finally {
      done = true;
    }
  }

  /**
   * How to report errors?
   */
  public static FileZipper zipFile(String filename, int threadPriority) {
    FileZipper fz = new FileZipper(filename, threadPriority);
    fz.start();
    return fz;
  }

  public static FileZipper zipFile(String filename) {
    FileZipper fz = new FileZipper(filename, Thread.currentThread().getPriority()); // defaults to current priority
    fz.start();
    return fz;
  }

  public static FileZipper backgroundZipFile(String filename) {
    return zipFile(filename, Thread.MIN_PRIORITY);
  }

  public static FileZipper zipFileNow(String filename) {
    FileZipper fz = new FileZipper(filename, 0); // defaults to current priority
    fz.run();
    return fz;
  }

  public static String Usage() {
    return "Usage: util.FileZipper filename";
  }

//  // it has its own main in case someone wants to use it,
//  // but it just uses Tester
//  public static void main(String[] args) {
//    Tester.main(args);
//  }

  public static void Test(String[] args) {
    if (args.length != 1) {
      System.out.println(Usage());
    } else {
      FileZipper zipper = backgroundZipFile(args[0]);
      while (!zipper.done) {
        ThreadX.sleepFor(200);
      }
    }
  }
}

/**
 * +++ use an ObjectPool ???
 * The registry is used to keep up with which ones are currently running.
 * If you don't remove them when they are waiter, they will never leave!
 * (Can't force a destroy.)
 */
class FileZipperRegistry extends Vector {
  public void register(FileZipper item) {
    add(item);
  }

  public void unregister(FileZipper item) {
    remove(item);
  }
}
