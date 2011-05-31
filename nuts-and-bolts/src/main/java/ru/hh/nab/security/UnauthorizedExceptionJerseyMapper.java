package ru.hh.nab.security;

import com.google.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
@Singleton
public class UnauthorizedExceptionJerseyMapper implements ExceptionMapper<UnauthorizedException> {
  @Override
  public Response toResponse(UnauthorizedException exception) {
    return Response.status(Response.Status.FORBIDDEN).build();
  }
}
