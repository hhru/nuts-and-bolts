package ru.hh.nab.web;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.testbase.NabTestConfig;

@Configuration
@EnableAutoConfiguration
@Import({NabTestConfig.class})
public class NabWebTestConfig {
}
