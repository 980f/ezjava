package pers.hal42.stream;

import java.io.File;
import java.io.FileFilter;

/**
 * FileFilter that selects directories
 */
public class DirFilter implements FileFilter {
  public boolean accept(File pathName) {
    return pathName.isDirectory();
  }

}
