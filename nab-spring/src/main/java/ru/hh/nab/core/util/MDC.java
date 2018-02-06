package ru.hh.nab.core.util;

import java.util.Optional;

public class MDC {
  public static final String REQUEST_ID_MDC_KEY = "request_id";
  public static final String CONTROLLER_MDC_KEY = "controller";
  public static final String DATA_SOURCE_MDC_KEY = "db";

  public static void setKey(String key, String value) {
    org.slf4j.MDC.put(key, value);
  }

  public static Optional<String> getKey(String key) {
    return Optional.ofNullable(org.slf4j.MDC.get(key));
  }

  public static void deleteKey(String key) {
    org.slf4j.MDC.remove(key);
  }

  private MDC() {
  }
}
