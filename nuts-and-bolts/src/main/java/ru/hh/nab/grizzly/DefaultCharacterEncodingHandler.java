package ru.hh.nab.grizzly;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import java.nio.charset.Charset;

public class DefaultCharacterEncodingHandler extends HttpHandler {

  @Override
  public void service(Request request, Response response) throws Exception {
    request.setCharacterEncoding(Charset.defaultCharset().name());
  }
}
