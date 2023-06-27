package ru.hh.nab.websocket;

import jakarta.websocket.server.ServerEndpoint;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.eclipse.jetty.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.eclipse.jetty.websocket.jakarta.server.internal.JakartaWebSocketServerContainer;
import ru.hh.nab.starter.NabApplicationBuilder;

public class NabWebsocketConfigurator {

  public static void configureWebsocket(NabApplicationBuilder nabApplicationBuilder, Collection<String> allowedEndpointsPackages) {
    nabApplicationBuilder.configureWebapp((jettyWebAppContext, springWebApplicationContext) ->
        jettyWebAppContext.getServletContext().setExtendedListenerTypes(true));

    nabApplicationBuilder.onWebAppStarted((servletContext, springWebApplicationContext) -> {
      try {
        new JakartaWebSocketServletContainerInitializer().onStartup(Set.of(), servletContext);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      JakartaWebSocketServerContainer serverContainer = JakartaWebSocketServerContainer.getContainer(servletContext);

      List<? extends Class<?>> beans = springWebApplicationContext.getBeansWithAnnotation(ServerEndpoint.class).values().stream()
          .map(Object::getClass)
          .filter(clazz -> allowedEndpointsPackages.stream().anyMatch(allowedPackage -> clazz.getName().startsWith(allowedPackage)))
          .toList();

      if (beans.isEmpty()) {
        throw new IllegalStateException(String.format("Can not configure websocket - no %s classes found", ServerEndpoint.class.getName()));
      }

      try {
        for (Class<?> bean : beans) {
          serverContainer.addEndpoint(bean);
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

    });
  }

}
