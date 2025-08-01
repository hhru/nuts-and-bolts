package ru.hh.nab.web.jersey.resolver;

import jakarta.inject.Inject;
import java.util.Properties;
import org.glassfish.jersey.server.ResourceConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.http.RequestEntity.get;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.web.NabWebTestConfig;
import static ru.hh.nab.web.NabWebTestConfig.TEST_SERVICE_NAME;

@SpringBootTest(classes = XmlTest.TestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class XmlTest {

  @Inject
  private TestRestTemplate testRestTemplate;

  @Test
  public void testFilteredXML() {
    var dtoResponse = testRestTemplate.exchange(get("/").accept(APPLICATION_XML).build(), TestResource.DTO.class);
    assertEquals("test", dtoResponse.getBody().string);

    dtoResponse = testRestTemplate.exchange(get("/0C").accept(APPLICATION_XML).build(), TestResource.DTO.class);
    assertEquals("\uFFFD", dtoResponse.getBody().string);

    dtoResponse = testRestTemplate.exchange(get("/FFFE").accept(APPLICATION_XML).build(), TestResource.DTO.class);
    assertEquals("\uFFFD", dtoResponse.getBody().string);

    var strResponse = testRestTemplate.exchange(get("/special").accept(APPLICATION_XML).build(), String.class);
    assertEquals(
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><dto><string>&amp;&lt;</string></dto>",
        strResponse.getBody()
    );

    strResponse = testRestTemplate.exchange(get("/0A").accept(APPLICATION_XML).build(), String.class);
    assertEquals(
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><dto><string>\n</string></dto>",
        strResponse.getBody()
    );
  }

  @Configuration
  @Import(NabWebTestConfig.class)
  public static class TestConfiguration {

    @Bean
    public ResourceConfig resourceConfig() {
      MarshallerContextResolver contextResolver = new MarshallerContextResolver(
          new Properties(),
          TEST_SERVICE_NAME,
          mock(StatsDSender.class)
      );

      ResourceConfig resourceConfig = new ResourceConfig();
      resourceConfig.register(TestResource.class);
      resourceConfig.register(contextResolver);
      return resourceConfig;
    }
  }
}
