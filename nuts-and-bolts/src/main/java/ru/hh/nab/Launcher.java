package ru.hh.nab;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Stage;
import com.sun.jersey.spi.container.WebApplication;
import com.sun.jersey.spi.container.WebApplicationFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import mx4j.tools.adaptor.http.HttpAdaptor;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.slf4j.bridge.SLF4JBridgeHandler;
import ru.hh.nab.health.limits.LeakDetector;
import ru.hh.nab.health.limits.Limit;
import ru.hh.nab.health.limits.SimpleLimit;
import ru.hh.nab.security.PermissionLoader;
import ru.hh.nab.security.PropertiesPermissionLoader;

public class Launcher {
  static Module module;

  public static void main(String[] args) throws IOException {
    String settingsDir = System.getProperty("settingsDir");
    if (settingsDir != null) {
      System.setProperty("logback.configurationFile", new File(settingsDir, "logback.xml").getCanonicalPath());
    }
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();

    ArrayList<NabModule> modules = Lists.newArrayList(ServiceLoader.load(NabModule.class).iterator());
    if (modules.isEmpty()) {
      throw new IllegalStateException("No instances of " + NabModule.class.getName() + " found");
    }
    if (modules.size() > 1) {

      throw new IllegalStateException("Two or more instances of " + NabModule.class.getName() + " found: "
              + modules.stream().map(Object::getClass).map(Class::getName).collect(Collectors.joining(", ")));
    }
    main(Stage.PRODUCTION, modules.get(0), new SettingsModule());
  }

  public static Instance testMode(Stage stage, Module appModule, final Properties settings, final Properties apiSecurity, final Properties limits)
    throws IOException {
    return main(
      stage, appModule,
      new AbstractModule() {
        @Override
        protected void configure() {
        }

        @Provides
        @Named("settings.properties")
        @Singleton
        protected Properties settingsProperties() throws IOException {
          return settings;
        }

        @Provides
        @Singleton
        protected Settings settings() throws IOException {
          settings.setProperty("port", "0");
          return new Settings(settings);
        }

        @Provides
        @Singleton
        protected PermissionLoader permissionLoader() {
          return new PropertiesPermissionLoader(apiSecurity);
        }

        @Named("limits-with-names")
        @Provides
        @Singleton
        List<SettingsModule.LimitWithNameAndHisto> limitsWithNameAndHisto(LeakDetector leakDetector) throws IOException {
          List<SettingsModule.LimitWithNameAndHisto> ret = new ArrayList<>();

          for (String name : limits.stringPropertyNames()) {
            String property = limits.getProperty(name);
            int pos = property.indexOf(',');
            int max = Integer.parseInt(pos == -1 ? property : property.substring(0, pos));
            int warnThreshold = pos == -1 ? 0 : Integer.parseInt(property.substring(pos+1));
            Limit limit = new SimpleLimit(max, leakDetector, name, warnThreshold);
            ret.add(new SettingsModule.LimitWithNameAndHisto(limit, name, null));
          }
          return ret;
        }
      });
  }

  public static Instance main(Stage stage, Module appModule, Module settingsModule) throws IOException {
    module = appModule;

    try {
      final WebApplication wa = WebApplicationFactory.createWebApplication();
      final Injector inj = Guice.createInjector(stage,
        new JerseyModule(wa),
        appModule,
        new RequestScopeModule(),
        new JettyServerModule(),
        settingsModule);

      // start web server and find out actual listening port (for unit test instances)
      final Server jettyServer = inj.getInstance(Server.class);
      jettyServer.start();
      final int actualPort = ((ServerConnector) Arrays.asList(jettyServer.getConnectors()).stream()
        .filter(a -> a instanceof ServerConnector).findFirst().get()
      ).getLocalPort();
      HttpAdaptor adaptor = inj.getInstance(HttpAdaptor.class);
      if (adaptor != null) {
        adaptor.start();
      }

      return new Instance(inj, actualPort);
    } catch (IOException | RuntimeException ex) {
      Logger.getAnonymousLogger().log(Level.SEVERE, "boom", ex);
      throw ex;
    } catch (Exception ex) {
      Logger.getAnonymousLogger().log(Level.SEVERE, "boom", ex);
      throw new RuntimeException(ex);
    }
  }

  public static class Instance {
    public final Injector injector;
    public final int port;

    public Instance(Injector injector, int port) {
      this.injector = injector;
      this.port = port;
    }
  }
}
