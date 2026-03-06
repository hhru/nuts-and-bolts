package ru.hh.nab.web.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;

public interface ExceptionSerializer {
  /**
   * Returns true if serializer can be used to produce the response.
   */
  boolean isCompatible(HttpServletRequest request, HttpServletResponse response);

  Response serializeException(Response.StatusType statusCode, Exception exception);
}
