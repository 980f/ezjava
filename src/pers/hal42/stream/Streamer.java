package pers.hal42.stream;

import pers.hal42.Main;
import pers.hal42.lang.ObjectX;
import pers.hal42.logging.ErrorLogStream;
import pers.hal42.logging.LogLevelEnum;
import pers.hal42.text.Ascii;
import pers.hal42.thread.ThreadX;
import pers.hal42.util.InstanceNamer;

import java.io.*;
import java.util.EventObject;

public class Streamer implements Runnable {
  public long count = 0;
  int status = NOTSTARTED;
  private Thread thread = null; //background piping occurs on this thread
  private StreamEventListener listener = null; //bad things are reported to this thing
  private boolean ignoreEOF = false; //for ill behaved input streams, ignore End Of File indications.
  private int buffsize; //max ot transfer without coming up for air.
  private long howmany;//max to transfer, <0 implies transfer forever.
  private InputStream in = null; //input end of pipe
  private OutputStream out = null;//output end of pipe
  private boolean enabled = true; // set to false to kill one that has been streaming forever, but needs to stop!
  //special value for "howMany" argument
  public static final int StreamForever = -1;
  //values for "status"   // +++ enumerate
  static final int NOTSTARTED = -1;
  static final int RUNNING = 0;
  static final int DONE = 1;
  static final int ERRORED = 2;
  private static final ErrorLogStream dbg = ErrorLogStream.getForClass(Streamer.class, LogLevelEnum.WARNING);
  //@todo code in swapStreams make DEFAULTBUFFERSIZE be an absolute max!!!
  private static final int DEFAULTBUFFERSIZE = 10000;
  private static final int MAXBUFFERSIZE = 100000;
  //need to distinguish threads while debugging:
  private static InstanceNamer threadNamer = new InstanceNamer("Streamer");

  private Streamer(InputStream in, OutputStream out, int buffsize, long
    howmany, StreamEventListener listener, boolean ignoreEOF) {
    this.in = in;
    this.out = out;
    this.listener = listener;
    this.ignoreEOF = ignoreEOF;
    this.buffsize = buffsize;
    this.howmany = howmany;
    if (listener != null) {//run on background thread
      thread = new Thread(this, threadNamer.Next());
      thread.start();
    } else {//else run now.
      run();
    }
  }

  public Streamer(InputStream in, OutputStream out) {
    this.in = in;
    this.out = out;
  }

  /**
   * transfer bytes per arguments set on 'this'
   */
  public void run() {
    status = RUNNING;
    try {
      // +++ add the ability to drop this to a lower priority. --- maybe not.  bad thing to muck with
      count = swapStreams(in, out, buffsize, howmany, ignoreEOF); // the read blocking can make this run at a lower priority, for now
      status = DONE;
    } catch (Exception e) {
      dbg.Caught(e);
      status = ERRORED;
    }
    if (listener != null) {
      listener.notify(new EventObject(this));//not much info in this notification..need to extend EventObject into StreamClosingEvent
    } else {
      dbg.VERBOSE("Run ended, but can't notify since listener is null.");
    }
    in = null; //input end of pipe
    out = null;//output end of pipe
  }

  public void StopAndClose() {
    dbg.ERROR("Stopping and closing streams");
    IOX.Close(in);
    IOX.Close(out);
    Stop();
  }

  public void StopNoClose() {
    dbg.ERROR("Stopping but NOT closing streams");
    Stop();
  }
  // end of class proper
  /////////////////

  private void Stop() {
    enabled = false;
    if ((thread != null) && thread.isAlive()) {
      thread.interrupt();
//      ThreadX.join(thread, 1000); // ??? --- safety feature
    }
  }

