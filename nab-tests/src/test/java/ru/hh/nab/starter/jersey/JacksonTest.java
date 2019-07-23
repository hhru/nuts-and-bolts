package ru.hh.nab.starter.jersey;

import org.junit.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import ru.hh.nab.starter.NabApplication;
import ru.hh.nab.testbase.NabTestBase;
import ru.hh.nab.testbase.NabTestConfig;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;

@ContextConfiguration(classes = {NabTestConfig.class})
public class JacksonTest extends NabTestBase {
  @Override
  protected NabApplication getApplication() {
    return NabApplication.builder().configureJersey(SpringCtxForJersey.class)
      .registerResources(ObjectMapperContextResolver.class)
      .bindToRoot().build();
  }

  @Test
  public void testJacksonJaxb() {
    var response = createRequest("/").accept(APPLICATION_JSON).get();
    assertEquals("{\"string\":\"test\"}", response.readEntity(String.class));

    response = createRequest("/0C").accept(APPLICATION_JSON).get();
    assertEquals("{\"string\":\"\uFFFD\"}", response.readEntity(String.class));

    response = createRequest("/FFFE").accept(APPLICATION_JSON).get();
    assertEquals("{\"string\":\"\uFFFD\"}", response.readEntity(String.class));

    response = createRequest("/0A").accept(APPLICATION_JSON).get();
    assertEquals("{\"string\":\"\\n\"}", response.readEntity(String.class));

    response = createRequest("/special").accept(APPLICATION_JSON).get();
    assertEquals("{\"string\":\"&<\"}", response.readEntity(String.class));
  }

  @Configuration
  @Import(TestResource.class)
  static class SpringCtxForJersey {
  }
}
