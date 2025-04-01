package ru.hh.nab.jclient.metrics;

import java.util.ArrayList;
import java.util.List;

class SimpleJsonBuilder {
  private List<Pair> pairs = new ArrayList<>();

  void put(String key, Object value) {
    pairs.add(new Pair(key, value));
  }

  String build() {
    var sb = new StringBuilder(64).append("{");
    for (Pair pair : pairs) {
      sb.append(pair.build()).append(",");
    }
    if (sb.charAt(sb.length() - 1) == ',') {
      sb.setCharAt(sb.length() - 1, '}');
    } else {
      sb.append('}');
    }
    return sb.toString();
  }

  static class Pair {
    private final String key;
    private final Object value;

    Pair(String key, Object value) {
      this.key = key;
      this.value = value;
    }

    String build() {
      if (value == null) {
        return "\"" + key + "\":null";
      } else if (value instanceof Number) {
        return "\"" + key + "\":" + value;
      } else {
        return "\"" + key + "\":\"" + value.toString() + "\"";
      }
    }
  }
}
