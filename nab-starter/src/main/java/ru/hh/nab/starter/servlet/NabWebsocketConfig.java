package ru.hh.nab.starter.servlet;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerEndpoint;
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.springframework.web.context.WebApplicationContext;

public class NabWebsocketConfig {

  public static Set<String> getAllowedPackages() {
    return Set.of("ru.hh");
  }

  public static void registerWebSockets(ServletContext servletContext, WebApplicationContext rootCtx) {
    try {
      new WebSocketServerContainerInitializer().onStartup(Set.of(), servletContext);
    } catch (ServletException e) {
      throw new RuntimeException(e);
    }

    javax.websocket.server.ServerContainer serverContainer = (ServerContainer) servletContext.getAttribute(
        WebSocketServerContainerInitializer.ATTR_JAVAX_SERVER_CONTAINER
    );

    List<? extends Class<?>> endpoints = rootCtx.getBeansWithAnnotation(ServerEndpoint.class).values().stream()
        .map(Object::getClass)
        .filter(clazz -> getAllowedPackages().stream().anyMatch(allowedPackage -> clazz.getName().startsWith(allowedPackage)))
        .collect(Collectors.toList());

    try {
      for (Class<?> endpoint : endpoints) {
        serverContainer.addEndpoint(endpoint);
      }
    } catch (DeploymentException e) {
      throw new RuntimeException(e);
    }

  }

}
