package ru.hh.nab.starter.exceptions;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import static ru.hh.nab.starter.exceptions.NabExceptionMapper.LOW_PRIORITY;

@Provider
@Priority(LOW_PRIORITY)
@ApplicationScoped
public class NotFoundExceptionMapper extends NabExceptionMapper<NotFoundException> {
  public NotFoundExceptionMapper() {
    super(null, LoggingLevel.NOTHING);
  }

  @Override
  protected Response serializeException(Response.StatusType statusCode, NotFoundException exception) {
    return exception.getResponse();
  }
}
