package ru.hh.nab.websocket;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerEndpoint;
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import ru.hh.nab.starter.NabApplicationBuilder;

public class NabWebsocketConfigurator {

  public static void configureWebsocket(NabApplicationBuilder nabApplicationBuilder, Collection<String> allowedEndpointsPackages) {
    nabApplicationBuilder.configureWebapp((jettyWebAppContext, springWebApplicationContext) ->
        jettyWebAppContext.getServletContext().setExtendedListenerTypes(true));

    nabApplicationBuilder.onWebAppStarted((servletContext, springWebApplicationContext) -> {
      try {
        new WebSocketServerContainerInitializer().onStartup(Set.of(), servletContext);
      } catch (ServletException e) {
        throw new RuntimeException(e);
      }

      javax.websocket.server.ServerContainer serverContainer = (ServerContainer) servletContext.getAttribute(
          WebSocketServerContainerInitializer.ATTR_JAVAX_SERVER_CONTAINER
      );

      List<? extends Class<?>> endpoints = springWebApplicationContext
          .getBeansWithAnnotation(ServerEndpoint.class)
          .values()
          .stream()
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
