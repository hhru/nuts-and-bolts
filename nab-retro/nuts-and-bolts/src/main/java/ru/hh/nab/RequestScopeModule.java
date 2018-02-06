package ru.hh.nab;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.servlet.RequestScoped;
import ru.hh.health.monitoring.TimingsLogger;
import ru.hh.health.monitoring.TimingsLoggerFactory;
import ru.hh.nab.scopes.RequestScope;
import javax.inject.Singleton;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Properties;

import static com.google.common.collect.Maps.newHashMap;

class RequestScopeModule extends AbstractModule {
  @Override
  protected void configure() {
    bindScope(RequestScoped.class, RequestScope.REQUEST_SCOPE);
    requestStaticInjection(RequestScope.class);
    bind(ServletRequest.class).to(HttpServletRequest.class);
    bind(ServletResponse.class).to(HttpServletResponse.class);
  }

  @Provides
  @RequestScoped
  HttpServletRequest httpRequest() {
    Object request = RequestScope.currentRequest();
    if (request == null || !(request instanceof HttpServletRequest)) {
      throw new IllegalArgumentException("Not a HttpServletRequest request");
    }
    return (HttpServletRequest) request;
  }

  @Provides
  @RequestScoped
  HttpServletResponse httpResponse() {
    Object response = RequestScope.currentResponse();
    if (response == null || !(response instanceof HttpServletResponse)) {
      throw new IllegalArgumentException("Not a HttpServletResponse response");
    }
    return (HttpServletResponse) response;
  }

  @Provides
  RequestScope.RequestScopeClosure requestScopeClosure() {
    return RequestScope.currentClosure();
  }

  @Provides
  @Singleton
  TimingsLoggerFactory timingsLoggerFactory(Settings settings) {
    return new TimingsLoggerFactory(getDelays(settings));
  }

  @Provides
  @RequestScoped
  TimingsLogger timingsLogger() {
    return RequestScope.currentTimingsLogger();
  }

  private static Map<String, Long> getDelays(Settings settings) {
    Map<String, Long> delays = newHashMap();
    Properties delaysProps = settings.subTree("timings.delays");
    if (delaysProps != null) {
      for (Map.Entry<Object, Object> ent : delaysProps.entrySet()) {
        delays.put(ent.getKey().toString(), Long.valueOf(ent.getValue().toString()));
      }
    }
    return delays;
  }
}
