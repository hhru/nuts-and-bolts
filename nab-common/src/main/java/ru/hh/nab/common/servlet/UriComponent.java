package ru.hh.nab.common.servlet;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class UriComponent {

  /**
   * Copies logic (changed method and variable names)
   * from {@link org.glassfish.jersey.uri.UriComponent#decodeQuery(java.lang.String, boolean)}
   * Tradeoff between adding heavy library to just parse query and supporting custom good-enough implementation
   * Use this to get query parameter from filter
   */
  public static Map<String, List<String>> decodeQuery(String queryString, boolean decodeNames, boolean decodeValues) {
    var result = new HashMap<String, List<String>>();
    if (queryString == null || queryString.length() == 0) {
      return result;
    }

    int startIndex = 0;
    do {
      final int endIndex = queryString.indexOf('&', startIndex);
      if (endIndex == -1) {
        parseKVDecodeIfNeededAddToResult(result, queryString.substring(startIndex), decodeNames, decodeValues);
      } else if (endIndex > startIndex) {
        parseKVDecodeIfNeededAddToResult(result, queryString.substring(startIndex, endIndex), decodeNames, decodeValues);
      }
      startIndex = endIndex + 1;
    } while (startIndex > 0 && startIndex < queryString.length());

    return result;
  }

  private static void parseKVDecodeIfNeededAddToResult(Map<String, List<String>> params, String param, boolean decodeNames,
                                                       boolean decodeValues) {
    int equalsIndex = param.indexOf('=');
    if (equalsIndex == 0) {
      return;
    }
    if (equalsIndex > 0) {
      var key = (decodeNames) ? URLDecoder.decode(param.substring(0, equalsIndex), StandardCharsets.UTF_8) : param.substring(0, equalsIndex);
      var value = (decodeValues) ? URLDecoder.decode(param.substring(equalsIndex + 1), StandardCharsets.UTF_8) : param.substring(equalsIndex + 1);
      params.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
    } else if (param.length() > 0) {
      var key = (decodeNames) ? URLDecoder.decode(param.substring(0, equalsIndex), StandardCharsets.UTF_8) : param.substring(0, equalsIndex);
      params.computeIfAbsent(key, ignored -> new ArrayList<>()).add("");
    }
  }

  private UriComponent() {
  }
}
