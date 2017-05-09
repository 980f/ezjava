/**
* Title:        DirFilter
* Description:
* Copyright:    2000 PayMate.net
* Company:      paymate
* @author       paymate
* @version      $Id: DirFilter.java,v 1.1 2003/07/27 19:36:54 mattm Exp $
*/
package pers.hal42.stream;
import  java.io.FileFilter;
import  java.io.File;

public class DirFilter implements FileFilter {
  public boolean accept(File pathName) {
    return pathName.isDirectory();
  }

}
//$Id: DirFilter.java,v 1.1 2003/07/27 19:36:54 mattm Exp $
