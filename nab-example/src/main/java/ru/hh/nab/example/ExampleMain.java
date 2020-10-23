package ru.hh.nab.example;

import com.sun.net.httpserver.HttpServer;
import ru.hh.nab.common.properties.PropertiesUtils;
import ru.hh.nab.starter.NabApplication;
import ru.hh.nab.starter.NabProdConfig;

import java.io.IOException;
import java.net.InetSocketAddress;

public class ExampleMain {

  public static void main(String[] args) throws IOException {
    //specify settings dir if its not currentDIr
    System.setProperty(PropertiesUtils.SETINGS_DIR_PROPERTY, "nab-example/src/etc");
    //you need to run consul agent to be able to run NaB application
    int consulPort = runConsulMock();
    //better to use settings with fixed port, but for the sake of dynamic usage we use env
    System.setProperty(NabProdConfig.CONSUL_PORT_ENV_KEY, String.valueOf(consulPort));
    NabApplication.builder()
        .configureJersey(ExampleJerseyConfig.class).bindToRoot()
        .build().run(ExampleConfig.class);
  }

  private static int runConsulMock() throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 1);
    server.createContext("/v1/agent/self", exchange -> exchange.sendResponseHeaders(200, 0));
    server.createContext("/v1/agent/service/register", exchange -> exchange.sendResponseHeaders(200, 0));
    server.setExecutor(null);
    server.start();
    return server.getAddress().getPort();
  }
}
