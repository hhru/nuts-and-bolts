package ru.hh.nab.starter;

import io.sentry.Sentry;
import jakarta.servlet.ServletContextEvent;
import java.lang.management.ManagementFactory;
import static java.text.MessageFormat.format;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.metrics.Tag;
import static ru.hh.nab.metrics.Tag.APP_TAG_NAME;
import ru.hh.nab.metrics.TaggedSender;
import ru.hh.nab.starter.logging.LogLevelOverrideApplier;
import ru.hh.nab.starter.logging.LogLevelOverrideExtension;
import ru.hh.nab.starter.server.jetty.JettyLifeCycleListener;
import ru.hh.nab.starter.server.jetty.JettyServer;
import ru.hh.nab.starter.server.jetty.JettyServerFactory;
import static ru.hh.nab.starter.server.jetty.JettyServerFactory.createWebAppContextHandler;
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
    return run(baseContext, false, true);
  }

  /**
   *
   * @param directlyUseAsWebAppRoot if this context used directly it gets no initialization by initializing listener
   * {@link ContextLoader#configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext, jakarta.servlet.ServletContext)}
   */
  public JettyServer run(WebApplicationContext baseContext, boolean directlyUseAsWebAppRoot, boolean exitOnError) {
    try {
      configureLogger(baseContext);
      configureSentry(baseContext);
      JettyServer jettyServer = createJettyServer(baseContext, directlyUseAsWebAppRoot);
      jettyServer.start();
      logStartupInfo(baseContext);
      return jettyServer;
    } catch (Exception e) {
      return logErrorAndExit(e, exitOnError);
    }
  }

  public JettyServer runOnTestServer(JettyServerFactory.JettyTestServer testServer, WebApplicationContext baseContext, boolean raiseIfServerInited) {
    try {
      configureLogger(baseContext);
      logStartupInfo(baseContext);
      WebAppInitializer webAppInitializer = createWebAppInitializer(servletContextConfig, baseContext, false);
      ServletContextHandler jettyWebAppContext = createWebAppContextHandler(new FileSettings(new Properties()), List.of(webAppInitializer));
      return testServer.loadServerIfNeeded(jettyWebAppContext, raiseIfServerInited);
    } catch (Exception e) {
      return logErrorAndExit(e, false);
    }
  }

  JettyServer createJettyServer(WebApplicationContext baseContext, boolean directlyUseAsWebAppRoot) {
    return createJettyServer(
        baseContext,
        directlyUseAsWebAppRoot,
        webAppContext -> webAppContext.getServer().addEventListener(new JettyLifeCycleListener(baseContext))
    );
  }

  private JettyServer createJettyServer(WebApplicationContext baseContext,
                                boolean directlyUseAsWebAppRoot,
                                WebAppInitializer extwebAppInitializer){
    FileSettings fileSettings = baseContext.getBean(FileSettings.class);
    ThreadPool threadPool = baseContext.getBean(ThreadPool.class);
    StatsDSender sender = baseContext.getBean(StatsDSender.class);
    WebAppInitializer webAppInitializer = createWebAppInitializer(servletContextConfig, baseContext, directlyUseAsWebAppRoot);
    TaggedSender appSender = new TaggedSender(sender, Set.of(new Tag(APP_TAG_NAME, baseContext.getBean(SERVICE_NAME, String.class))));
    return JettyServerFactory.create(fileSettings, threadPool, appSender, List.of(webAppInitializer, extwebAppInitializer));
  }

  public static NabApplicationBuilder builder() {
    return new NabApplicationBuilder();
  }

  public static void configureSentry(ApplicationContext context) {
    FileSettings settings = context.getBean(FileSettings.class);
    String dsn = settings.getString("sentry.dsn");
    if (StringUtils.isBlank(dsn)) {
      LOGGER.warn("Sentry DSN is empty!");
    } else {
      Sentry.init(options -> {
        options.setEnableExternalConfiguration(true);
        options.setDsn(dsn);
      });
    }
  }

  private static void configureLogger(ApplicationContext context) {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();

    getLogLevelOverrideExtension(context).ifPresent(extension -> new LogLevelOverrideApplier().run(extension, context.getBean(FileSettings.class)));
  }

  private static void logStartupInfo(ApplicationContext context) {
    AppMetadata appMetadata = context.getBean(AppMetadata.class);
    LOGGER.info("Started {} PID={} (version={})", appMetadata.getServiceName(), getCurrentPid(), appMetadata.getVersion());
  }

  private static String getCurrentPid() {
    return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
  }

  private static Optional<LogLevelOverrideExtension> getLogLevelOverrideExtension(ApplicationContext context) {
    try {
      var extension = context.getBean(LogLevelOverrideExtension.class);
      LOGGER.info("{} activated", LogLevelOverrideExtension.class.getSimpleName());
      return Optional.of(extension);
    } catch (NoSuchBeanDefinitionException e) {
      // Extension not activated, normal behaviour
      return Optional.empty();
    }
  }

  private static <T> T logErrorAndExit(Exception e, boolean exitOnError) {
    try {
      LOGGER.error("Failed to start, shutting down", e);
      System.err.println(format("[{0}] Failed to start, shutting down: {1}", LocalDateTime.now(), ExceptionUtils.getRootCause(e).getMessage()));
      return null;
    } finally {
      if (exitOnError) {
        System.exit(1);
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  private static WebAppInitializer createWebAppInitializer(NabServletContextConfig servletContextConfig,
                                                           WebApplicationContext baseCtx,
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
    if (baseCtx.getServletContext() != null) {
      webAppRootCtx.setServletContext(baseCtx.getServletContext());
    }
    return webAppRootCtx;
  }

}
