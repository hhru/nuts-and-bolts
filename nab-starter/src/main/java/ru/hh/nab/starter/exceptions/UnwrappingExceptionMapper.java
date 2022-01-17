package ru.hh.nab.starter.exceptions;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import org.glassfish.jersey.server.internal.process.MappableException;
import org.glassfish.jersey.spi.ExceptionMappers;

public abstract class UnwrappingExceptionMapper<T extends Exception> implements ExceptionMapper<T> {

  @Inject
  private ExceptionMappers mappers;

  @Override
  public Response toResponse(T exception) {
    Throwable cause = exception.getCause();
    if (cause != null) {
      ExceptionMapper<Throwable> mapper = mappers.findMapping(cause);
      if (mapper != null) {
        return mapper.toResponse(cause);
      }

      if (cause instanceof WebApplicationException) {
        return ((WebApplicationException) cause).getResponse();
      }
    }

    throw new MappableException(cause == null ? exception : cause);
  }
}
