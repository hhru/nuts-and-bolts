package ru.hh.nab.starter.jersey;

import static jakarta.ws.rs.core.MediaType.APPLICATION_XML;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.testbase.NabTestConfig;
import ru.hh.nab.testbase.ResourceHelper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class XmlTest {

  private final ResourceHelper resourceHelper;

  public XmlTest(@LocalServerPort int serverPort) {
    this.resourceHelper = new ResourceHelper(serverPort);
  }

  @Test
  public void testFilteredXML() {
    var response = resourceHelper.createRequest("/").accept(APPLICATION_XML).get();
    assertEquals("test", response.readEntity(TestResource.DTO.class).string);

    response = resourceHelper.createRequest("/0C").accept(APPLICATION_XML).get();
    assertEquals("\uFFFD", response.readEntity(TestResource.DTO.class).string);

    response = resourceHelper.createRequest("/FFFE").accept(APPLICATION_XML).get();
    assertEquals("\uFFFD", response.readEntity(TestResource.DTO.class).string);

    response = resourceHelper.createRequest("/special").accept(APPLICATION_XML).get();
    assertEquals(
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><dto><string>&amp;&lt;</string></dto>",
        response.readEntity(String.class)
    );

    response = resourceHelper.createRequest("/0A").accept(APPLICATION_XML).get();
    assertEquals(
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><dto><string>\n</string></dto>",
        response.readEntity(String.class)
    );
  }

  @Configuration
  @EnableAutoConfiguration
  @Import({
      NabTestConfig.class,
      TestResource.class,
  })
  public static class TestConfiguration {
  }
}
