# <p style="color: #f5f5f5;">ConfigProvider</p>

## <p style="color: #f5f5f5;">Описание</p>
<hr style="border: 1px solid #444; margin: 16px 0;">

Класс `ConfigProvider` предоставляет конфигурации для Kafka потребителей (consumers) и производителей (producers) на основе настроек, загруженных из файлов конфигурации.
Он также обеспечивает валидацию конфигураций и интеграцию с системой мониторинга. Вся концигурация основана на существующих настройках из официальной документации
для [consumer](https://kafka.apache.org/documentation/#consumerconfigs) или [producer](https://kafka.apache.org/documentation/#producerconfigs)

### Основные возможности:
- **Конфигурация Consumer**: сохранение конфиграции для kafka consumer
- **Конфигурация Producer**: сохранение конфиграции для kafka producer

### Создание ConfigProvider

Для примера возьмем кластер kafka site. У каждого кластера будет своя конфигурация.

```java
@Bean
@KafkaSite
public ConfigProvider kafkaSiteConfigProvider(
  @Named(NamedQualifier.SERVICE_NAME) String serviceName,
  FileSettings fileSettings,
  StatsDSender statsDSender
) {
  return new ConfigProvider(serviceName, "kafka.site", fileSettings, statsDSender);
}
```

### Конфигурация common

Настройки под префиксом `common` будут использоватсья для создания как kafka consumer, так и producer.

```properties
kafka.site.common.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="sitemap" password="{{ kafka_roles.site['sitemap']['pass'] }}";
kafka.site.common.sasl.mechanism=PLAIN
kafka.site.common.security.protocol=SASL_PLAINTEXT
```

### Конфигурация producer

Настройки под префиксом `producer` будут использоватсья для создания kafka producer. Профиль `default` будет имет приоритет
над `common`.

```properties
kafka.site.producer.default.client.id=client-id
kafka.site.producer.default.acks=1
kafka.site.producer.default.retries=2
kafka.site.producer.default.linger.ms={{ kafka_linger_time_ms }}
kafka.site.producer.default.max.request.size=16777216
```

### Конфигурация consumer

Настройки под префиксом `consumer` будут использоватсья для создания kafka consumer. Профиль `default` будет имет приоритет
над `common`.

```properties
kafka.site.consumer.default.auto.offset.reset=latest
kafka.site.consumer.default.fetch.max.wait.ms=5000
kafka.site.consumer.default.max.poll.interval.ms=300000
kafka.site.consumer.default.max.poll.records=10
kafka.site.consumer.default.enable.auto.commit=false
```

> ⚠️ В настоящий момент поддерживается только профиль default.</p>

### Конфигурация consumer для topic

Настройки под префиксом `consumer.topic` будут использоватсья для создания kafka consumer. Настройки topic-а
`random_topic_name` буду иметь приоритет над настройками самого consumer-а.

```properties
kafka.site.consumer.topic.random_topic_name.default.fetch.max.wait.ms=500
kafka.site.consumer.topic.random_topic_name.default.max.poll.records=5
kafka.site.consumer.topic.random_topic_name.default.fetch.min.bytes={{ 1024 * 100 }}
```

> ⚠️ В настоящий момент поддерживается только профиль default.</p>

## <p style="color: #f5f5f5;">Методы</p>
<hr style="border: 1px solid #444; margin: 16px 0;">

### `String getServiceName()`

- **Описание**: Возвращает имя сервиса, переданное при создании объекта.

### `String getKafkaClusterName()`

- **Описание**: Возвращает имя кластера Kafka, переданное при создании объекта.

### `Map<String, Object> getConsumerConfig(String topicName)`

- **Описание**: Возвращает настройки для указанного топика.

### `Map<String, Object> getDefaultProducerConfig()`

- **Описание**: Возвращает конфигурацию для производителя Kafka по умолчанию.

### `Map<String, Object> getProducerConfig(String producerName)`

- **Описание**: Возвращает конфигурацию для производителя Kafka с указанным именем.

# <p style="color: #f5f5f5;">SerializerSupplier</p>

## <p style="color: #f5f5f5;">Описание</p>
<hr style="border: 1px solid #444; margin: 16px 0;">

`SerializerSupplier` представляет собой способ динамического определения способа серилизации объектов.

### Основные возможности:
- **Управление сериализацией**: Полный контроль над способом сериализации объектов перед их отправкой.

### Пример реализации

```java
public class JacksonSerializerSupplier implements SerializerSupplier {

  private final ObjectMapper objectMapper;

  public JacksonSerializerSupplier(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public <T> Serializer<T> supply() {
    JsonSerializer<T> serializer = new JsonSerializer<>(objectMapper);
    serializer.setAddTypeInfo(false);
    return serializer;
  }
}
```


## <p style="color: #f5f5f5;">Методы</p>
<hr style="border: 1px solid #444; margin: 16px 0;">

`Serializer<T> supply()`

- **Описание**: Инициализирует стандартный apache kafka Serializer

# <p style="color: #f5f5f5;">DeserializerSupplier</p>

## <p style="color: #f5f5f5;">Описание</p>
<hr style="border: 1px solid #444; margin: 16px 0;">

`DeserializerSupplier` представляет собой способ динамического определения способа десериализации объектов.

### Основные возможности:
- **Управление десериализацией**: Полный контроль над способом десериализацией объектов перед началом их обработки.

### Пример реализации

```java
public class JacksonDeserializerSupplier implements DeserializerSupplier {

  private final ObjectMapper objectMapper;

  public JacksonDeserializerSupplier(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public <T> Deserializer<T> supplyFor(Class<T> clazz) {
    return new JsonDeserializer<>(clazz, objectMapper, false);
  }
}
```

## <p style="color: #f5f5f5;">Методы</p>
<hr style="border: 1px solid #444; margin: 16px 0;">

`Deserializer<T> supplyFor(Class<T> clazz)`

- **Описание**: Инициализирует стандартный apache kafka Deserializer

# <p style="color: #f5f5f5;">KafkaProducerFactory</p>

## <p style="color: #f5f5f5;">Описание</p>
<hr style="border: 1px solid #444; margin: 16px 0;">

Класс KafkaProducerFactory предоставляет фабрику для создания и настройки Kafka-продюсеров.
Он использует конфигурации, предоставляемые ConfigProvider, и позволяет гибко управлять параметрами продюсера,
включая сериализацию данных и настройки подключения к Kafka-кластеру.

### Основные возможности:
- **Содание Kafka-продюсеров**: упрощенное создание и настройка Kafka-продюсеров, обеспечивающая при этом валидацию конфигураций и поддержку кастомизации.

### Пример создания фабрики

По умолчанию предлагается создавать TelemetryAwareProducerFactory, которая инструментирует producer-а телеметрией.

```java
@Bean
@KafkaSite
public KafkaProducerFactory kafkaSiteProducerFactory(
    @KafkaSite ConfigProvider configProvider,
    OpenTelemetry telemetry,
    KafkaHostsFetcher kafkaHostsFetcher
) {
  return new TelemetryAwareProducerFactory(
      configProvider,
      new JacksonSerializerSupplier(KafkaSiteObjectMapperFactory.createObjectMapper()),
      telemetry,
      () -> kafkaHostsFetcher.get(KAFKA_SITE)
  );
}
```

### Пример создания producer-а

```properties
kafka.site.producer.default.client.id=client-id
kafka.site.producer.default.acks=1
kafka.site.producer.default.retries=2
kafka.site.producer.default.linger.ms={{ kafka_linger_time_ms }}
kafka.site.producer.default.max.request.size=16777216
```

Допустим мы сконфигурировали фабрику для работы с `kafka.site`. В таком случае все настройки `producer.default` будут использованы
для конфигурации producer-а.

```java
public KafkaProducer createProducer(KafkaProducerFactory factory) {
  return factory.createProducer("default");
}
```

## <p style="color: #f5f5f5;">Методы</p>
<hr style="border: 1px solid #444; margin: 16px 0;">

### `KafkaProducer createProducer(String producerSettingsName)`

- **Описание**: Создает Kafka-продюсер с указанным именем конфигурации.
- **Примечание**: Если указан bootstrapServersSupplier, он будет использован для настройки адресов брокеров.

# <p style="color: #f5f5f5;">KafkaProducer</p>

## <p style="color: #f5f5f5;">Описание</p>
<hr style="border: 1px solid #444; margin: 16px 0;">

`KafkaProducer` предоставляет базовую реализацию для отправки сообщений в Kafka. Он определяет набор методов для отправки сообщений в различные топики с возможностью указания ключа
и использования асинхронного исполнителя (Executor). Класс абстрагирует детали отправки сообщений, оставляя реализацию метода sendMessage для конкретных подклассов.
Его единственная реализация `DefaultKafkaProducer` использует KafkaTemplate из Spring Kafka для выполнения операций отправки
и поддерживает асинхронную отправку сообщений с возможностью обработки результатов через CompletableFuture.

### Основные возможности:
- **Отправка сообщений**: асинхронно отправляет сообщения в Kafka

### Пример создания producer-а

```properties
kafka.site.producer.default.client.id=client-id
kafka.site.producer.default.acks=1
kafka.site.producer.default.retries=2
kafka.site.producer.default.linger.ms={{ kafka_linger_time_ms }}
kafka.site.producer.default.max.request.size=16777216
```

Допустим мы сконфигурировали фабрику для работы с `kafka.site`. В таком случае все настройки `producer.default` будут использованы
для конфигурации producer-а.

```java
public KafkaProducer createProducer(KafkaProducerFactory factory) {
  return factory.createProducer("default");
}
```

## <p style="color: #f5f5f5;">Методы</p>
<hr style="border: 1px solid #444; margin: 16px 0;">

### `<T> CompletableFuture<KafkaSendResult<T>> sendMessage(String topic, T kafkaMessage)`

- **Описание**: Блокирующий вызов для отправки сообщения в указанный топик без указания ключа.

### `<T> CompletableFuture<KafkaSendResult<T>> sendMessage(String topic, T kafkaMessage, Executor executor)`

- **Описание**: Отправляет сообщение в указанный топик без указания ключа с использованием указанного исполнителя.

### `<T> CompletableFuture<KafkaSendResult<T>> sendMessage(String topic, String key, T kafkaMessage)`

- **Описание**: Блокирующий вызов для отправки сообщения в указанный топик с указанием ключа.

### `<T> CompletableFuture<KafkaSendResult<T>> sendMessage(String topic, String key, T kafkaMessage, Executor executor)`

- **Описание**: Отправляет сообщение в указанный топик с указанием ключа и использованием указанного исполнителя.

### `<T> CompletableFuture<KafkaSendResult<T>> sendMessage(ProducerRecord<String, T> record, Executor executor)`

- **Описание**: Отправляет сформированное kafka сообщение (ProducerRecord) c использованием указанного исполнителя.

# <p style="color: #f5f5f5;">ConsumerBuilder</p>

## <p style="color: #f5f5f5;">Описание</p>
<hr style="border: 1px solid #444; margin: 16px 0;">

Интерфейс `ConsumerBuilder<T>` предоставляет гибкий и мощный способ настройки и создания потребителей Kafka.

### Основные возможности:
- **Настройка клиента**: Установка идентификатора клиента (`withClientId`) и имени операции (`withOperationName`).
- **Группы потребителей**: Поддержка consumer-group для обеспечения обработки одной партиции только одним потребителем (`withConsumerGroup`).
- **Стратегии потребления**: Определение стратегии обработки сообщений (`withConsumeStrategy`) и пользовательской стратегии для повторных попыток (`withRetryConsumeStrategy`).
- **Retry Queue (RQ)**: Настройка RQ с использованием отдельных топиков и политик повторных попыток (`withRetries`).
- **Dead Letter Queue (DLQ)**: Настройка DLQ для обработки сообщений, которые не могут быть обработаны после исчерпания бюджета повторных попыток (`withDlq`).

### Retry Queue

По умолчанию имя retry queue топика будет имяТопика_имяСервиса_имяОперации_retry_receive.

```java
@Bean("resumeModerationKafkaConsumer")
public KafkaConsumer<ResumeModerationMessage> buildKafkaConsumer(
    @KafkaSite KafkaConsumerFactory kafkaSiteConsumerFactory,
    @KafkaSite KafkaProducer kafkaSiteProducer) {
  return kafkaSiteConsumerFactory
      .builder(KafkaEventType.RESUME_MODERATION.getTopic(), ResumeModerationMessage.class)
      .withOperationName(ResumeModerationMessageConsumer.class.getSimpleName())
      .withConsumeStrategy(ConsumeStrategy.atLeastOnceWithBatchAck(this::process))
      // Включение Retry Queue
      .withRetries(kafkaSiteProducer, RetryPolicyResolver.always(RetryPolicy.fixed(Duration.ofSeconds(30)).withTtl(Duration.ofHours(1))))
      .build();
}
```

#### Гибкая настройка частоты повторов

1. RetryPolicy.fixed(Duration.ofSeconds(60)) — повторять каждые 60 секунд.
2. RetryPolicy.progressive(retryCount -> retryCount < 3 ? Duration.ofSeconds(60) : Duration.ofHours(1)) — повторять каждые 60 секунд, если количество попыток меньше 3; если попыток 3 или больше, повторять каждый час.

#### Гибкая настройка условий завершения повторов

1. RetryPolicy.withRetryLimit(3) — завершить попытки, если их количество превышает 3.
2. RetryPolicy.withDeadline(OffsetDateTime.now().plusHours(3L).toInstant()) — завершить попытки, если прошло 3 часа с момента начала.
3. RetryPolicy.withTtl(Duration.ofHours(3L)) — завершить попытки, если сообщение было создано более 3 часов назад.

### Dead Letter Queue

```java
@Bean("resumeModerationKafkaConsumer")
public KafkaConsumer<ResumeModerationMessage> buildKafkaConsumer(
    @KafkaSite KafkaConsumerFactory kafkaSiteConsumerFactory,
    @KafkaSite KafkaProducer kafkaSiteProducer) {
  return kafkaSiteConsumerFactory
      .builder(KafkaEventType.RESUME_MODERATION.getTopic(), ResumeModerationMessage.class)
      .withOperationName(ResumeModerationMessageConsumer.class.getSimpleName())
      .withConsumeStrategy(ConsumeStrategy.atLeastOnceWithBatchAck(this::process))
      // Включение DLQ
      .withDlq("my_dlq_topic", kafkaSiteProducer)
      .build();
}
```

## <p style="color: #f5f5f5;">Методы</p>
<hr style="border: 1px solid #444; margin: 16px 0;">

### `ConsumerBuilder<T> withClientId(String clientId)`
- **Описание**: Устанавливает идентификатор клиента для потребителя Kafka.

### `ConsumerBuilder<T> withOperationName(String operationName)`
- **Описание**: Устанавливает имя операции для потребителя Kafka.

### `ConsumerBuilder<T> withConsumeStrategy(ConsumeStrategy<T> consumeStrategy)`
- **Описание**: Устанавливает стратегию потребления для обработки сообщений.

### `ConsumerBuilder<T> withRetries(KafkaProducer retryProducer, RetryPolicyResolver<T> retryPolicyResolver)`
- **Описание**: Настраивает потребителя Kafka для повторных попыток через `Ack#retry(ConsumerRecord, Throwable)`.
- **Требования**: `ConsumeStrategy` должна быть потокобезопасной.
- **Примечание**: Сообщения, запланированные для повторных попыток, хранятся в отдельном топике Kafka.

### `ConsumerBuilder<T> withRetries(KafkaProducer retryProducer, RetryPolicyResolver<T> retryPolicyResolver, RetryTopics retryTopics)`
- **Описание**: Настраивает потребителя Kafka для повторных попыток с использованием указанных топиков для повторных попыток.
- **Требования**: `ConsumeStrategy` должна быть потокобезопасной.
- **Примечание**: Если указан один топик, то повторные попытки обрабатываются этим сервисом. Если указаны два топика, то другой сервис отвечает за планирование повторных попыток.

### `ConsumerBuilder<T> withRetryConsumeStrategy(ConsumeStrategy<T> retryConsumeStrategy)`
- **Описание**: Указывает пользовательскую стратегию потребления для повторных попыток.
- **Требования**: Пользовательская стратегия должна обрабатывать сообщения только тогда, когда они готовы к обработке.

### `static <T> SimpleDelayedConsumeStrategy<T> decorateForDelayedRetry(ConsumeStrategy<T> delegate, Duration sleepIfNotReadyDuration)`
- **Описание**: Декорирует стратегию потребления для обработки сообщений только тогда, когда они готовы к повторной попытке.
- **Примечание**: Используется для задержки обработки сообщений до их готовности.

### `ConsumerBuilder<T> withDlq(String destination, KafkaProducer producer)`
- **Описание**: Настраивает Dead Letter Queue (DLQ) для отправки сообщений в случае непредвиденных ситуаций.
- **Примечание**: Сообщения отправляются в DLQ в случае явного намерения или исчерпания бюджета повторных попыток.

### `ConsumerBuilder<T> withLogger(Logger logger)`
- **Описание**: Устанавливает логгер для потребителя Kafka.

### `ConsumerBuilder<T> withConsumerGroup()`
- **Описание**: Включает потребителя в consumer-group, чтобы одна партиция топика не обрабатывалась более чем одним потребителем одновременно.

### `ConsumerBuilder<T> withAllPartitionsAssigned(SeekPosition seekPosition, Duration checkNewPartitionsInterval)`
- **Описание**: Потребитель подписывается на все партиции топика.
- **Примечание**: Используется для stateless-сервисов, которые читают все партиции в каждом экземпляре сервиса.
- **Параметры**:
    - `seekPosition`: Указывает, откуда начинать чтение сообщений (с начала или конца топика).
    - `checkNewPartitionsInterval`: Как часто проверять наличие новых партиций в топике. Если `null`, проверка не выполняется.

### `ConsumerBuilder<T> withAllPartitionsAssigned(SeekPosition seekPosition)`
- **Описание**: Упрощенная версия метода `withAllPartitionsAssigned` без указания интервала проверки новых партиций.

### `KafkaConsumer<T> build()`
- **Описание**: Создает и возвращает экземпляр потребителя Kafka.

### `@Deprecated KafkaConsumer<T> start()`
- **Описание**: Устаревший метод. Используйте `build()` и затем `KafkaConsumer#start()`.

# <p style="color: #f5f5f5;">KafkaConsumer</p>

## <p style="color: #f5f5f5;">Описание</p>
<hr style="border: 1px solid #444; margin: 16px 0;">

`KafkaConsumer` представляет собой высокоуровневую абстракцию над Apache Kafka и Spring Kafka,
предназначенную для эффективной обработки сообщений. Класс обеспечивает удобное управление жизненным циклом потребителя,
обработку сообщений и автоматическую подписку на изменения в партициях топика.

### Основные возможности:
- **Управление жизненным циклом**: Полный контроль над запуском, остановкой и мониторингом состояния потребителя.
- **Обработка сообщений**: Гибкая настройка стратегий обработки сообщений, включая поддержку batch-обработки.
- **Автоматическая подписка на изменения**: Интеграция с механизмами автоматической подписки на изменения в партициях для потребителей без указания `group_id` или `consumer_group`

### Автоматическое управление жизненным циклом через SmartLifeCycle

```java
@Bean
  public KafkaConsumer<VacancyBodySubEntityReplicationEvent> kafkaCdcVacancyBodyTextConsumer(
      @KafkaCdc KafkaConsumerFactory kafkaConsumerFactory
  ) {
    return kafkaConsumerFactory
        .builder("cdc_db_hh_vacancy_body_text", VacancyBodySubEntityReplicationEvent.class)
        .withOperationName("personalSyncVacancyBodyText")
        .withConsumeStrategy(ConsumeStrategy.atLeastOnceWithBatchAck(this::process))
        .build();
  }
```

### Ручное управление жизненным циклом
```java
@PostConstruct
public void subscribe() {
  LOGGER.info("subscribing to topic {}", topicName);
  kafkaConsumer = kafkaConsumerFactory
      .builder(topicName, MailMessage.class)
      .withOperationName("mailer_message_listener")
      .withConsumeStrategy(mailerMessageProcessor::process)
      .build();
  kafkaConsumer.start();
}

@PreDestroy
public void stop() {
  if (kafkaConsumer != null) {
    kafkaConsumer.stop();
  }
}
```


## <p style="color: #f5f5f5;">Методы</p>
<hr style="border: 1px solid #444; margin: 16px 0;">

### `void start()`

- **Описание**: Запускает потребителя и инициирует процесс обработки сообщений из Kafka-топика.

### `void stop()`

- **Описание**: Останавливает потребителя и прерывает процесс обработки сообщений. Использует `InterruptedException` для корректного завершения работы.

### `boolean isRunning()`

- **Описание**: Возвращает текущее состояние потребителя.

### `Collection<TopicPartition> getAssignedPartitions()`

- **Описание**: Возвращает список партиций, назначенных текущему потребителю.

### `void onMessagesBatch(List<ConsumerRecord<String, T>> messages, Consumer<?, ?> consumer)`

- **Описание**: Определяет логику обработки пакета сообщений, полученных из Kafka-топика с помощью метода `poll`
- **Примечание**:
  - В текущей реализации поддерживается только batch-обработка сообщений.
  - После обработки каждого пакета смещение (offset) автоматически сдвигается на следующее сообщение в партиции.

# <p style="color: #f5f5f5;">Ack</p>

## <p style="color: #f5f5f5;">Описание</p>
<hr style="border: 1px solid #444; margin: 16px 0;">

Интерфейс `Ack<T>` предоставляет механизм управления потребления сообщений из Kafka.

### Основные возможности:
- **Acknowledge**: Сигнал о коцне обработки сообщения с последующим смещением offset-а на новое
- **Negative Acknowledge**: Сигнал об отправке сообщения в DLQ
- **Retry**: Сигнал об отправке собщения в RQ
- **Seek**: Ручное управление offset-ами
- **Commit**: Ручное управление сommit-ами

### Обработка одного сообщения с at least once гарантиями доставки

```java
@Bean
public KafkaConsumer<VacancyReplicationEvent> kafkaVacancyConsumer(
    @KafkaCdc KafkaConsumerFactory kafkaConsumerFactory
) {
  return kafkaConsumerFactory
      .builder(VACANCY_CDC_TOPIC, VacancyReplicationEvent.class)
      .withOperationName(OPERATION)
      // Используется стратегия с at least once гарантией доставки
      .withConsumeStrategy(ConsumeStrategy.atLeastOnceWithBatchAck(this::process))
      .build();
}
```

### Обработка batch-ей сообщений

```java
 @PostConstruct
public void startConsumer() {
  consumer = kafkaSiteConsumerFactory
      .builder("hh_logout", HhLogoutMessage.class)
      .withAllPartitionsAssigned(SeekPosition.LATEST)
      .withOperationName("invalidate_websocket_session")
      // Используется стратегия для работы с batch-ами сообщений
      .withConsumeStrategy((messages, ack) -> {
        ack.acknowledge();
        onHhLogoutEvent(messages);
      })
      .start();
}

@PreDestroy
public void stopConsumer() {
  consumer.stop();
}
```

### Как работать с Ack

```java
public class TranslationUpdate implements ConsumeStrategy<String> {

  @Override
  public void onMessagesBatch(List<ConsumerRecord<String, String>> messages, Ack<String> ack) throws InterruptedException {
    for (ConsumerRecord<String, String> message : messages) {
      String words = message.value();
      CompletableFuture<String> futureResult = requestTranslation(words);
      try {
        String result = futureResult.get(1L, TimeUnit.SECONDS);
      } catch (ExecutionException e) { // Ошибка в логике обработки, запишем в DLQ и разберем позже вручную
        ack.nAck(message);
      } catch (TimeoutException e) { // Ответ не получен, запишем в RQ и попробуем еще раз позже автоматически
        ack.retry(message, e);
      }
    }
    // Мы обработали весь batch без ошибок, дожидаемся конца операций retry & nAck
    ack.acknowledge();
  }

  private CompletableFuture<String> requestTranslation(String value) throws InterruptedException {
    // Делаем вызов к внешнему сервису
    return CompletableFuture.completedFuture(value);
  }
}
```

## <p style="color: #f5f5f5;">Методы</p>
 <hr style="border: 1px solid #444; margin: 16px 0;">

### `void acknowledge()`

- **Описание**: Перемещает committedOffset и fetchOffset на смещения, возвращенные при последнем вызове Consumer.poll() для подписанных топиков и партиций.
- **Примечание**: Этот метод должен выполняться только в потоке слушателя потребителя. Он блокирует поток до тех пор, пока смещения не будут зафиксированы в Kafka и все будущие повторные попытки для текущего пакета не будут завершены.

### `void acknowledge(ConsumerRecord<String, T> message)`

- **Описание**: Перемещает committedOffset и fetchOffset для топика и партиции переданного сообщения.
- **Примечание**: Этот метод должен выполняться только в потоке слушателя потребителя, иначе будет выброшено исключение ConcurrentModificationException. Он блокирует поток до тех пор, пока смещения не будут зафиксированы в Kafka и все будущие повторные попытки для текущего пакета не будут завершены.

### `void nAck(ConsumerRecord<String, T> message)`

- **Описание**: Неблокирующая асинхронная операция, которая отправляет сообщение в отдельный топик Dead Letter Queue (DLQ).
- **Примечание**: Этот метод должен вызываться только в потоке слушателя потребителя, чтобы избежать ConcurrentModificationException.

### `void acknowledge(Collection<ConsumerRecord<String, T>> messages)`

- **Описание**: Перемещает committedOffset и fetchOffset для топиков и партиций переданных сообщений.
- **Примечание**: Этот метод должен выполняться только в потоке слушателя потребителя, иначе будет выброшено исключение ConcurrentModificationException. Он блокирует поток до тех пор, пока смещения не будут зафиксированы в Kafka и все будущие повторные попытки для текущего пакета не будут завершены.

### `void nAck(Collection<ConsumerRecord<String, T>> messages)`

- **Описание**: Неблокирующая асинхронная операция, которая отправляет сообщения в отдельный топик Dead Letter Queue (DLQ).
- **Примечание**: Этот метод должен вызываться только в потоке слушателя потребителя, чтобы избежать ConcurrentModificationException.

### `void seek(ConsumerRecord<String, T> message)`

- **Описание**: Перемещает fetchOffset для топика и партиции сообщения на позицию, следующую за смещением сообщения.
- **Примечание**: Этот метод должен выполняться только в потоке слушателя потребителя, иначе будет выброшено исключение ConcurrentModificationException. Он блокирует поток до тех пор, пока смещения не будут зафиксированы в Kafka и все будущие повторные попытки для текущего пакета не будут завершены.

### `void commit(Collection<ConsumerRecord<String, T>> messages)`

- **Описание**: Фиксирует указанные смещения для переданных сообщений в Kafka.
- **Примечание**: Этот метод должен выполняться только в потоке слушателя потребителя, иначе будет выброшено исключение ConcurrentModificationException. Он предназначен для использования только в случае асинхронной обработки сообщений.

### `void retry(ConsumerRecord<String, T> message, Throwable error)`

- **Описание**: Планирует повторную попытку обработки сообщения из-за ошибки или решения бизнес-логики.
- **Примечание**: Время повторной попытки определяется конфигурацией потребителя. Вызывающий может использовать возвращенный Future, но не обязан ждать его завершения, так как следующий вызов seek или любого из методов acknowledge сделает это.
