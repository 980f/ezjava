package pers.hal42.util;
//import  net.paymate.Main; // --- I hate this

import  java.io.*;//getReader & getInputStream use just about everything

public class Streamer implements Runnable {
  private static final ErrorLogStream dbg = new ErrorLogStream(Streamer.class.getName(), ErrorLogStream.VERBOSE);
/*
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

  public static final InputStream getInputStream(Object item){
    InputStream casted=null;
    try {
      if(item != null){
        if(item instanceof InputStream){
          casted=(InputStream)item;
        } else
        if(item instanceof byte []){
          casted= new ByteArrayInputStream((byte [])item);
        } else
        if(item instanceof FileDescriptor){
          casted= new FileInputStream((FileDescriptor)item);
        } else
        if(item instanceof File){//structured filename
          casted= new FileInputStream((File)item);
        } else
        if(item instanceof String){//raw file name
          casted= new FileInputStream(Main.LocalFile((String)item));
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

// blocks if reader or writer block; mostly meant for files and strings
  public static final int swapCharacters(Reader in, Writer out) throws IOException {
    if(in == null) {
      throw(new IOException("Streamer.swapCharacters: IN is null."));
    }
    if(out == null) {
      throw(new IOException("Streamer.swapCharacters: OUT is null."));
    }
    int b;
    // here's the almighty spew
    int spewed = 0;
    while((b = in.read()) != -1) {
      out.write(b);
      spewed++;
    }
    return spewed;
  }

  private static final int DEFAULTBUFFERSIZE = 10000;
  private static final int MAXBUFFERSIZE = 100000;

  public static final long swapStreams(InputStream in, OutputStream out) throws IOException {
    return swapStreams(in, out, DEFAULTBUFFERSIZE);
  }

  public static final long swapStreams(InputStream in, OutputStream out, int buffsize) throws IOException {
    return swapStreams(in, out, buffsize, -1);
  }

  /**
   * @param howMany - how many bytes to move; -1 means all
   */
  public static final long swapStreams(InputStream in, OutputStream out, int buffsize, long howMany) throws IOException {
    if(in == null) {
      throw(new IOException("Streamer.swapStreams: IN is null."));
    }
    if(out == null) {
      throw(new IOException("Streamer.swapStreams: OUT is null."));
    }
    int max = DEFAULTBUFFERSIZE; // the default
    // don't use more than howMany:
    if(howMany > 0) {
      max = Math.min(max, (int)howMany);
    }
    // don't use more than buffsize:
    if(buffsize > 0) {
      max = Math.min(max, buffsize);
    }
    // limit to < MAXBUFFERSIZE bytes, to be safe and reasonable
    max = Math.min(max, MAXBUFFERSIZE);
    byte [] bytes = new byte[max];
    long spewed = 0;
    while((howMany > 0) ? (spewed < howMany) : true) {
      int thisRead = in.read(bytes);
      if(thisRead == -1){
        // +++ bitch?
        break;
      }
      out.write(bytes, 0, thisRead);
      spewed+=thisRead;
    }
    out.flush();
    return spewed;
  }

  public static final Streamer backgroudSwapStreams(InputStream in, OutputStream out) {
    Streamer streamer = new Streamer(in, out);
    Thread thread = new Thread(streamer);
    thread.start();
    return streamer;
  }

  public Streamer(InputStream in, OutputStream out) {
    this.in = in;
    this.out = out;
  }

  InputStream in = null;
  OutputStream out = null;
  // +++ enumerate
  int status = NOTSTARTED;
  long count = 0;
  static final int NOTSTARTED = -1;
  static final int RUNNING = 0;
  static final int DONE = 1;
  static final int ERRORED = 2;

  public void run() {
    status = RUNNING;
    try {
      // +++ add the ability to drop this to a lower priority.
      count = swapStreams(in, out); // the read blocking can make this run at a lower priority, for now
      status = DONE;
    } catch (Exception e) {
      status = ERRORED;
// +++ bitch
    }
  }

}
