package ru.hh.nab.logging;

import ch.qos.logback.classic.LoggerContext;
import java.util.Properties;
import org.junit.Test;

public class NabLoggingConfiguratorTemplateTest {

  @Test(expected = AssertionError.class)
  public void testConfigure() {
    NabLoggingConfiguratorTemplate configurator = new NabLoggingConfiguratorTemplate() {
      @Override
      protected Properties createLoggingProperties() {
        return new Properties();
      }

      @Override
      public void configure(LoggingContextWrapper context) {
        HhMultiAppender requests = createAppender(context, "requests", () -> new HhMultiAppender(true));
        HhMultiAppender libraries = createAppender(context, "requests", () -> new HhMultiAppender(true));
      }

      @Override
      protected String getContextPropertyPrefix() {
        return "";
      }
    };
    LoggerContext lc = new LoggerContext();
    configurator.setContext(lc);
    configurator.configure(lc);
  }
}
