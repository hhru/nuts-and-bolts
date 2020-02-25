### Подключение 

В своем сервисе добавьте зависимость:
 ```
        <dependency>
            <groupId>ru.hh.nab</groupId>
            <artifactId>nab-kafka</artifactId>
            <version>${nab.version}</version>
        </dependency>

```
 
 
А также spring config:

```
  @Bean
  public ConfigProvider configProvider(String serviceName, FileSettings fileSettings) {
    return new ConfigProvider(serviceName, "kafka.cluster.name", fileSettings);
  }

  // если в сервисе нужно подписаться на топик
  @Bean
  public KafkaConsumerFactory kafkaConsumerFactory(ConfigProvider configProvider, StatsDSender statsDSender) {
    return new DefaultConsumerFactory(configProvider, new JacksonDeserializerSupplier(createObjectMapper()), statsDSender);
  }

  // если в сервисе нужно слать сообщения в топик
  @Bean
  @SuppressWarnings("rawtypes")
  public KafkaProducer<KafkaMessage> kafkaProducer(ConfigProvider configProvider) {
    return new KafkaProducerFactory(configProvider, new JacksonSerializerSupplier(createObjectMapper())).createDefaultProducer();
  }

```



### Подписываемся на топик

Inject'им KafkaConsumerFactory и вызываем следующий метод:

```
kafkaConsumerFactory.subscribe(
  topicName,  
  operationName, 
  MessageClass.class,  
  (messages, ack) -> processBatch(messages, ack)
);
```

Совокупоность параметров topicName и operationName, а также имя сервиса, определяет [имя consumer группы](https://github.com/hhru/nuts-and-bolts/blob/master/nab-kafka/src/main/java/ru/hh/nab/kafka/consumer/ConsumerGroupId.java). 
Подробнее про [consumer группы](https://kafka.apache.org/documentation/#intro_consumers)

Лямбда реализовывает интерфейс [ConsumerStrategy](https://github.com/hhru/nuts-and-bolts/blob/master/nab-kafka/src/main/java/ru/hh/nab/kafka/consumer/ConsumeStrategy.java)


### Метрики

Собираем:

* [Время обработки батча](https://github.com/hhru/nuts-and-bolts/blob/master/nab-kafka/src/main/java/ru/hh/nab/kafka/monitoring/MonitoringConsumeStrategy.java)
* метрики нативных апачевских клиентов

[Библиотека](https://github.com/hhru/hh-java-libs/tree/master/kafka-client-utils) для сбора метрик.

Подключается следующим образом: 
```
  @Bean
  public Object kafkaStatsDReporter(String serviceName, StatsDClient statsDClient, ScheduledExecutorService scheduledExecutorService) {
    return KafkaStatsDReporter.initialize(serviceName, statsDClient, scheduledExecutorService);
  }
```

### Формат конфигов сервиса

`kafka.common` - общие для Consumer'a/Producer'a
`kafka.producer.default.<key>` - общие настройки для Producer'ов
`kafka.consumer.default.<key>` - общие настройки для Consumer'ов
`kafka.consumer.topic.<topicName>.default.<key>` - для Consumer'а конкретного топика

### Пример использования

Consumer в [hh-pandora](https://github.com/hhru/hh-pandora/blob/master/pandora-service/src/main/java/ru/hh/pandora/integration/AreaStateListener.java)

Producer в [hh-xmlback](https://github.com/hhru/hh.ru/blob/master/hh-core/src/main/java/com/headhunter/tools/notification/KafkaPublisher.java)