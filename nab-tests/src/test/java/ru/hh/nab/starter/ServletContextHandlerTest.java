package ru.hh.nab.starter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.ContextConfiguration;
import ru.hh.nab.testbase.NabTestBase;
import ru.hh.nab.testbase.NabTestConfig;

@ContextConfiguration(classes = {NabTestConfig.class})
public class ServletContextHandlerTest extends NabTestBase {

  private ServletContextListener listener;
  private ClassLoader cl = new ClassLoader(getClass().getClassLoader()) {};

  @Override
  protected NabApplication getApplication() {
    listener = mock(ServletContextListener.class);
    return NabApplication.builder()
      .addListenerBean(ctx -> listener)
      .setContextPath("test")
      .setClassLoader(cl)
      .build();
  }

  @Test
  public void testServletContextInitialization() {
    ArgumentCaptor<ServletContextEvent> eventCaptor = ArgumentCaptor.forClass(ServletContextEvent.class);
    verify(listener, times(1)).contextInitialized(eventCaptor.capture());
    ServletContext servletContext = eventCaptor.getValue().getServletContext();
    assertEquals("test", servletContext.getContextPath());
    assertEquals(cl, servletContext.getClassLoader());
  }
}
