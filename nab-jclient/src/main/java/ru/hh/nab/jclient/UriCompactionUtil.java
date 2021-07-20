package ru.hh.nab.jclient;

import java.util.Collection;
import static java.util.Optional.ofNullable;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import ru.hh.jclient.common.Uri;

public final class UriCompactionUtil {

  private static final char SLASH = '/';
  private static final String SLASH_STR = String.valueOf(SLASH);
  private static final Collection<Integer> NON_HEX_LETTERS = IntStream.rangeClosed('g', 'z').boxed()
    .collect(Collectors.toUnmodifiableSet());
  private static final String REPLACEMENT = "[id-or-hash]";

  public static String compactUri(String uriPath, int minCompactingLength, int minPossibleHashLength) {
    return compactUri(uriPath, minCompactingLength, minPossibleHashLength, REPLACEMENT);
  }

  public static String compactUri(Uri uri, int minCompactingLength, int minPossibleHashLength) {
    return compactUri(uri, minCompactingLength, minPossibleHashLength, REPLACEMENT);
  }

  public static String compactUri(Uri uri, int minCompactingLength, int minPossibleHashLength, String replacement) {
    return compactUri(uri == null ? null : uri.getPath(), minCompactingLength, minPossibleHashLength, replacement);
  }

  public static String compactUri(String uriPath, int minCompactingLength, int minPossibleHashLength, String replacement) {
    return ofNullable(uriPath)
      .map(path -> {
        StringJoiner joiner = new StringJoiner(SLASH_STR, SLASH_STR, "");
        int startIndex = 0;
        do {
          final int endIndex = path.indexOf(SLASH, startIndex);
          if (endIndex == -1) {
            joiner.add(compactPathPart(path.substring(startIndex), minCompactingLength, minPossibleHashLength, replacement));
          } else if (endIndex > startIndex) {
            joiner.add(compactPathPart(path.substring(startIndex, endIndex), minCompactingLength, minPossibleHashLength, replacement));
          }
          startIndex = endIndex + 1;
        } while (startIndex > 0 && startIndex < path.length());
        return joiner.toString();
      }).orElse(null);
  }

  private static String compactPathPart(String pathPart, int minCompactingLength, int minPossibleHashLength, String replacement) {
    int partLength = pathPart.length();
    if (partLength < minCompactingLength) {
      return pathPart;
    }
    long digitsCount = pathPart.chars().filter(Character::isDigit).count();
    if (digitsCount == partLength) {
      return replacement;
    }
    if (pathPart.chars().anyMatch(NON_HEX_LETTERS::contains)) {
      return pathPart;
    }
    if (partLength > minPossibleHashLength && digitsCount >= minCompactingLength) {
      return replacement;
    }
    return pathPart;
  }

  private UriCompactionUtil() {
  }
}
