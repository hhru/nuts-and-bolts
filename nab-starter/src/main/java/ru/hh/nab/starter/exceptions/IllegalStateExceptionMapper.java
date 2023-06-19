package ru.hh.nab.starter.exceptions;

import jakarta.annotation.Priority;
import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import jakarta.ws.rs.ext.Provider;
import static ru.hh.nab.starter.exceptions.NabExceptionMapper.LOW_PRIORITY;

@Provider
@Priority(LOW_PRIORITY)
public class IllegalStateExceptionMapper extends NabExceptionMapper<IllegalStateException> {
  public IllegalStateExceptionMapper() {
    super(CONFLICT, LoggingLevel.ERROR_WITH_STACK_TRACE);
  }
}
