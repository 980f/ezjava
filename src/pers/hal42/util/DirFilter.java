package pers.hal42.util;

import java.io.File;
import java.io.FileFilter;

public class DirFilter implements FileFilter {
  public boolean accept(File pathName) {
    return pathName.isDirectory();
  }

}
