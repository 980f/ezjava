package pers.hal42.stream;

import pers.hal42.lang.StringX;
import pers.hal42.text.StringIterator;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by Andy on 5/25/2017.
 *
 * originally inside of ScrapeSoup.java.
 *
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
    try {
      return nextValue;
    } finally {
      try {
        nextValue = StringX.trim(bylines.readLine());
      } catch (IOException e) {
        nextValue = null;
        IOX.Close(file);
      }
    }
  }
}
