package ru.hh.nab.starter;

import jakarta.servlet.ServletContextEvent;
import static java.text.MessageFormat.format;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import ru.hh.nab.common.properties.FileSettings;
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
   * @param directlyUseAsWebAppRoot if this context used directly it gets no initialization by initializing listener
   * {@link ContextLoader#configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext, jakarta.servlet.ServletContext)}
   */
  public JettyServer run(WebApplicationContext baseContext, boolean directlyUseAsWebAppRoot, boolean exitOnError) {
    try {
      JettyServer jettyServer = createJettyServer(baseContext, directlyUseAsWebAppRoot);
      jettyServer.start();
      return jettyServer;
    } catch (Exception e) {
      return logErrorAndExit(e, exitOnError);
    }
  }

  public JettyServer runOnTestServer(JettyServerFactory.JettyTestServer testServer, WebApplicationContext baseContext, boolean raiseIfServerInited) {
    try {
      WebAppInitializer webAppInitializer = createWebAppInitializer(servletContextConfig, baseContext, false);
      ServletContextHandler jettyWebAppContext = createWebAppContextHandler(List.of(webAppInitializer));
      return testServer.loadServerIfNeeded(jettyWebAppContext, raiseIfServerInited);
    } catch (Exception e) {
      return logErrorAndExit(e, false);
    }
  }

  private JettyServer createJettyServer(
      WebApplicationContext baseContext,
      boolean directlyUseAsWebAppRoot
  ) {
    FileSettings fileSettings = baseContext.getBean(FileSettings.class);
    WebAppInitializer webAppInitializer = createWebAppInitializer(servletContextConfig, baseContext, directlyUseAsWebAppRoot);
    return JettyServerFactory.create(fileSettings, List.of(webAppInitializer));
  }

  public static NabApplicationBuilder builder() {
    return new NabApplicationBuilder();
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

  private static WebAppInitializer createWebAppInitializer(
      NabServletContextConfig servletContextConfig,
      WebApplicationContext baseCtx,
      boolean directWebappRoot
  ) {
    WebApplicationContext targetCtx = directWebappRoot ? baseCtx : createChildWebAppCtx(baseCtx);
    return webApp -> {
      servletContextConfig.preConfigureWebApp(webApp, baseCtx);
      webApp.addEventListener(new ContextLoaderListener(targetCtx) {
        @Override
        public void contextInitialized(ServletContextEvent event) {
          super.contextInitialized(event);
          servletContextConfig.onWebAppStarted(event.getServletContext(), targetCtx);
        }

        @Override
        public void contextDestroyed(ServletContextEvent event) {
          super.contextDestroyed(event);
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
