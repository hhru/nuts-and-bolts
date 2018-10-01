package ru.hh.nab.starter.exceptions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

public interface ExceptionSerializer {
  /**
   * Returns true if serializer can be used to produce the response.
   */
  boolean isCompatible(HttpServletRequest request, HttpServletResponse response);

  Response serializeException(Response.StatusType statusCode, Exception exception);
}
