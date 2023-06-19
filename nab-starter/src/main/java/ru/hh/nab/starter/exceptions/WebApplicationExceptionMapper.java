package ru.hh.nab.starter.exceptions;

import jakarta.annotation.Priority;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.List;
import static ru.hh.jclient.common.HttpStatuses.BAD_GATEWAY;
import static ru.hh.jclient.common.HttpStatuses.INTERNAL_SERVER_ERROR;
import static ru.hh.nab.starter.exceptions.NabExceptionMapper.LOW_PRIORITY;

@Provider
@Priority(LOW_PRIORITY)
public class WebApplicationExceptionMapper extends NabExceptionMapper<WebApplicationException> {

  private static final List<Integer> ALWAYS_LOGGABLE_ERRORS = List.of(INTERNAL_SERVER_ERROR, BAD_GATEWAY);

  public WebApplicationExceptionMapper() {
    super(null, LoggingLevel.ERROR_WITH_STACK_TRACE);
  }

  @Override
  protected void logException(WebApplicationException wae, LoggingLevel loggingLevel) {
    if (wae.getCause() == null && !ALWAYS_LOGGABLE_ERRORS.contains(wae.getResponse().getStatus())) {
      return;
    }
    if (wae.getResponse().getStatus() < 500) {
      loggingLevel = LoggingLevel.INFO_WITH_STACK_TRACE;
    }
    if (wae.getResponse().getStatus() == BAD_GATEWAY) {
      loggingLevel = LoggingLevel.DEBUG_WITH_STACK_TRACE;
    }
    super.logException(wae, loggingLevel);
  }

  @Override
  protected Response serializeException(Response.StatusType statusCode, WebApplicationException exception) {
    return exception.getResponse();
  }
}
