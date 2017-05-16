package pers.hal42.stream;

import pers.hal42.lang.StringX;


public class FileName {
  String name;

  // +++ parse into pieces here and create functions for manipulating pieces
  // (pathstack, filename, and extension, and dot, if there is one)
  public FileName(String name) {
    this.name = name;
  }

  public String safeExtension() {
    return safeExtension(name);
  }

  public String extension() {
    return extension(name);
  }

  public static String safeExtension(String fullname) {
    return StringX.TrivialDefault(extension(fullname), "");
  }

  public static String extension(String fullname) {
    return StringX.afterLastDot(fullname);
  }
}
