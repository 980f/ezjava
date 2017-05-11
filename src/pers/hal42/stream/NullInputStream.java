package pers.hal42.stream;
import  java.io.IOException;
import  java.io.InputStream;

public class NullInputStream extends InputStream {

  public NullInputStream() {
    super();
  }

  public int read() throws IOException {
    return -1;
  }

}

