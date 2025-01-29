package ru.hh.nab.web.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import ru.hh.nab.web.NabWebTestConfig;
import static ru.hh.nab.web.NabWebTestConfig.TEST_SERVICE_NAME;
import static ru.hh.nab.web.NabWebTestConfig.TEST_SERVICE_VERSION;

@SpringBootTest(classes = NabWebTestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class StatusResourceTest {

  @Inject
  private TestRestTemplate testRestTemplate;

  @Test
  public void testStatusResponse() {
    ResponseEntity<Project> response = testRestTemplate.getForEntity("/status", Project.class);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode().value());
    Project project = response.getBody();
    assertEquals(TEST_SERVICE_NAME, project.name);
    assertEquals(TEST_SERVICE_VERSION, project.version);
    assertTrue(project.uptime > 0);
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
