package ru.hh.nab.starter;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import ru.hh.nab.testbase.NabTestConfig;
import ru.hh.nab.testbase.ResourceHelper;
import ru.hh.nab.testbase.extensions.NabJunitWebConfig;
import ru.hh.nab.testbase.extensions.NabTestServer;
import ru.hh.nab.testbase.extensions.OverrideNabApplication;

@NabJunitWebConfig(NabTestConfig.class)
public class ServletContextHandlerTest {
  private static final ServletContextListener listener = mock(ServletContextListener.class);
  private static ClassLoader cl = new ClassLoader(ServletContextHandlerTest.class.getClassLoader()) {
  };

  @NabTestServer(overrideApplication = ServletContextApplication.class)
  ResourceHelper resourceHelper;

  @Test
  public void testServletContextInitialization() {
    ArgumentCaptor<ServletContextEvent> eventCaptor = ArgumentCaptor.forClass(ServletContextEvent.class);
    verify(listener, times(1)).contextInitialized(eventCaptor.capture());
    ServletContext servletContext = eventCaptor.getValue().getServletContext();
    assertEquals("test", servletContext.getContextPath());
    assertEquals(cl, servletContext.getClassLoader());
  }

  public static class ServletContextApplication implements OverrideNabApplication {
    @Override
    public NabApplication getNabApplication() {
      return NabApplication
          .builder()
          .addListenerBean(ctx -> listener)
          .setContextPath("test")
          .setClassLoader(cl)
          .build();
    }
  }
}
