package ru.hh.util;

import java.util.regex.Pattern;
import javax.annotation.Nullable;

public class CsvBuilder {
  private final StringBuilder result = new StringBuilder();
  private static final Pattern ESCAPED_CHAR = Pattern.compile("[,\r\n\"]", Pattern.MULTILINE);

  public CsvBuilder put(String... values) {
    if (result.length() != 0)
      result.append("\n");
    for (int i = 0; i < values.length; i++) {
      if (i != 0)
        result.append(',');
      result.append(csvEscaped(values[i]));
    }
    return this;
  }

  public String build() {
    return result.toString();
  }

  static String csvEscaped(@Nullable String src) {
    if (src == null)
      return "null";
    if (ESCAPED_CHAR.matcher(src).find()) {
      return "\"" + src.replaceAll("\"", "\"\"") + "\"";
    }
    return src;
  }
}
