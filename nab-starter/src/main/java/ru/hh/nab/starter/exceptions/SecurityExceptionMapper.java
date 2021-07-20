package ru.hh.nab.starter.exceptions;

import javax.annotation.Priority;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import static ru.hh.nab.starter.exceptions.NabExceptionMapper.LOW_PRIORITY;

@Provider
@Priority(LOW_PRIORITY)
public class SecurityExceptionMapper extends NabExceptionMapper<SecurityException> {
  public SecurityExceptionMapper() {
    super(Response.Status.FORBIDDEN, LoggingLevel.INFO_WITHOUT_STACK_TRACE);
  }
}
