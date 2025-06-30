package ru.hh.nab.common.mdc;

import java.util.Optional;
import java.util.concurrent.Callable;
import ru.hh.nab.common.util.ExceptionUtils;
import ru.hh.trace.TraceIdGenerator;

public class MDC {
  public static final String REQUEST_ID_MDC_KEY = "rid";
  public static final String CONTROLLER_MDC_KEY = "controller";

  public static Optional<String> getRequestId() {
    return getKey(REQUEST_ID_MDC_KEY);
  }

  public static void setRequestId(String rid) {
    setKey(REQUEST_ID_MDC_KEY, rid);
  }

  public static void clearRequestId() {
    deleteKey(REQUEST_ID_MDC_KEY);
  }

  public static Optional<String> getController() {
    return getKey(CONTROLLER_MDC_KEY);
  }

  public static void setController(String controller) {
    setKey(CONTROLLER_MDC_KEY, controller);
  }

  public static void clearController() {
    deleteKey(CONTROLLER_MDC_KEY);
  }

  public static Optional<String> getKey(String key) {
    return Optional.ofNullable(org.slf4j.MDC.get(key));
  }

  public static void setKey(String key, String value) {
    org.slf4j.MDC.put(key, value);
  }

  public static void deleteKey(String key) {
    org.slf4j.MDC.remove(key);
  }


  /**
   * @param rid use {@link TraceIdGenerator#generateTraceId()} to generate valid request ids
   * @param operation a callable
   * @return callable result
   */
  public static <T> T runWithRequestId(String rid, Callable<T> operation) {
    String previousRequestId = MDC.getRequestId().orElse(null);
    try {
      MDC.setRequestId(rid);
      return ExceptionUtils.getOrThrow(operation);
    } finally {
      MDC.setRequestId(previousRequestId);
    }
  }

  private MDC() {
  }
}
