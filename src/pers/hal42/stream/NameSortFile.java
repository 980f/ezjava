package pers.hal42.stream;

import java.util.*;

/**
 * <p>Title: $Source: /cvs/src/net/paymate/io/NameSortFile.java,v $</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: PayMate.net</p>
 * @author PayMate.net
 * @version $Revision: 1.1 $
 */

import java.io.File;

public class NameSortFile extends BaseSortFile implements Comparator {

  /*package*/ NameSortFile(boolean descending) {
    super(descending);
  }

  public static NameSortFile Ascending() {
    return new NameSortFile(false);
  }

  public static NameSortFile Descending() {
    return new NameSortFile(true);
  }

  private String pathname(Object oh) {
    return( (File) oh).getAbsolutePath();
  }

  public int compare(Object o1, Object o2) {
    return chain(pathname(o1).compareTo(pathname(o2)), o1, o2);
  }

} //$Id: NameSortFile.java,v 1.1 2004/03/08 22:54:14 andyh Exp $
