package ru.hh.nab.example;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
    WebsocketEchoEndpoint.class,
    WebsocketSessionsHandler.class
})
public class ExampleWebsocketConfig {
}
