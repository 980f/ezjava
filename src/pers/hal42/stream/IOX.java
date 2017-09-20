package pers.hal42.stream;

import pers.hal42.lang.DateX;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.text.TextList;
import pers.hal42.timer.UTC;

import java.io.*;
import java.nio.file.*;

/** utilties related to files and stream.  Look in java.nio for newer equivalents */
public class IOX {
  private IOX() {
    // don't construct me; I am for static functions
  }

  public static final ErrorLogStream dbg = ErrorLogStream.getForClass(IOX.class);

  public static boolean makeBackup(String filename) {
    try {
      if (FileExists(filename)) {
        final FileSystem system = FileSystems.getDefault();
        Path path = system.getPath(filename);
        Path bakdir = system.getPath(filename + ".bak");
        createDir(bakdir);
        Path bakfile=system.getPath(bakdir.toString(),Long.toString(System.currentTimeMillis()));
        Files.move(path, bakfile);
        return true;
      } else {
        return true;//no backup need if file does not exist.
      }
    } catch (IOException e) {
      dbg.Caught(e);
      return false;
    }
  }

  /** @returns bytes from file, on any error null */
  public static byte[] readBlob(Path path) {
    try {
      return Files.readAllBytes(path);
    } catch (IOException e) {
      return null;
    }
  }

  public static Exception writeBlob(Path path, byte[] content) {
    IOX.createDir(path.getParent());
    try {
      Files.write(path, content);
      return null;
    } catch (IOException e) {
      return e;
    }
  }

  public static boolean createDir(Path parent) {
    try {
      Files.createDirectories(parent);
      return true;
    }
    catch(FileAlreadyExistsException feh){
      return true;//JFC why do so many people think this is exceptional?
    }
    catch (IOException e) {
      dbg.Caught(e);
      return false;
    }
  }

  public static File[] listFiles(File dir) {
    File[] list = dir.listFiles();
    return list != null ? list : new File[0];
  }

  public static File[] listFiles(File dir, FileFilter filter) {
    File[] list = dir.listFiles(filter);
    return list != null ? list : new File[0];
  }

  /**
   * quickie DOSlike file attribute control
   *
   * @return true if file was made read/write.
   * just CREATING a FilePermission object modifies the underlying file,
   * that is really bogus syntax. Need to wrap all of that in a FileAttr class.
   */
  public static boolean makeWritable(File f) {
    try {
      FilePermission attr = new FilePermission(f.getAbsolutePath(), "read,write,delete");
      return true;
    } catch (Exception oops) {
      return false; //errors get us here
    }
  }

  /**
   * @return true if file was made readonly.
   * see makeWritable for complaints about java implementation.
   */
  public static boolean makeReadonly(File f) {
    try {
      FilePermission attr = new FilePermission(f.getAbsolutePath(), "read");
      return true;
    } catch (Exception oops) {
      return false; //errors get us here
    }
  }

  /**
   * returns true is something actively was deleted.
   */
  public static boolean deleteFile(File f) {
    try {
      return f != null && f.exists() && makeWritable(f) && f.delete();
    } catch (Exception oops) {
      return false; //errors get us here
    }
  }


  public static Exception Flush(OutputStream os) {
    try {
      os.flush();
      return null;
    } catch (Exception ex) {
      return ex;
    }
  }

  /**
   * close, stifle all errors. Flush first if object supports flushing
   *
   * @returns whether there were no errors, usually ignored.
   */
  public static <T extends Closeable> boolean Close(T closeable) {
    try {
      if (closeable instanceof Flushable) {
        ((Flushable) closeable).flush();
      }
      closeable.close();
      return true;
    } catch (IOException | NullPointerException ignored) {
      return false;
    }
  }

  /**
   * @returns a PrintStream for @param filename or null, doesn't throw exceptions.
   */
  public static PrintStream makePrintStream(String filename) {
    try {
      return new PrintStream(filename);
    } catch (FileNotFoundException | NullPointerException e) {
      return null;
    }
  }

