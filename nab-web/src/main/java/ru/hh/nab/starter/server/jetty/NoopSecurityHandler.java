package ru.hh.nab.starter.server.jetty;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;

// TODO: https://jira.hh.ru/browse/HH-237202
public class NoopSecurityHandler extends ConstraintSecurityHandler {

  @Override
  public void handle(
      String target,
      Request baseRequest,
      HttpServletRequest request,
      HttpServletResponse response
  ) throws IOException, ServletException {
    Handler handler = getHandler();
    if (handler != null) {
      handler.handle(target, baseRequest, request, response);
    }
  }
}
