package ru.hh.nab.starter.exceptions;

import javax.annotation.Priority;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import static ru.hh.nab.starter.exceptions.NabExceptionMapper.LOW_PRIORITY;

@Provider
@Priority(LOW_PRIORITY)
public class NotFoundExceptionMapper extends NabExceptionMapper<NotFoundException> {
  public NotFoundExceptionMapper() {
    super(null, LoggingLevel.NOTHING);
  }

  @Override
  protected Response serializeException(Response.StatusType statusCode, NotFoundException exception) {
    return exception.getResponse();
  }
}
