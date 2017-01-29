package pers.hal42.util;
import  java.io.FileFilter;
import  java.io.File;

public class TailFilter implements FileFilter {
  protected String myFilter = null;

  public boolean accept(File pathName) {
    return (myFilter != null) && pathName.isFile() && pathName.getName().endsWith(myFilter);
  }

  public TailFilter(String filter) {
    myFilter = filter;
  }

}

