package ru.hh.nab.web.jetty.session;

import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

// TODO: https://jira.hh.ru/browse/HH-237202
public class NoopSessionHandler extends SessionHandler {

  @Override
  public boolean handle(Request request, Response response, Callback callback) throws Exception {
    Handler next = getHandler();
    if (next == null) {
      return false;
    }

    return next.handle(request, response, callback);
  }
}
