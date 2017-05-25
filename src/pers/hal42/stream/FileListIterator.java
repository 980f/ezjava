package pers.hal42.stream;

import pers.hal42.lang.StringX;
import pers.hal42.text.StringIterator;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by Andy on 5/25/2017.
 * <p>
 * originally inside of ScrapeSoup.java.
 * <p>
 * NOTE: this will close the file when the last string is read from it.
 */
public class FileListIterator implements StringIterator {
  FileReader file;
  BufferedReader bylines;
  boolean done = false;
  String nextValue = null;

  public FileListIterator(FileReader file) {
    this.file = file;
    bylines = new BufferedReader(file);
    next();//prime the pump
  }

  public FileListIterator(String filename) throws FileNotFoundException {
    this(new FileReader(filename));
  }

  public boolean hasNext() {
    return nextValue != null;
  }

  public String next() {
    String nextly = nextValue; //try/finally did ambiguous things on init
    try {
      nextValue = bylines.readLine();
    } catch (IOException e) {
      nextValue = null;
    }
    if (nextValue != null) {
      nextValue = StringX.trim(nextValue);//trim converts nulls to empty string.
    } else {
      IOX.Close(file);
    }
    return nextly;
  }
}
