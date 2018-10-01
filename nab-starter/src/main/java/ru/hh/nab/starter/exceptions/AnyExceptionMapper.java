package ru.hh.nab.starter.exceptions;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

@Provider
public class AnyExceptionMapper extends NabExceptionMapper<Exception> {
  public AnyExceptionMapper() {
    super(INTERNAL_SERVER_ERROR, LoggingLevel.ERROR_WITH_STACK_TRACE);
  }

  @Override
  protected Response serializeException(Response.StatusType statusCode, Exception exception) {
    Throwable cause, lastNotNullCause = exception;
    while ((cause = lastNotNullCause.getCause()) != null) {
      lastNotNullCause = cause;
    }

    if ("com.mchange.v2.resourcepool".equals(lastNotNullCause.getClass().getCanonicalName())) {
      statusCode = SERVICE_UNAVAILABLE;
    }

    return super.serializeException(statusCode, exception);
  }
}
