package ru.hh.nab.grizzly;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import java.util.Collections;
import java.util.Map;

public class RequestDispatcher extends HttpHandler {

  private final Map<String, Map<HttpMethod, GrizzletHandler>> handlers;

  public RequestDispatcher(Iterable<GrizzletHandler> grizzlets) {
    Map<String, Map<HttpMethod, GrizzletHandler>> modifiableHandlersMap = Maps.newHashMap();
    for (GrizzletHandler handler : grizzlets) {
      Map<HttpMethod, GrizzletHandler> handlerByMethod = modifiableHandlersMap.get(handler.getPath());
      if (handlerByMethod == null) {
        handlerByMethod = Maps.newEnumMap(HttpMethod.class);
        modifiableHandlersMap.put(handler.getPath(), handlerByMethod);
      }
      for (HttpMethod method : handler.getMethods()) {
        GrizzletHandler alreadyHandler = handlerByMethod.put(method, handler);
        if (alreadyHandler != null) {
          throw new IllegalArgumentException("More than one handler detected for path='" + handler.getPath() + "', method='" + method.name() + '\'');
        }
      }
    }
    for (Map.Entry<String, Map<HttpMethod, GrizzletHandler>> entry : modifiableHandlersMap.entrySet()) {
      modifiableHandlersMap.put(entry.getKey(), Collections.unmodifiableMap(entry.getValue()));
    }
    handlers = ImmutableMap.copyOf(modifiableHandlersMap);
  }

  @Override
  public void service(Request request, Response response) throws Exception {
    String uri = request.getRequestURI();
    Map<HttpMethod, GrizzletHandler> handlerByMethod = handlers.get(uri);
    GrizzletHandler handler = null;
    if (handlerByMethod != null) {
      handler = handlerByMethod.get(HttpMethod.valueOf(request.getMethod().toString()));
    }
    if (handler != null) {
      handler.handle(request, response);
    } else {
      SimpleGrizzlyAdapterChain.abstain();
    }
  }
}

