package ru.hh.nab.web.jetty.security;

import org.eclipse.jetty.ee10.servlet.security.ConstraintSecurityHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

// TODO: https://jira.hh.ru/browse/HH-237202
public class NoopSecurityHandler extends ConstraintSecurityHandler {

  @Override
  public boolean handle(Request request, Response response, Callback callback) throws Exception {
    Handler handler = getHandler();
    if (handler != null) {
      handler.handle(request, response, callback);
    }
    return false;
  }
}
