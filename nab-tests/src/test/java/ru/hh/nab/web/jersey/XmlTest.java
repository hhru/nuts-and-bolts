package ru.hh.nab.web.jersey;

import static jakarta.ws.rs.core.MediaType.APPLICATION_XML;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.hh.nab.testbase.web.WebTestBase;
import ru.hh.nab.web.NabWebTestConfig;

@SpringBootTest(classes = {NabWebTestConfig.class, TestResource.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class XmlTest extends WebTestBase {

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
}
