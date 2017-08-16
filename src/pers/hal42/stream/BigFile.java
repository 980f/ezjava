package pers.hal42.stream;


import pers.hal42.lang.Safe;
import pers.hal42.lang.StringX;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.logging.LogLevelEnum;

import java.io.*;

// +++ build a listener system that can get notified of progress

public class BigFile {
  private ErrorLogStream dbg = ErrorLogStream.getForClass(BigFile.class, LogLevelEnum.VERBOSE);
  private String path = "";
  private long size = 0;


  /**
   * Constructor for the BigFile object
   *
   * @param path the base path to the file to split or join.
   * @param size the size in kb to split the file into.
   */
  public BigFile(String path, int size) {
    this.path = path;
    this.size = size;
  }

  /**
   * Constructor for the BigFile object
   *
   * @param path Description of Parameter
   */
  public BigFile(String path) {
    this(path, 0);
  }

  /**
   * split the file by lines [the size parameter is the number of lines between
   * splits, not the number of bytes]
   */
  void splitfilelines() {
    File infile = new File(path);

    if (!infile.exists()) {
      dbg.ERROR("No such file:\n" + path);
      return;
    } else if (!infile.canRead()) {
      dbg.ERROR("Cannot read file:\n" + path);
      return;
    }

    long filelength = infile.length();

    FileReader fr = null;
    BufferedReader br = null;
    try {
      fr = new FileReader(infile);
      br = new BufferedReader(fr);
    } catch (Exception ex) {
      dbg.ERROR("File not found:\n" + path);
      return;
    }

    if (filelength < size) {
      dbg.VERBOSE("Splitting unnecessary.\nFile is less than\n" + (size / 1024) + " Kbytes long");
      return;
    } else if (filelength == size) {
      dbg.VERBOSE("Splitting unnecessary.\nFile is " + (size / 1024) + " Kbytes long");
      return;
    }

    long tot = 0;
    long p = 0;
    boolean done = false;
    boolean failed = false;
    File file = new File(path);
    String name = file.getAbsolutePath();
    String root = "";
    String ext = "";
    int pos = name.lastIndexOf(".");
    if (pos > Safe.INVALIDINDEX) {
      root = StringX.left(name, pos);
      ext = StringX.subString(name, pos + 1);
      dbg.ERROR("Split [" + name + "] at " + pos + " to get [" + root + "] and [" + ext + "]");
    }
    while (!done) {
      dbg.VERBOSE(statDisp(tot, filelength));
      try {
        long linecount = 0;
        FileWriter fw = new FileWriter(root + "." + p + "." + ext);
        BufferedWriter bw = new BufferedWriter(fw);
        String line = "";
        while (((line = br.readLine()) != null) && (linecount < size)) {
          bw.write(line); // +++ NOTE that this might convert CRLFs !!!
          bw.newLine();
          tot += line.length();
          linecount++;
        }
        if (line == null) {
          done = true;
        }
        bw.flush();
        fw.flush();
        bw.close();
        fw.close();
      } catch (Exception ex) {
        dbg.Caught(ex);
        failed = true;
        break;
      }
      p++;
    }
    dbg.VERBOSE(statDisp(tot, filelength) + ", " + tot + " bytes, " + p + " files.");
    try {
      br.close();
      fr.close();
    } catch (Exception s) {
      // +++ bitch
    }
    dbg.VERBOSE("Done... split into " + p + " files.\nTotal " + tot + " bytes");
    if (failed) {
      dbg.ERROR("May have failed!");
    }
  }