  /**
   * close, stifle all errors. Flush first if object supports flushing
   *
   * @returns whether there were no errors, usually ignored.
   */
  public static <T extends AutoCloseable> boolean Close(T closeable) {
    try {
      if(closeable!=null) {
        if (closeable instanceof Flushable) {
          ((Flushable) closeable).flush();
        }
        closeable.close();
      }
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * returns true if dir now exists
   */
  public static boolean createDir(String filename) {
    return createDir(new File(filename));
  }

  public static boolean createDir(File file) {
    return file.exists() || file.mkdir();
  }

  public static void createDirs(File file) {
    try {
      //noinspection ResultOfMethodCallIgnored
      file.mkdirs();
    } catch (Exception ex) {
      // gulp
    }
  }

  public static void createParentDirs(File file) {
    String parent = file.getParent();
    createDirs(new File(parent));
  }

  public static void createParentDirs(String filename) {
    createParentDirs(new File(filename));
  }

  public static String fromStream(ByteArrayInputStream bais, int len) {
    byte[] chunk = new byte[len];
    if (bais.read(chunk, 0, len) >= 0) {
      return new String(chunk);
    } else {
      return "";
    }
  }

  //////////
  public static boolean FileExists(File f) {
    return f != null && f.exists();
  }

  public static boolean FileExists(String fname) {
    return FileExists(new File(fname));
  }

  public static FileOutputStream fileOutputStream(String filename) {
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(filename);
    } catch (Exception e) {
      // +++ bitch
    }
    return fos;
  }

  public static String FileToString(String filename) {
    try {
      FileInputStream filein = new FileInputStream(filename);
      ByteArrayOutputStream baos = new ByteArrayOutputStream((int) fileSize(filename));
      Streamer.Buffered(filein, baos);
      return new String(baos.toByteArray());
    } catch (Exception ex) {
      return "";
    }
  }

  /**
   * Creates a unique filename given a particular pattern
   *
   * @param pathedPrefix - the path (just directory) where the file will be located + the first part of the filename
   * @param suffix       - the last part of the filename
   *                     <p>
   *                     ie: filename = path + datetimestamp + suffix
   *                     <p>
   *                     eg: createUniqueFilename("c:\temp", "myfile", ".txt") = c:\temp\myfile987654321.txt"
   */
  public static String createUniqueFilename(String pathedPrefix, String suffix) {
    //todo:0 needs mutex, also java.nio has this functionality so proxy to that.
    for (int attempts = 20; attempts-- > 0; ) {
      String filename = pathedPrefix + DateX.timeStampNow() + suffix;
      File file = new File(filename);
      if (!file.exists()) {
        return filename;
      }
    }
    return null;
  }

  public static long fileModTicks(String filename) {
    try {
      return (new File(filename)).lastModified();
    } catch (Exception anything) {
      return -1;
    }
  }

  public static UTC fileModTime(String filename) {
    return UTC.New(fileModTicks(filename));
  }

  public static long fileSize(String filename) {
    try {
      return (new File(filename)).length();
    } catch (Exception anything) {
      return -1;
    }
  }

  public static TextList showAsciiProfile(String filename) {
    try {
      return showAsciiProfile(new FileInputStream(filename));
    } catch (Exception ex) {
      TextList ret = new TextList();
      ret.add("Exception Ascii profiling \"" + filename + "\": " + ex);
      return ret;
    }
  }

  public static int[] asciiProfile(InputStream in) throws IOException {
    int[] profile = new int[256];
    int one;
    while ((one = in.read()) != -1) {
      profile[one]++;
    }
    return profile;
  }

  public static TextList showAsciiProfile(InputStream in) {
    TextList ret = new TextList();
    try {
      int[] profile = asciiProfile(in);
      for (int i = 0; i < profile.length; i++) {
        ret.add("Value " + i + " had " + profile[i] + " occurrences.");
      }
    } catch (Exception ex) {
      ret.add("IOException Ascii profiling stream: " + ex);
    }
    return ret;
  }

  public static BufferedReader StreamLineReader(InputStream stream) {
    try {
      if (stream != null) {//what other stream state should we check?
        return new BufferedReader(new InputStreamReader(stream));
      } else {
        return null;
      }
    } catch (Exception ex) {
      return null;
    }
  }

  /**
   * @return reader that can read whole lines from a file
   */
  public static BufferedReader FileLineReader(File file) {
    try {
      return StreamLineReader(new FileInputStream(file));
    } catch (Exception ex) {
      return null;
    }
  }

  /**
   * @param fname name of file to read lines from
   * @returns reader that can read whole lines from a file
   * todo:2 return a reader that passes back error messages instead of returning null.
   */
  public static BufferedReader FileLineReader(String fname) {
    try {
      return StreamLineReader(new FileInputStream(fname));
    } catch (Exception ex) {
      return null;
    }
  }

  public static PrintStream FilePrinter(String fname) {
    try {
      return new PrintStream(new FileOutputStream(new File(fname)));
    } catch (Exception ex) {
      System.err.print("Making a FilePrinter got:" + ex);
      System.err.println("...output will go to this stream instead of file [" + fname + "]");
      return System.err;
    }
  }

  /**
   * read contents of a regular file into an array of strings
   *
   * @returns textlist with each item one line from file
   */
  public static TextList TextFileContent(File file) {
    TextList content = new TextList();
    BufferedReader reader = FileLineReader(file);
    if (reader != null) {
      try {
        while (reader.ready()) {
          content.add(reader.readLine());
        }
      } catch (Throwable ex) {
        //on exception break while and keep what we got, adding note:
        content.add("Exception while reading file");
        content.add(ex.getLocalizedMessage());
      }
    }

    return content;
  }

  /**
   * @param fname filename
   * @return see TextFileContent(File file)
   */
  public static TextList TextFileContent(String fname) {
    return TextFileContent(new File(fname));
  }

  public static void main(String[] args) {
    System.out.println(showAsciiProfile(args[0]));
  }
}
