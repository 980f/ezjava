package pers.hal42.stream;

import java.io.File;
import java.io.FileFilter;

public class TailFilter implements FileFilter {
  protected String myFilter = null;

  public TailFilter(String filter) {
    myFilter = filter;
  }

  public boolean accept(File pathName) {
    return (myFilter != null) && pathName.isFile() && pathName.getName().endsWith(myFilter);
  }

}

