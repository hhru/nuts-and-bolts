package ru.hh.nab.web;

import jakarta.servlet.GenericServlet;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.PrintWriter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.testbase.NabTestConfig;
import ru.hh.nab.testbase.ResourceHelper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ServletTest {

  private  final ResourceHelper resourceHelper;

  public ServletTest(@LocalServerPort int serverPort) {
    this.resourceHelper = new ResourceHelper(serverPort);
  }

  @Test
  public void testServlet() {
    Response response = resourceHelper.executeGet("/springservlet");
    assertEquals("testValue", response.readEntity(String.class));
  }

  static final class TestServlet extends GenericServlet {
    private final String textValue;

    TestServlet(String textValue) {
      this.textValue = textValue;
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws IOException {
      try (PrintWriter writer = res.getWriter()) {
        writer.write(textValue);
      }
    }
  }

  @Configuration
  @EnableAutoConfiguration
  @Import(NabTestConfig.class)
  public static class Cfg {

    @Bean
    public ServletRegistrationBean<TestServlet> testServlet(FileSettings fileSettings) {
      TestServlet testServlet = new TestServlet(fileSettings.getString("customTestProperty"));
      return new ServletRegistrationBean<>(testServlet, "/springservlet");
    }
  }
}
