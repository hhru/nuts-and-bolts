package ru.hh.nab.web.jersey;

import jakarta.inject.Inject;
import org.glassfish.jersey.server.ResourceConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.RequestEntity.get;
import ru.hh.nab.web.NabWebTestConfig;
import ru.hh.nab.web.jersey.resolver.ObjectMapperContextResolver;

@SpringBootTest(classes = JacksonTest.TestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class JacksonTest {

  @Inject
  private TestRestTemplate testRestTemplate;

  @Test
  public void testJacksonJaxb() {
    var response = testRestTemplate.exchange(get("/").accept(APPLICATION_JSON).build(), String.class);
    assertEquals("{\"string\":\"test\"}", response.getBody());

    response = testRestTemplate.exchange(get("/0C").accept(APPLICATION_JSON).build(), String.class);
    assertEquals("{\"string\":\"\uFFFD\"}", response.getBody());

    response = testRestTemplate.exchange(get("/FFFE").accept(APPLICATION_JSON).build(), String.class);
    assertEquals("{\"string\":\"\uFFFD\"}", response.getBody());

    response = testRestTemplate.exchange(get("/0A").accept(APPLICATION_JSON).build(), String.class);
    assertEquals("{\"string\":\"\\n\"}", response.getBody());

    response = testRestTemplate.exchange(get("/special").accept(APPLICATION_JSON).build(), String.class);
    assertEquals("{\"string\":\"&<\"}", response.getBody());
  }

  @Configuration
  @Import(NabWebTestConfig.class)
  public static class TestConfiguration {

    @Bean
    public ResourceConfig resourceConfig() {
      ResourceConfig resourceConfig = new ResourceConfig();
      resourceConfig.register(ObjectMapperContextResolver.class);
      resourceConfig.register(TestResource.class);
      return resourceConfig;
    }
  }
}
