package ru.hh.nab.starter.jersey;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
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
public class JacksonTest {
  @HHJetty(port = 9009, overrideApplication = SpringCtxForJersey.class)
  ResourceHelper resourceHelper;

  @Test
  public void testJacksonJaxb() {
    var response = resourceHelper.createRequest("/").accept(APPLICATION_JSON).get();
    assertEquals("{\"string\":\"test\"}", response.readEntity(String.class));

    response = resourceHelper.createRequest("/0C").accept(APPLICATION_JSON).get();
    assertEquals("{\"string\":\"\uFFFD\"}", response.readEntity(String.class));

    response =resourceHelper. createRequest("/FFFE").accept(APPLICATION_JSON).get();
    assertEquals("{\"string\":\"\uFFFD\"}", response.readEntity(String.class));

    response = resourceHelper.createRequest("/0A").accept(APPLICATION_JSON).get();
    assertEquals("{\"string\":\"\\n\"}", response.readEntity(String.class));

    response = resourceHelper.createRequest("/special").accept(APPLICATION_JSON).get();
    assertEquals("{\"string\":\"&<\"}", response.readEntity(String.class));
  }

  @Configuration
  @Import(TestResource.class)
  public static class SpringCtxForJersey implements OverrideNabApplication {
    @Override
    public NabApplication getNabApplication() {
      return NabApplication.builder().configureJersey(SpringCtxForJersey.class)
          .registerResources(ObjectMapperContextResolver.class)
          .bindToRoot().build();
    }
  }
}