  /**
   * split the file by size
   *
   * @deprecated broken:needs to wait on stream completion
   */
  void splitfile() {
    File infile = new File(path);
    if (!infile.exists()) {
      dbg.ERROR("No such file:\n" + path);
      return;
    }
    if (!infile.canRead()) {
      dbg.ERROR("Cannot read file:\n" + path);
      return;
    }

    long filelength = infile.length();

    try (FileInputStream is = new FileInputStream(infile)) {
      long tot = 0;
      long p = 0;
      while (tot < filelength) {
        dbg.VERBOSE(statDisp(tot, filelength));
        long sz = ((filelength - tot) < size) ? (int) (filelength - tot) : size;
        try {
          FileOutputStream os = new FileOutputStream(path + "." + p);
          Streamer streamer = Streamer.Buffered(is, os, 100000, sz, null, false);
//todo:0 streamer.join
          long rd = streamer.count;
          if (rd < sz) {
            dbg.ERROR("Only " + rd + " bytes transferred instead of the " + sz + " expected!");
          }
          tot += rd;
          os.close();
        } catch (Exception ex) {
          dbg.Caught(ex);
          return;
        }
        p++;
      }
      dbg.VERBOSE(statDisp(tot, filelength) + ", " + tot + " bytes, " + p + " files.");
      dbg.VERBOSE("Done... split into " + p + " files.\nTotal " + tot + " bytes");
    } catch (Exception ex) {
      dbg.ERROR("File not found:\n" + path);
    }
  }


  /**
   */
  void joinfile() {

    int p = 0;
    int rd = 0;
    int dotindex = 0;
    byte temp[] = null;
    long inner = 0;
    long filelen = 0;
    long oldfilelen = 0;
    long total = 0;
    FileOutputStream os = null;
    FileInputStream is = null;
    File infile = null;
    File test = null;

    dotindex = path.lastIndexOf(".");

    if (dotindex == -1) {
      dbg.ERROR("Not a valid JavaSplit file.");
      return;
    }
    String name = path.substring(0, dotindex);

    if (!StringX.isInteger(path.substring(dotindex + 1, path.length()))) {
      dbg.ERROR("Not a valid JavaSplit file.");
      return;
    }

    test = new File(path);

    if (!test.exists()) {
      dbg.ERROR("No such file:\n" + path);
      return;
    } else if (!test.canRead()) {
      dbg.ERROR("Cannot read file:\n" + path);
      return;
    }

    test = new File(name);

    if (test.exists()) {
      dbg.WARNING("The target file already exists.\nPlease move the file " + name + "\nTo another directory.");
      return;
    }

    try {
      os = new FileOutputStream(name);
    } catch (Exception ex) {
      dbg.Caught(ex);
      return;
    }

    while (true) {
      try {
        IOX.Close(is);
        infile = new File(name + "." + p);
        is = new FileInputStream(infile);
        p++;
      } catch (Exception ex) {
        //setMessage(  "Done... joined " + p + " files" );
        dbg.VERBOSE("Done... joined " + p + " files.\nTotal " + total + " bytes");
        break;
      }

      filelen = infile.length();

      if (filelen > oldfilelen || temp == null) {
        temp = new byte[(int) filelen];
      }
      oldfilelen = filelen;
      try {
        inner = 0;
        while (inner < filelen) {
          rd = is.read(temp);
          total += rd;
          inner += rd;
          os.write(temp, 0, rd);
          //setMessage(  "Reading file " + p  );
          temp = null;
        }
      } catch (Exception ex) {
        dbg.Caught(ex);
        break;
      }
    }
    IOX.Close(os);
  }


  /**
   * main is the main method to call
   *
   * @param args filename [blocksize [-lines]]
   */
  public static void main(String args[]) {
// use the setall    ErrorLogStream.cpf.myLevel.setLevel(ErrorLogStream.VERBOSE);
    switch (args.length) {
    default:
    case 0: {
      System.out.println("BigFile filename [size [-lines]]");
    }
    break;
    case 1: {
      //only filename means to join
      BigFile fa = new BigFile(args[0]);
      fa.joinfile();
    }
    break;
    case 3: {
      // split by size or lines
      if (StringX.equalStrings("-lines", args[2])) {
        BigFile fa = new BigFile(args[0], Integer.parseInt(args[1]));
        fa.splitfilelines();
        break;
      }
    } // else fall through to case 2
    case 2: {
      // filesize means to split
      BigFile fa = new BigFile(args[0], Integer.parseInt(args[1]));
      fa.splitfile();
    }
    break;
    }
  }


  /**
   * Status Display
   *
   * @param total      Total transferred this far
   * @param filelength File length
   * @return Returns a status display String like: 45%
   */
  private static String statDisp(long total, long filelength) {
    return "" + Math.round((100 * (total / (double) filelength))) + " %";
  }
}
