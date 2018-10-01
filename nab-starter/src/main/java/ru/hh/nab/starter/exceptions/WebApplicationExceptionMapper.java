package ru.hh.nab.starter.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class WebApplicationExceptionMapper extends NabExceptionMapper<WebApplicationException> {
  public WebApplicationExceptionMapper() {
    super(null, LoggingLevel.ERROR_WITH_STACK_TRACE);
  }

  @Override
  protected void logException(WebApplicationException wae, LoggingLevel loggingLevel) {
    if (wae.getCause() == null) {
      return;
    }
    super.logException(wae, loggingLevel);
  }

  @Override
  protected Response serializeException(Response.StatusType statusCode, WebApplicationException exception) {
    return exception.getResponse();
  }
}
