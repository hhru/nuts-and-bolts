package ru.hh.nab.example;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.starter.NabApplication;
import ru.hh.nab.starter.NabProdConfig;
import ru.hh.nab.starter.servlet.DefaultServletConfig;

@Configuration
@Import({NabProdConfig.class})
public class ExampleMain {

  public static void main(String[] args) {
    NabApplication.run(new DefaultServletConfig() {
      @Override
      public void registerResources(ResourceConfig resourceConfig) {
        resourceConfig.register(ExampleResource.class);
      }
    }, ExampleMain.class);
  }
}
