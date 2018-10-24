package ru.hh.nab.starter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ru.hh.nab.testbase.NabTestBase;
import ru.hh.nab.testbase.NabTestConfig;
import static org.junit.Assert.assertEquals;

@ContextConfiguration(classes = {NabTestConfig.class})
public class ServletTest extends NabTestBase {

  @Override
  protected NabApplication getApplication() {
    return NabApplication.builder().addServlet(ctx -> ctx.getBean(TestServlet.class), Cfg.class).bindTo("/springservlet").build();
  }

  @Test
  public void testServlet() {
    Response response = executeGet("/springservlet");
    assertEquals("testValue", response.readEntity(String.class));
  }

  static final class TestServlet extends GenericServlet {

    private final String textValue;

    TestServlet(String textValue) {
      this.textValue = textValue;
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
      try (PrintWriter writer = res.getWriter()) {
        writer.write(textValue);
      }
    }
  }

  @Configuration
  static class Cfg {
    @Bean
    TestServlet testServlet(Properties serviceProperties) {
      return new TestServlet(serviceProperties.getProperty("customTestProperty"));
    }
  }
}
