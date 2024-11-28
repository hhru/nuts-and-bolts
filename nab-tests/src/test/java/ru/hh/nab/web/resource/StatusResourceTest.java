package ru.hh.nab.web.resource;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.hh.nab.testbase.NabTestConfig;
import ru.hh.nab.testbase.web.WebTestBase;
import ru.hh.nab.web.NabWebTestConfig;

@SpringBootTest(classes = NabWebTestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class StatusResourceTest extends WebTestBase {

  @Test
  public void testStatusResponse() {
    try (Response response = resourceHelper.createRequest("/status").build(HttpMethod.GET).invoke()) {
      assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
      Project project = response.readEntity(Project.class);
      assertEquals(NabTestConfig.TEST_SERVICE_NAME, project.name);
      assertEquals(NabTestConfig.TEST_SERVICE_VERSION, project.version);
      assertTrue(project.uptime > 0);
    }
  }

  @XmlRootElement
  private static final class Project {
    @XmlAttribute
    private String name;
    @XmlElement
    private String version;
    @XmlElement
    private long uptime;
  }
}
