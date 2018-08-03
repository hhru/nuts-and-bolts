package ru.hh.nab.example;

import org.glassfish.jersey.server.ResourceConfig;
import ru.hh.nab.starter.NabApplication;
import ru.hh.nab.starter.servlet.DefaultServletConfig;

public class ExampleMain {

  public static void main(String[] args) {
    NabApplication.run(new DefaultServletConfig() {
      @Override
      public void registerResources(ResourceConfig resourceConfig) {
        resourceConfig.register(ExampleResource.class);
      }
    }, ExampleConfig.class);
  }
}
