package ru.hh.nab.starter.exceptions;

import java.util.List;
import javax.annotation.Priority;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
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
