package ru.hh.nab.starter.exceptions;

import javax.annotation.Priority;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import javax.ws.rs.ext.Provider;
import static ru.hh.nab.starter.exceptions.NabExceptionMapper.LOW_PRIORITY;

@Provider
@Priority(LOW_PRIORITY)
public class IllegalArgumentExceptionMapper extends NabExceptionMapper<IllegalArgumentException> {
  public IllegalArgumentExceptionMapper() {
    super(BAD_REQUEST, LoggingLevel.ERROR_WITH_STACK_TRACE);
  }
}
