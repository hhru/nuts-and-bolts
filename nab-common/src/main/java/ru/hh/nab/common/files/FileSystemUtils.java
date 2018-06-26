package ru.hh.nab.common.files;

import org.apache.commons.lang3.SystemUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileSystemUtils {
  private static final String LINUX_TMPFS_PATH = "/dev/shm";
  private static final String MAC_OS_TMPFS_PATH = "/private/tmpfs/";

  public static Path getTmpfsPath() {
    if (SystemUtils.IS_OS_LINUX) {
      return getPathIfExists(LINUX_TMPFS_PATH);
    }
    if (SystemUtils.IS_OS_MAC_OSX) {
      return getPathIfExists(MAC_OS_TMPFS_PATH);
    }
    return null;
  }

  private static Path getPathIfExists(String path) {
    Path result = Paths.get(path);
    return Files.exists(result) ? result : null;
  }
}
