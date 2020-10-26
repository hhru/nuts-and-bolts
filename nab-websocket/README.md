### Описание 

Работа с вебсокетами в основана на [Java API for WebSocket (JSR 356)](https://docs.oracle.com/javaee/7/tutorial/websocket.htm) стандарте

Чтобы добавить в сервис работу с вебсокетами, нужно:

1) В сервисе добавить зависимость:

```
<dependency>
    <groupId>ru.hh.nab</groupId>
    <artifactId>nab-websocket</artifactId>
    <version>${nab.version}</version>
</dependency>
```

2) При конфигурации наба использовать NabWebsocketConfigurator

```java
public static void main(String[] args) {
    NabApplication.builder()
        .configureJersey(ExampleJerseyConfig.class).bindToRoot()
        .apply(builder -> NabWebsocketConfigurator.configureWebsocket(builder, Set.of("ru.hh")))
        .build().run(ExampleConfig.class);
  }
``` 

3) Добавить в spring контекст ресурс описывающий нужный endpoint, [например](https://github.com/hhru/nuts-and-bolts/blob/master/nab-example/src/main/java/ru/hh/nab/example/WebsocketEchoEndpoint.java)