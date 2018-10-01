package ru.hh.nab.starter.exceptions;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class SecurityExceptionMapper extends NabExceptionMapper<SecurityException> {
  public SecurityExceptionMapper() {
    super(Response.Status.FORBIDDEN, LoggingLevel.INFO_WITHOUT_STACK_TRACE);
  }
}
