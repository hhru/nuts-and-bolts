package ru.hh.nab.starter.exceptions;

import java.sql.SQLTransientConnectionException;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import javax.annotation.Priority;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import javax.ws.rs.ext.Provider;
import org.apache.commons.lang3.exception.ExceptionUtils;
import static ru.hh.nab.starter.exceptions.NabExceptionMapper.LOW_PRIORITY;

@Provider
@Priority(LOW_PRIORITY)
public class AnyExceptionMapper extends NabExceptionMapper<Exception> {
  public AnyExceptionMapper() {
    super(INTERNAL_SERVER_ERROR, LoggingLevel.ERROR_WITH_STACK_TRACE);
  }

  @Override
  public Response toResponseInternal(Response.StatusType status, LoggingLevel loggingLevel, Exception exception) {
    List<Throwable> throwableList = ExceptionUtils.getThrowableList(exception);
    var serviceUnavailableTypeException = throwableList.stream()
      .filter(ex -> ex instanceof SQLTransientConnectionException || ex instanceof RejectedExecutionException)
      .findAny();

    if (serviceUnavailableTypeException.isPresent()) {
      status = SERVICE_UNAVAILABLE;
      loggingLevel = LoggingLevel.WARN_WITHOUT_STACK_TRACE;
    }

    return super.toResponseInternal(status, loggingLevel, exception);
  }
}