  /**
   * @param howMany - how many bytes to move; -1 means all
   */
  private long swapStreams(InputStream in, OutputStream out, int buffsize, long howMany, boolean ignoreEOF) throws IOException {
    if (in == null) {
      throw (new IOException("Streamer.swapStreams: IN is null."));
    }
    if (out == null) {
      throw (new IOException("Streamer.swapStreams: OUT is null."));
    }
    boolean forever = howMany < 0;
    // limit to < MAXBUFFERSIZE bytes, to be safe and reasonable
    int max = MAXBUFFERSIZE; // the maximum we are willing to move
    // don't use more than howMany:
    if (!forever) {//then we are given amount to move
      max = Math.min(max, (int) howMany);
    }
    // but don't move more than buffsize per iteration:
    if (buffsize > 0) {
      max = Math.min(max, buffsize);
    }

    long spewed = 0;
    dbg.VERBOSE((forever ? "piping forever" : ("piping up to " + howMany)) + " in chunks of " + max);
    if (max == 1) {
      int bytes;//= Receiver.EndOfInput;
      while (enabled && (forever || (spewed < howMany))) {
        dbg.VERBOSE("waiting for " + max + " byte(s)");
        bytes = in.read();
        if (bytes == Receiver.EndOfInput) {
          if (ignoreEOF) {
            dbg.VERBOSE("ignoring EOF, continuing");
            ThreadX.sleepFor(5);//+++parameterize.
            // use of read(byte[]) has defeated the "nicing" of javaxPort.
            continue;
          } else {
            dbg.VERBOSE("EOF received after " + spewed + " bytes transferred!");
            break;
          }
        }
        if (bytes > ObjectX.INVALIDINDEX) {//other negative values will come in someday!
          dbg.VERBOSE("piped:" + Ascii.bracket(bytes));
          out.write(bytes);
          out.flush(); // ----- testing !!!
          spewed++;
        }
      }
      out.flush();
    } else {
      byte[] bytes = new byte[max];
      while (enabled && (forever || (spewed < howMany))) {
        dbg.VERBOSE("waiting for " + max + " byte(s)");
        int thisRead = in.read(bytes);
        if (thisRead == Receiver.EndOfInput) {
          if (ignoreEOF) {
            dbg.VERBOSE("ignoring EOF, continuing");
            ThreadX.sleepFor(17);//+++parameterize.
            // use of read(byte[]) has defeated the "nicing" of javaxPort.
            continue;
          } else {
            dbg.VERBOSE("EOF received after " + spewed + " bytes transferred!");
            break;
          }
        }
        if (thisRead > -1) {//other negative values will come in someday!
          dbg.VERBOSE("piping " + thisRead + " bytes");
          out.write(bytes, 0, thisRead);
          out.flush(); // ----- testing !!!
          dbg.VERBOSE("piped:" + Ascii.bracket(bytes));
          spewed += thisRead;
        }
      }
      out.flush();
    }
    return spewed;
  }

  public static Streamer Unbuffered(InputStream in, OutputStream out, boolean ignoreEOF) {
    return Unbuffered(in, out, null, ignoreEOF);
  }

  public static Streamer Unbuffered(InputStream in, OutputStream out, StreamEventListener listener, boolean ignoreEOF) {
    return Buffered(in, out, 1, StreamForever, listener, ignoreEOF);//"Buffered" used but args defeat buffering
  }

  // most common use:
  public static Streamer Buffered(InputStream in, OutputStream out) {
    return Buffered(in, out, DEFAULTBUFFERSIZE, StreamForever, null, false);//@@@
  }

  /////////////////
  // Stream related utilities, not attached to class members at all.

  /* this was in use by jpos control layer stuff. Why is it commented out?
  public static final BufferedReader getReader(Object item) {
    BufferedReader casted=null;
    try {
      if(item != null){
        if(item instanceof BufferedReader){
          casted=(BufferedReader)item;
        } else if(item instanceof Reader){
          casted= new BufferedReader((Reader)item);
        } else if(item instanceof byte []){
          casted= new BufferedReader(new InputStreamReader(new ByteArrayInputStream((byte [])item)));
          //mark() and reset() were giving me fits...easier to store data and make new stream
        } else if(item instanceof FileDescriptor){
          casted= new BufferedReader(new FileReader((FileDescriptor)item));
        } else if(item instanceof File){//structured filename
          casted= new BufferedReader(new FileReader((File)item));
        } else if(item instanceof String){//raw file name
          String pathname=(String)item;
          if(!OS.isRooted(pathname)){
            pathname=Main.Application.Home+pathname;
          }
          casted= new BufferedReader(new FileReader(pathname));
        }
      }
    }
    catch (IOException ioex){
      //should we let it throw?
      //by doing nothing we return a null on error.
    }
    finally {
      return casted;
    }
  }
*/

