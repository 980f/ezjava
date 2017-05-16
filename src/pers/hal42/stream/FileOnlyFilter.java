package pers.hal42.stream;


import java.io.File;
import java.io.FileFilter;

public class FileOnlyFilter implements FileFilter {
  public boolean accept(File pathName) {
    return pathName.isFile();
  }
}


