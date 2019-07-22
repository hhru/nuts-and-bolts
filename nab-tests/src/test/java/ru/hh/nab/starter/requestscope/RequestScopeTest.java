package ru.hh.nab.starter.requestscope;

import org.junit.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import ru.hh.nab.starter.NabApplication;
import ru.hh.nab.testbase.NabTestBase;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;

@ContextConfiguration(classes = {RequestConfig.class})
public class RequestScopeTest extends NabTestBase {

  @Inject
  private Provider<RequestDetails> requestProvider;

  @Test
  public void requestScopeTest() {

    final String name = requestProvider.get().getField();
    Response response = target("/hello")
        .queryParam("name", name)
        .request()
        .get();
    assertEquals(OK.getStatusCode(), response.getStatus());
    assertEquals(String.format("Hello, %s!", name), response.readEntity(String.class));
  }

  @Override
  protected NabApplication getApplication() {
    return NabApplication
        .builder()
        .configureJersey(SpringCtxForJersey.class)
        .bindToRoot()
        .build();
  }

  @Configuration
  @Import(TestResource.class)
  static class SpringCtxForJersey {
  }
}
