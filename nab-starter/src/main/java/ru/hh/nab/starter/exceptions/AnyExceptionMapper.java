package ru.hh.nab.starter.exceptions;

import jakarta.annotation.Priority;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import java.sql.SQLTransientConnectionException;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import ru.hh.errors.common.Errors;
import ru.hh.nab.starter.http.HttpStatus;
import static ru.hh.nab.starter.jersey.NabPriorities.LOW_PRIORITY;

@Priority(LOW_PRIORITY)
@APIResponse(
    responseCode = "500",
    description = "Internal Server Error",
    content = @Content(
        mediaType = MediaType.APPLICATION_JSON,
        schema = @Schema(
            implementation = Errors.class
        )
    )
)
@APIResponse(
    responseCode = "597",
    description = "Service partially unavailable",
    content = @Content(
        mediaType = MediaType.APPLICATION_JSON,
        schema = @Schema(
            implementation = Errors.class
        )
    )
)
public class AnyExceptionMapper extends NabExceptionMapper<Exception> {
  public AnyExceptionMapper() {
    super(INTERNAL_SERVER_ERROR, LoggingLevel.ERROR_WITH_STACK_TRACE);
  }

  @Override
  protected Response toResponseInternal(Response.StatusType status, LoggingLevel loggingLevel, Exception exception) {
    List<Throwable> throwableList = ExceptionUtils.getThrowableList(exception);
    var serviceUnavailableTypeException = throwableList
        .stream()
        .filter(ex -> ex instanceof SQLTransientConnectionException || ex instanceof RejectedExecutionException)
        .findAny();

    if (serviceUnavailableTypeException.isPresent()) {
      status = HttpStatus.SERVICE_PARTIALLY_UNAVAILABLE;
      loggingLevel = LoggingLevel.WARN_WITHOUT_STACK_TRACE;
    }

    return super.toResponseInternal(status, loggingLevel, exception);
  }
}
