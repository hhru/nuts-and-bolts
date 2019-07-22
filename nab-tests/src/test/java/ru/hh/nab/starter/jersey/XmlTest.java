package ru.hh.nab.starter.jersey;

import org.junit.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import ru.hh.nab.starter.NabApplication;
import ru.hh.nab.testbase.NabTestBase;
import ru.hh.nab.testbase.NabTestConfig;

import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static org.junit.Assert.assertEquals;

@ContextConfiguration(classes = {NabTestConfig.class})
public class XmlTest extends NabTestBase {
  @Override
  protected NabApplication getApplication() {
    return NabApplication.builder().configureJersey(SpringCtxForJersey.class).bindToRoot().build();
  }

  @Test
  public void testFilteredXML() {
    var response = createRequest("/").accept(APPLICATION_XML).get();
    assertEquals("test", response.readEntity(TestResource.DTO.class).string);

    response = createRequest("/0C").accept(APPLICATION_XML).get();
    assertEquals("\uFFFD", response.readEntity(TestResource.DTO.class).string);

    response = createRequest("/FFFE").accept(APPLICATION_XML).get();
    assertEquals("\uFFFD", response.readEntity(TestResource.DTO.class).string);

    response = createRequest("/special").accept(APPLICATION_XML).get();
    assertEquals(
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><dto><string>&amp;&lt;</string></dto>",
      response.readEntity(String.class)
    );

    response = createRequest("/0A").accept(APPLICATION_XML).get();
    assertEquals(
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><dto><string>\n</string></dto>",
      response.readEntity(String.class)
    );
  }

  @Configuration
  @Import(TestResource.class)
  static class SpringCtxForJersey {
  }
}
