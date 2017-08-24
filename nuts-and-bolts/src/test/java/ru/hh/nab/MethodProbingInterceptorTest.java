package ru.hh.nab;

import com.google.inject.Module;
import com.google.inject.OutOfScopeException;
import com.google.inject.name.Names;
import java.util.Properties;
import org.junit.Test;
import ru.hh.nab.health.monitoring.OutOfRequestScope;
import ru.hh.nab.health.monitoring.Probe;
import ru.hh.nab.scopes.InScopeException;
import ru.hh.nab.scopes.RequestScope;
import ru.hh.nab.testing.JerseyTest;

public class MethodProbingInterceptorTest extends JerseyTest {
  @Override
  protected Properties settings() {
    return new Properties();
  }

  @Override
  protected Module module() {
    return new NabModule() {
      @Override
      protected void configureApp() {
        bindWithAnnotationMethodProbes(Probe.class, Service.class);
        bind(String.class).annotatedWith(Names.named("serviceName")).toInstance("serviceName");
      }
    };
  }

  public static class Service {
    @Probe(desc = "in request scope")
    public void doInRequestScope() {

    }

    @Probe(desc = "outside of request scope")
    @OutOfRequestScope
    public void doOutsideOfRequestScope() {

    }
  }

  @Test
  public void testOutOfRequestScope() throws Exception {
    Service service = injector().getInstance(Service.class);
    service.doOutsideOfRequestScope();
  }

  @Test
  public void testInRequestScope() throws Exception {
    RequestScope.enter(new RequestScope.RequestContext("request_id", "performer token", "uid", "remote address", null, null), "context");
    Service service = injector().getInstance(Service.class);
    service.doInRequestScope();
    RequestScope.leave();
  }

  @Test(expected = OutOfScopeException.class)
  public void test() throws Exception {
    Service service = injector().getInstance(Service.class);
    service.doInRequestScope();
  }

  @Test(expected = InScopeException.class)
  public void testOutOfRequestScopeFail() throws Exception {
    try {
      RequestScope.enter(new RequestScope.RequestContext("request_id", "performer token", "uid", "remote address", null, null), "context");
      Service service = injector().getInstance(Service.class);
      service.doOutsideOfRequestScope();
    } finally {
      RequestScope.leave();
    }
  }
}
