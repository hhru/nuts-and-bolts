package ru.hh.nab.starter.exceptions;

import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Priority;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.sql.SQLTransientConnectionException;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static ru.hh.nab.starter.exceptions.NabExceptionMapper.LOW_PRIORITY;

@Provider
@Priority(LOW_PRIORITY)
public class AnyExceptionMapper extends NabExceptionMapper<Exception> {
  public AnyExceptionMapper() {
    super(INTERNAL_SERVER_ERROR, LoggingLevel.ERROR_WITH_STACK_TRACE);
  }

  @Override
  public Response toResponse(Exception exception) {
    Throwable cause = ExceptionUtils.getRootCause(exception);

    if (exception instanceof SQLTransientConnectionException || cause instanceof SQLTransientConnectionException) {
      statusCode = SERVICE_UNAVAILABLE;
      loggingLevel = LoggingLevel.WARN_WITHOUT_STACK_TRACE;
    }

    return super.toResponse(exception);
  }
}
