package ru.hh.nab.logging;

import ch.qos.logback.classic.LoggerContext;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

public class NabLoggingConfiguratorTemplateTest {

  @Test
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
    };
    LoggerContext lc = new LoggerContext();
    configurator.setContext(lc);
    assertThrows(AssertionError.class, () -> configurator.configure(lc));
  }
}
