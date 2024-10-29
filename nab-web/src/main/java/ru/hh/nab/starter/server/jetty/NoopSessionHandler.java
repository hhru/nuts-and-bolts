package ru.hh.nab.starter.server.jetty;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.session.SessionHandler;

// TODO: https://jira.hh.ru/browse/HH-237202
public class NoopSessionHandler extends SessionHandler {

  @Override
  public void doScope(
      String target,
      Request baseRequest,
      HttpServletRequest request,
      HttpServletResponse response
  ) throws IOException, ServletException {
    nextScope(target, baseRequest, request, response);
  }

  @SuppressWarnings("RedundantMethodOverride")
  @Override
  public void doHandle(
      String target,
      Request baseRequest,
      HttpServletRequest request,
      HttpServletResponse response
  ) throws IOException, ServletException {
    nextHandle(target, baseRequest, request, response);
  }
}
