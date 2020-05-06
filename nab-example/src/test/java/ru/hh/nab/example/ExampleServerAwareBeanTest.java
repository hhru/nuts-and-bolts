package ru.hh.nab.example;

import javax.ws.rs.core.Response;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.starter.NabApplication;
import ru.hh.nab.testbase.NabTestConfig;
import ru.hh.nab.testbase.ResourceHelper;
import ru.hh.nab.testbase.extensions.NabJunitWebConfig;
import ru.hh.nab.testbase.extensions.NabTestServer;
import ru.hh.nab.testbase.extensions.OverrideNabApplication;

@NabJunitWebConfig(NabTestConfig.class)
public class ExampleServerAwareBeanTest {

  @NabTestServer(overrideApplication = SpringCtxForJersey.class)
  ResourceHelper resourceHelper;

  @Test
  public void testBeanWithNabTestContext() {
    try (Response response = resourceHelper.createRequestFromAbsoluteUrl(resourceHelper.baseUrl() + "/hello").get()) {
      assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
      assertEquals("Hello, world!", response.readEntity(String.class));
    }
  }

  @Configuration
  @Import(ExampleResource.class)
  public static class SpringCtxForJersey implements OverrideNabApplication {
    @Override
    public NabApplication getNabApplication() {
      return NabApplication.builder().configureJersey(SpringCtxForJersey.class).bindToRoot().build();
    }
  }
}
