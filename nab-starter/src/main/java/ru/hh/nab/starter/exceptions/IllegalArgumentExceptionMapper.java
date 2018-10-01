package ru.hh.nab.starter.exceptions;

import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

@Provider
public class IllegalArgumentExceptionMapper extends NabExceptionMapper<IllegalArgumentException> {
  public IllegalArgumentExceptionMapper() {
    super(BAD_REQUEST, LoggingLevel.ERROR_WITH_STACK_TRACE);
  }
}
