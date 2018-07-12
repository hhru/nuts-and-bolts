package ru.hh.nab.core;

import static java.text.MessageFormat.format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import static ru.hh.nab.core.NabApplication.registerSlf4JHandler;
import static ru.hh.nab.core.NabApplication.startJettyServer;
import ru.hh.nab.core.servlet.DefaultServletConfig;
import ru.hh.nab.core.servlet.ServletConfig;

import java.time.LocalDateTime;

/**
 * @deprecated use {@link NabApplication}
 */
@Deprecated
public abstract class Launcher {
  private static final Logger LOGGER = LoggerFactory.getLogger(Launcher.class);

  /**
   * @deprecated use {@link NabApplication#run(Class[])}
   */
  @Deprecated
  protected static void doMain(ApplicationContext context) {
    doMain(context, new DefaultServletConfig());
  }

  /**
   * @deprecated use {@link NabApplication#run(ServletConfig, Class[])}
   */
  @Deprecated
  protected static void doMain(ApplicationContext context, ServletConfig config) {
    registerSlf4JHandler();

    try {
      startJettyServer(context, config);
    } catch (Exception e) {
      LOGGER.error("Failed to start, shutting down", e);
      System.err.println(format("[{0}] Failed to start, shutting down: {1}", LocalDateTime.now(), e.getMessage()));
      System.exit(1);
    }
  }
}
