package pers.hal42.util;

import  java.io.FileFilter;
import  java.io.File;

public class DirFilter implements FileFilter {
  public boolean accept(File pathName) {
    return pathName.isDirectory();
  }

}
