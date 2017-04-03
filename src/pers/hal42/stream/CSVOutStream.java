package pers.hal42.stream;

import java.io.*;

public class CSVOutStream extends FilterOutputStream {

  boolean linestarted=false;
  public int EOL='\n'; //do NOT use system dependent info, use internet defaults.

  public void write(int b) throws IOException {
    if(b==EOL){
      linestarted=false;
    }
    super.write(b);
  }

  CSVOutStream write(String processed,boolean quoteit) throws IOException {
    if(linestarted){
      write(',');
    } else {
      linestarted=true;
    }
    if(quoteit){
      write('"');
    }
    write(processed.getBytes());
    if(quoteit){
      write('"');
    }
    return this;
  }

  public CSVOutStream flushLine()throws IOException {
    write(EOL);
    super.flush();
    return this;
  }

  public CSVOutStream append(String s)throws IOException {
    return write(s,true); //quote all strings
  }

  public CSVOutStream append(int eye)throws IOException {
    return write(Integer.toString(eye),false);
  }

  public CSVOutStream append(long ell)throws IOException {
    return write(Long.toString(ell),false);
  }

/**
 * convert embedded special characters to \ escape codes before output.
 */
//  public CSVOutStream escape(String s){
//    write(s,true); //quote all strings
//  }

  public CSVOutStream(OutputStream out) {
    super(out);
  }

}

