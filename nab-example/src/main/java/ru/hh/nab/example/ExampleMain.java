package ru.hh.nab.example;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.core.NabApplication;

@Configuration
@Import(ExampleProdConfig.class)
public class ExampleMain {

  public static void main(String[] args) {
    NabApplication.run(ExampleMain.class);
  }
}
