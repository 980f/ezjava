package pers.hal42.stream;


import  java.io.FileFilter;
import  java.io.File;

public class FileOnlyFilter implements FileFilter {
  public boolean accept(File pathName) {
    return pathName.isFile();
  }
}


