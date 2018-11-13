package ru.hh.nab.example;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(ExampleResource.class)
public class ExampleJerseyConfig {
}
