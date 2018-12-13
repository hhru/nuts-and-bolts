package ru.hh.nab.common.mdc;

import java.util.Optional;
import java.util.concurrent.Callable;

public class MDC {
  public static final String REQUEST_ID_MDC_KEY = "rid";
  public static final String CONTROLLER_MDC_KEY = "controller";

  public static Optional<String> getRequestId() {
    return getKey(REQUEST_ID_MDC_KEY);
  }

  public static void setRequestId(String rid) {
    org.slf4j.MDC.put(REQUEST_ID_MDC_KEY, rid);
  }

  public static void clearRequestId() {
    org.slf4j.MDC.remove(REQUEST_ID_MDC_KEY);
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

  public static String generateRequestId(String suffix) {
    return System.currentTimeMillis() + suffix;
  }

  /**
   * @param rid         предпочтительней передавать rid сгенерированный
   *                      с помощью {@link MDC#generateRequestId(java.lang.String)}
   * @param operation   результат этой функции будет возвращен
   * @return            результат параметра operation
   */
  public static <T> T runWithRequestId(String rid, Callable<T> operation) {
    String previousRequestId = MDC.getRequestId().orElse(null);
    try {
      MDC.setRequestId(rid);
      return operation.call();
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      MDC.setRequestId(previousRequestId);
    }
  }

  private MDC() {
  }
}
