package ru.hh.nab.web.jersey;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import org.glassfish.jersey.server.ResourceConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.testbase.NabTestConfig;
import ru.hh.nab.testbase.ResourceHelper;
import ru.hh.nab.web.jersey.resolver.ObjectMapperContextResolver;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class JacksonTest {

  private final ResourceHelper resourceHelper;

  public JacksonTest(@LocalServerPort int serverPort) {
    this.resourceHelper = new ResourceHelper(serverPort);
  }

  @Test
  public void testJacksonJaxb() {
    var response = resourceHelper.createRequest("/").accept(APPLICATION_JSON).get();
    assertEquals("{\"string\":\"test\"}", response.readEntity(String.class));

    response = resourceHelper.createRequest("/0C").accept(APPLICATION_JSON).get();
    assertEquals("{\"string\":\"\uFFFD\"}", response.readEntity(String.class));

    response = resourceHelper.createRequest("/FFFE").accept(APPLICATION_JSON).get();
    assertEquals("{\"string\":\"\uFFFD\"}", response.readEntity(String.class));

    response = resourceHelper.createRequest("/0A").accept(APPLICATION_JSON).get();
    assertEquals("{\"string\":\"\\n\"}", response.readEntity(String.class));

    response = resourceHelper.createRequest("/special").accept(APPLICATION_JSON).get();
    assertEquals("{\"string\":\"&<\"}", response.readEntity(String.class));
  }

  @Configuration
  @EnableAutoConfiguration
  @Import({
      NabTestConfig.class,
      TestResource.class,
  })
  public static class TestConfiguration {

    @Bean
    public ResourceConfig resourceConfig() {
      return new ResourceConfig().register(ObjectMapperContextResolver.class);
    }
  }
}
