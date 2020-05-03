package ru.hh.nab.starter.jersey;

import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import ru.hh.nab.starter.NabApplication;
import ru.hh.nab.testbase.NabTestConfig;
import ru.hh.nab.testbase.ResourceHelper;
import ru.hh.nab.testbase.extensions.HHJetty;
import ru.hh.nab.testbase.extensions.HHJettyExtension;
import ru.hh.nab.testbase.extensions.OverrideNabApplication;

@ExtendWith({
    HHJettyExtension.class,
})
@SpringJUnitWebConfig({
    NabTestConfig.class
})
public class XmlTest {

  @HHJetty(port = 9008, overrideApplication = SpringCtxForJersey.class)
  ResourceHelper resourceHelper;

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
  @Import(TestResource.class)
  public static class SpringCtxForJersey implements OverrideNabApplication {
    @Override
    public NabApplication getNabApplication() {
      return NabApplication.builder().configureJersey(SpringCtxForJersey.class).bindToRoot().build();
    }
  }
}
