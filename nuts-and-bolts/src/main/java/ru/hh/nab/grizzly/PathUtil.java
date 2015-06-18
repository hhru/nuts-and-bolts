package ru.hh.nab.grizzly;

public class PathUtil {

  private PathUtil() {}

  public static String removeLastSlash(String path) {
    while (path.length() > 0 && path.charAt(path.length() - 1) == '/' && !path.equals("/")) {
      path = path.substring(0, path.length() - 1);
    }
    return path;
  }

  public static String contextPath(String path) {
    if (path.charAt(0) != '/') {
      throw new IllegalArgumentException("Path must start with '/'");
    }
    int sndSlashIdx = path.indexOf('/', 1);
    if (sndSlashIdx == -1) {
      return path;
    }
    return path.substring(0, sndSlashIdx);
  }
}
