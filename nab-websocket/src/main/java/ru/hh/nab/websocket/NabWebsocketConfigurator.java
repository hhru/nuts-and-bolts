package ru.hh.nab.websocket;

import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpoint;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import ru.hh.nab.starter.NabApplicationBuilder;

public class NabWebsocketConfigurator {

  public static void configureWebsocket(NabApplicationBuilder nabApplicationBuilder, Collection<String> allowedEndpointsPackages) {
    nabApplicationBuilder.configureWebapp((jettyWebAppContext, springWebApplicationContext) ->
        jettyWebAppContext.getServletContext().setExtendedListenerTypes(true));

    nabApplicationBuilder.onWebAppStarted((servletContext, springWebApplicationContext) -> {
      try {
        new JettyWebSocketServletContainerInitializer().onStartup(Set.of(), servletContext);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      jakarta.websocket.server.ServerContainer serverContainer = (ServerContainer) servletContext.getAttribute(
          jakarta.websocket.server.ServerContainer.class.getName()
      );

      List<? extends Class<?>> endpoints = springWebApplicationContext.getBeansWithAnnotation(ServerEndpoint.class).values().stream()
          .map(Object::getClass)
          .filter(clazz -> allowedEndpointsPackages.stream().anyMatch(allowedPackage -> clazz.getName().startsWith(allowedPackage)))
          .collect(Collectors.toList());

      if (endpoints.isEmpty()) {
        throw new IllegalStateException(String.format("Can not configure websocket - no %s classes found", ServerEndpoint.class.getName()));
      }

      try {
        for (Class<?> endpoint : endpoints) {
          serverContainer.addEndpoint(endpoint);
        }
      } catch (DeploymentException e) {
        throw new RuntimeException(e);
      }

    });
  }

}
