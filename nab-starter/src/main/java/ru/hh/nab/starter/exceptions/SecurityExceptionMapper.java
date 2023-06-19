package ru.hh.nab.starter.exceptions;

import jakarta.annotation.Priority;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import static ru.hh.nab.starter.exceptions.NabExceptionMapper.LOW_PRIORITY;

@Provider
@Priority(LOW_PRIORITY)
public class SecurityExceptionMapper extends NabExceptionMapper<SecurityException> {
  public SecurityExceptionMapper() {
    super(Response.Status.FORBIDDEN, LoggingLevel.INFO_WITHOUT_STACK_TRACE);
  }
}
