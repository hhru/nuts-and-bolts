package ru.hh.nab.starter;

import static java.text.MessageFormat.format;
import static ru.hh.nab.starter.server.jetty.JettyServer.JETTY_PORT;

import io.sentry.Sentry;
import java.util.Properties;
import java.util.function.Function;
import javax.servlet.ServletContextEvent;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import ru.hh.nab.common.properties.FileSettings;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import ru.hh.nab.starter.server.jetty.JettyServer;
import ru.hh.nab.starter.server.jetty.JettyServerFactory;
import ru.hh.nab.starter.servlet.WebAppInitializer;
import ru.hh.nab.starter.spring.HierarchicalWebApplicationContext;

public final class NabApplication {
  private static final Logger LOGGER = LoggerFactory.getLogger(NabApplication.class);

  private final NabServletContextConfig servletContextConfig;

  NabApplication(NabServletContextConfig servletContextConfig) {
    this.servletContextConfig = servletContextConfig;
  }

  public static JettyServer runWebApp(NabServletContextConfig servletContextConfig, Class<?>... configurationClasses) {
    return new NabApplication(servletContextConfig).run(configurationClasses);
  }

  public JettyServer run(Class<?>... configs) {
    AnnotationConfigWebApplicationContext aggregateCtx = new AnnotationConfigWebApplicationContext();
    try {
      aggregateCtx.register(configs);
      aggregateCtx.refresh();
      return run(aggregateCtx);
    } catch (Exception e) {
      return logErrorAndExit(e, true);
    }
  }

  public JettyServer run(WebApplicationContext baseContext) {
    return run(baseContext, false, serverCreateFunction -> serverCreateFunction.apply(null), true);
  }

  /**
   *
   * @param directlyUseAsWebAppRoot if this context used directly it gets no initialization by initializing listener
   * {@link ContextLoader#configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext, javax.servlet.ServletContext)}
   * @param serverStarter function which allows to synchronize server start with external action
   */
  public JettyServer run(WebApplicationContext baseContext,
      boolean directlyUseAsWebAppRoot,
      Function<Function<Integer, JettyServer>, JettyServer> serverStarter,
      boolean exitOnError) {
    try {
      configureLogger();
      configureSentry(baseContext);
      FileSettings fileSettings = baseContext.getBean(FileSettings.class);
      ThreadPool threadPool = baseContext.getBean(ThreadPool.class);
      WebAppInitializer webAppInitializer = createWebAppInitializer(servletContextConfig, baseContext, directlyUseAsWebAppRoot);
      JettyServer jettyServer = serverStarter.apply(port -> {
        FileSettings effectiveSettings = fileSettings;
        if (port != null) {
          Properties properties = fileSettings.getProperties();
          properties.setProperty(JETTY_PORT, String.valueOf(port));
          effectiveSettings = new FileSettings(properties);
        }
        JettyServer server = JettyServerFactory.create(effectiveSettings, threadPool, webAppInitializer);
        server.start();
        return server;
      });
      logStartupInfo(baseContext);
      return jettyServer;
    } catch (Exception e) {
      return logErrorAndExit(e, exitOnError);
    }
  }

  public static NabApplicationBuilder builder() {
    return new NabApplicationBuilder();
  }

  public static void configureLogger() {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  public static void configureSentry(ApplicationContext context) {
    FileSettings settings = context.getBean(FileSettings.class);
    Sentry.init(settings.getString("sentry.dsn"));
  }

  private static void logStartupInfo(ApplicationContext context) {
    AppMetadata appMetadata = context.getBean(AppMetadata.class);
    LOGGER.info("Started {} PID={} (version={})", appMetadata.getServiceName(), getCurrentPid(), appMetadata.getVersion());
  }

  private static String getCurrentPid() {
    return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
  }

  private static <T> T logErrorAndExit(Exception e, boolean exitOnError) {
    try {
      LOGGER.error("Failed to start, shutting down", e);
      System.err.println(format("[{0}] Failed to start, shutting down: {1}", LocalDateTime.now(), e.getMessage()));
      return null;
    } finally {
      if (exitOnError) {
        System.exit(1);
      }
    }
  }

  private static WebAppInitializer createWebAppInitializer(NabServletContextConfig servletContextConfig, WebApplicationContext baseCtx,
      boolean directWebappRoot) {
    WebApplicationContext targetCtx = directWebappRoot ? baseCtx : createChildWebAppCtx(baseCtx);
    return webApp -> {
      servletContextConfig.preConfigureWebApp(webApp, baseCtx);
      webApp.addEventListener(new ContextLoaderListener(targetCtx) {
        @Override
        public void contextInitialized(ServletContextEvent event) {
          super.contextInitialized(event);
          servletContextConfig.onWebAppStarted(event.getServletContext(), targetCtx);
          servletContextConfig.getListeners(targetCtx).forEach(listener -> listener.contextInitialized(event));
        }

        @Override
        public void contextDestroyed(ServletContextEvent event) {
          super.contextDestroyed(event);
          servletContextConfig.getListeners(targetCtx).forEach(listener -> listener.contextDestroyed(event));
          if (baseCtx instanceof ConfigurableApplicationContext) {
            ((ConfigurableApplicationContext) baseCtx).close();
          }
        }
      });
    };
  }

  private static AnnotationConfigWebApplicationContext createChildWebAppCtx(WebApplicationContext baseCtx) {
    AnnotationConfigWebApplicationContext webAppRootCtx = new HierarchicalWebApplicationContext(baseCtx);
    webAppRootCtx.setParent(baseCtx);
    webAppRootCtx.registerShutdownHook();
    if (baseCtx.getServletContext() != null) {
      webAppRootCtx.setServletContext(baseCtx.getServletContext());
    }
    return webAppRootCtx;
  }

}