  public static Streamer Buffered(InputStream in, OutputStream out, int buffsize, long howmany, StreamEventListener listener, boolean ignoreEOF) {
    return new Streamer(in, out, buffsize, howmany, listener, ignoreEOF);
  }

  public static InputStream getInputStream(Object item) {
    InputStream casted = null;
    try {
      if (item != null) {
        if (item instanceof InputStream) {
          casted = (InputStream) item;
        } else if (item instanceof byte[]) {
          casted = new ByteArrayInputStream((byte[]) item);
        } else if (item instanceof FileDescriptor) {
          casted = new FileInputStream((FileDescriptor) item);
        } else if (item instanceof File) {//structured filename
          casted = new FileInputStream((File) item);
        } else if (item instanceof String) {//raw file name
          casted = new FileInputStream(Main.LocalFile((String) item));
        }
      }
    } catch (IOException ioex) {
      //should we let it throw?
      //by doing nothing we return a null on error.
    } finally {
      return casted;
    }

  }

//  private static final int DEFAULTBUFFERSIZE = 10000;
//  private static final int MAXBUFFERSIZE = 100000;

  // blocks if reader or writer block; mostly meant for files and strings
  public static int swapCharacters(Reader in, Writer out) throws IOException {
    if (in == null) {
      throw (new IOException("Streamer.swapCharacters: IN is null."));
    }
    if (out == null) {
      throw (new IOException("Streamer.swapCharacters: OUT is null."));
    }
    int b;
    // here's the almighty spew
    int spewed = 0;
    while ((b = in.read()) != -1) {
      out.write(b);
      spewed++;
    }
    return spewed;
  }

  public static long swapStreams(InputStream in, OutputStream out) throws IOException {
    return swapStreams(in, out, DEFAULTBUFFERSIZE);
  }

  public static long swapStreams(InputStream in, OutputStream out, int buffsize) throws IOException {
    return swapStreams(in, out, buffsize, -1);
  }

  /**
   * @param howMany - how many bytes to move; -1 means all
   */
  public static long swapStreams(InputStream in, OutputStream out, int buffsize, long howMany) throws IOException {
    if (in == null) {
      throw (new IOException("Streamer.swapStreams: IN is null."));
    }
    if (out == null) {
      throw (new IOException("Streamer.swapStreams: OUT is null."));
    }
    int max = DEFAULTBUFFERSIZE; // the default
    // don't use more than howMany:
    if (howMany > 0) {
      max = Math.min(max, (int) howMany);
    }
    // don't use more than buffsize:
    if (buffsize > 0) {
      max = Math.min(max, buffsize);
    }
    // limit to < MAXBUFFERSIZE bytes, to be safe and reasonable
    max = Math.min(max, MAXBUFFERSIZE);
    byte[] bytes = new byte[max];
    long spewed = 0;
    while (howMany <= 0 || (spewed < howMany)) {
      int thisRead = in.read(bytes);
      if (thisRead == -1) {
        // +++ bitch?
        break;
      }
      out.write(bytes, 0, thisRead);
      spewed += thisRead;
    }
    out.flush();
    return spewed;
  }

  public static Streamer backgroudSwapStreams(InputStream in, OutputStream out) {
    Streamer streamer = new Streamer(in, out);
    Thread thread = new Thread(streamer);
    thread.start();
    return streamer;
  }

//  InputStream in = null;
//  OutputStream out = null;
//  // +++ enumerate
//  int status = NOTSTARTED;
//  long count = 0;
//  static final int NOTSTARTED = -1;
//  static final int RUNNING = 0;
//  static final int DONE = 1;
//  static final int ERRORED = 2;

//  public void run() {
//    status = RUNNING;
//    try {
//      // +++ add the ability to drop this to a lower priority.
//      count = swapStreams(in, out); // the read blocking can make this run at a lower priority, for now
//      status = DONE;
//    } catch (Exception e) {
//      status = ERRORED;
//// +++ bitch
//    }
//  }

}
