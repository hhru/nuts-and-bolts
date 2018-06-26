package ru.hh.nab.common.mdc;

import java.util.Optional;

public class MDC {
  public static final String REQUEST_ID_MDC_KEY = "request_id";
  public static final String CONTROLLER_MDC_KEY = "controller";

  public static Optional<String> getRequestId() {
    return getKey(REQUEST_ID_MDC_KEY);
  }

  public static Optional<String> getController() {
    return getKey(CONTROLLER_MDC_KEY);
  }

  public static void setKey(String key, String value) {
    org.slf4j.MDC.put(key, value);
  }

  public static void deleteKey(String key) {
    org.slf4j.MDC.remove(key);
  }

  private static Optional<String> getKey(String key) {
    return Optional.ofNullable(org.slf4j.MDC.get(key));
  }

  private MDC() {
  }
}
