package ru.hh.nab.starter.exceptions;

import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status.CONFLICT;

@Provider
public class IllegalStateExceptionMapper extends NabExceptionMapper<IllegalStateException> {
  public IllegalStateExceptionMapper() {
    super(CONFLICT, LoggingLevel.ERROR_WITH_STACK_TRACE);
  }
}
