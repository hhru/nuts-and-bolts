package ru.hh.nab.logging.json;

public final class JsonFieldNames {
  public static final String TIMESTAMP = "ts";
  public static final String LEVEL = "lvl";
  public static final String MESSAGE = "msg";
  public static final String LOGGER = "logger";
  public static final String EXCEPTION = "exception";
  public static final String MDC = "mdc";
  public static final String APPENDER = "appender";

  public static final String DEFAULT_TIMESTAMP_FORMAT = "yyyy-MM-dd' 'HH:mm:ss.SSSZ";

  private JsonFieldNames() {}
}
