# 17.0.0

## Description

Из кода удалены конфигурации спринговых бинов. Теперь nab представляет собой набор библиотек. Добавлены новые модули nab-web, nab-consul, nab-sentry.

1. Модуль nab-starter - удален.
    - класс NabApplication - удален
    - класс NabApplicationBuilder - удален
    - класс NabCommonConfig - удален
    - класс NabProdConfig - удален
    - класс AppMetadata - удален
    - класс NabServletContextConfig - удален
    - класс DefaultResourceConfig - удален
    - класс NabJerseyConfig - удален
    - интерфейс NabServletConfig - удален
    - класс StatusServletConfig - удален
    - аннотация Service - удалена
    - класс JettyServer - удален
    - класс JettyServerFactory - удален
    - класс JettyWebAppContext - удален
    - класс JettySettingsConstants - удален
    - класс JettyBeforeStopEvent - удален
    - класс JettyStartedEvent - удален
    - класс JettyEventListener - удален
    - класс JettyLifeCycleListener - удален
    - класс JettyServerException - удален
    - класс ChannelsReadyChecker - удален
    - интерфейс WebAppInitializer - удален
    - класс HierarchicalWebApplicationContext - удален
    - класс NabLogbackBaseConfigurator - удален
    - класс HHServerConnector - переехал в новый модуль nab-web в пакет `ru.hh.nab.web.jetty`. Также удалены некоторые конструкторы, удален оверрайд
      метода shutdown
    - класс StatusResource переехал в новый модуль nab-web в пакет `ru.hh.nab.web.resource`. Также изменена сигнатура конструктора
    - класс NabExceptionMapper переехал в новый модуль nab-web в пакет `ru.hh.nab.web.exceptions`
    - класс AnyExceptionMapper переехал в новый модуль nab-web в пакет `ru.hh.nab.web.exceptions`
    - класс CompletionExceptionMapper переехал в новый модуль nab-web в пакет `ru.hh.nab.web.exceptions`
    - класс ExecutionExceptionMapper переехал в новый модуль nab-web в пакет `ru.hh.nab.web.exceptions`
    - класс IllegalArgumentExceptionMapper переехал в новый модуль nab-web в пакет `ru.hh.nab.web.exceptions`
    - класс IllegalStateExceptionMapper переехал в новый модуль nab-web в пакет `ru.hh.nab.web.exceptions`
    - класс NabMappableException переехал в новый модуль nab-web в пакет `ru.hh.nab.web.exceptions`
    - класс NotFoundExceptionMapper переехал в новый модуль nab-web в пакет `ru.hh.nab.web.exceptions`
    - класс SecurityExceptionMapper переехал в новый модуль nab-web в пакет `ru.hh.nab.web.exceptions`
    - класс UnwrappingExceptionMapper переехал в новый модуль nab-web в пакет `ru.hh.nab.web.exceptions`
    - класс WebApplicationExceptionMapper переехал в новый модуль nab-web в пакет `ru.hh.nab.web.exceptions`
    - интерфейс ExceptionSerializer переехал в новый модуль nab-web в пакет `ru.hh.nab.web.exceptions`
    - класс MappableExceptionUtils переехал в новый модуль nab-web в пакет `ru.hh.nab.web.exceptions`
    - класс CacheUtils переехал в новый модуль nab-web в пакет `ru.hh.nab.web.http`
    - класс HttpStatus переехал в новый модуль nab-web в пакет `ru.hh.nab.web.http`
    - класс RequestContext переехал в новый модуль nab-web в пакет `ru.hh.nab.web.http`
    - класс RequestInfo переехал в новый модуль nab-web в пакет `ru.hh.nab.web.http`
    - класс CachedResponse переехал в новый модуль nab-web в пакет `ru.hh.nab.web.jersey.filter.cache`
    - класс CachingOutputStream переехал в новый модуль nab-web в пакет `ru.hh.nab.web.jersey.filter.cache`
    - класс Header переехал в новый модуль nab-web в пакет `ru.hh.nab.web.jersey.filter.cache`
    - класс Serializer переехал в новый модуль nab-web в пакет `ru.hh.nab.web.jersey.filter.cache`
    - класс CacheFilter переехал в новый модуль nab-web в пакет `ru.hh.nab.web.jersey.filter`. Также изменена сигнатура конструктора
    - класс ErrorAcceptFilter переехал в новый модуль nab-web в пакет `ru.hh.nab.web.jersey.filter`
    - класс ResourceInformationFilter переехал в новый модуль nab-web в пакет `ru.hh.nab.web.jersey.filter`
    - класс CommonHeadersFilter переехал в новый модуль nab-web в пакет `ru.hh.nab.web.servlet.filter`
    - класс RequestIdLoggingFilter переехал в новый модуль nab-web в пакет `ru.hh.nab.web.servlet.filter`
    - класс SkippableFilter переехал в новый модуль nab-web в пакет `ru.hh.nab.web.servlet.filter`
    - класс SentryAppenderInterceptor переехал в новый модуль nab-web в пакет `ru.hh.nab.web.jersey.interceptor`
    - класс CharacterEscapeBase переехал в новый модуль nab-web в пакет `ru.hh.nab.web.jersey.resolver`
    - класс JsonCharacterEscapes переехал в новый модуль nab-web в пакет `ru.hh.nab.web.jersey.resolver`
    - класс MarshallerContextResolver переехал в новый модуль nab-web в пакет `ru.hh.nab.web.jersey.resolver`. Также изменена сигнатура конструктора
    - класс ObjectMapperContextResolver переехал в новый модуль nab-web в пакет `ru.hh.nab.web.jersey.resolver`
    - класс PartiallyOverflowingCache переехал в новый модуль nab-web в пакет `ru.hh.nab.web.jersey.resolver`
    - класс XmlEscapeHandler переехал в новый модуль nab-web в пакет `ru.hh.nab.web.jersey.resolver`
    - класс NabPriorities переехал в новый модуль nab-web в пакет `ru.hh.nab.web.jersey`
    - класс MonitoredQueuedThreadPool переехал в новый модуль nab-web в пакет `ru.hh.nab.web.jetty`
    - класс StructuredRequestLogger переехал в новый модуль nab-web в пакет `ru.hh.nab.web.jetty`
    - класс ConsulFetcher переехал в новый модуль nab-consul в пакет `ru.hh.nab.consul`. Также изменена сигнатура конструктора
    - класс ConsulMetricsTracker переехал в новый модуль nab-consul в пакет `ru.hh.nab.consul`
    - класс ConsulService переехал в новый модуль nab-consul в пакет `ru.hh.nab.consul`. Также изменена сигнатура конструктора; в методах register и
      deregister удалена обработка флага `consul.registration.enabled`
    - класс ConsulServiceException переехал в новый модуль nab-consul в пакет `ru.hh.nab.consul`
    - класс HostPort переехал в новый модуль nab-consul в пакет `ru.hh.nab.consul`
    - интерфейс HostsFetcher переехал в новый модуль nab-consul в пакет `ru.hh.nab.consul`
    - класс LogLevelOverrideApplier переехал в модуль nab-logging в пакет `ru.hh.nab.logging.override`. Также изменена сигнатура конструктора и метода
      run
    - интерфейс LogLevelOverrideExtension переехал в модуль nab-logging в пакет `ru.hh.nab.logging.override`
    - класс SkipLogLevelOverrideException переехал в модуль nab-logging в пакет `ru.hh.nab.logging.override`
    - класс RequestHeaders переехал в модуль nab-common в пакет `ru.hh.nab.common.constants`
    - класс SentryFilter переехал в новый модуль nab-sentry в пакет `ru.hh.nab.sentry`
    - класс NabApplication.SentryEventProcessor переехал в новый модуль nab-sentry в пакет `ru.hh.nab.sentry`

2. Модуль nab-web - помимо классов, которые были перенесены из nab-starter, в него также добавлены:
    - класс NoopSecurityHandler, который наследуется от ConstraintSecurityHandler и овверайдит метод handle (в реализации просто вызывается следующий
      в цепочке handler)
    - класс SecurityConfiguration, который наследуется от AbstractConfiguration и оверрайдит метод configure (в реализации в WebAppContext сеттится
      NoopSecurityHandler, если флаг securityEnabled=false). Данный класс может использоваться для настройки WebAppContext
    - класс NoopSessionHandler, который наследуется от SessionHandler и оверрайдит методы doScope и doHandle (в реализации просто вызывается следующий
      в цепочке handler)
    - класс SessionConfiguration, который наследуется от AbstractConfiguration и оверрайдит метод configure (в реализации в WebAppContext сеттится
      NoopSessionHandler, если флаг sessionEnabled=false). Данный класс может использоваться для настройки WebAppContext

3. Модуль nab-websocket - удален.

4. Модуль nab-common:
    - интерфейс NabServletFilter - удален
    - класс FileSystemUtils - удален
    - класс MonitoredThreadPoolExecutor - переехал в модуль nab-metrics в пакет `ru.hh.nab.metrics.executor`. Также изменена сигнатура статического
      метода create
    - класс ThreadDiagnosticRejectedExecutionHandler - переехал в модуль nab-metrics в пакет `ru.hh.nab.metrics.executor`
    - класс FileSettings - помечен как deprecated
    - класс PropertiesUtils - добавлены различные утилитные методы для более удобной работы с Properties, а метод fromFilesInSettingsDir помечен как
      deprecated
    - класс NamedQualifier - добавлены константы SERVICE_VERSION и DATACENTERS
    - добавлен класс ServletFilterPriorities, содержащий константные значения, которые могут быть использованы в сервисах при регистрации сервлетных
      фильтров, чтобы упорядочить их

5. Модуль nab-logging - помимо классов, которые были перенесены из nab-starter:
    - в классе NabLoggingConfiguratorTemplate удален метод loadPropertiesFile

6. Модуль nab-sentry - помимо классов, которые были перенесены из nab-starter:
    - добавлен класс SentryInitializer, который может быть использован для конфигурации sentry

7. Модуль nab-metrics - помимо классов, которые были перенесены из nab-common:
    - константы из StatsDConstants переехали в класс StatsDClientFactory
    - в классе JvmMetricsSender удален статический метод create
    - класс StatsDClientFactory - изменена сигнатура статического метода createNonBlockingClient

8. Модуль nab-data-source:
    - класс NabDataSourceCommonConfig - удален
    - класс NabDataSourceProdConfig - удален
    - интерфейс MetricsTrackerFactoryProvider - изменена сигнатура метода create
    - класс NabMetricsTrackerFactoryProvider - изменена сигнатура метода create
    - класс NabMetricsTrackerFactory - изменена сигнатура конструктора
    - класс DataSourceFactory - изменена сигнатура методов create и createDataSource

9. Модуль nab-jpa:
    - класс NabJpaCommonConfig - удален
    - класс NabJpaProdConfig - удален

10. Модуль nab-hibernate:
    - класс NabHibernateCommonConfig - удален
    - класс NabHibernateProdConfig - удален

11. Модуль nab-jclient:
    - класс NabJClientConfig - удален

12. Модуль nab-kafka:
    - класс ConfigProvider - изменена сигнатура конструктора

13. Модуль nab-telemetry:
    - класс NabTelemetryConfig - удален

14. Модуль nab-telemetry-jdbc:
    - класс NabTelemetryJdbcProdConfig - удален
    - аннотация Connection - удалена
    - аннотация Statement - удалена
    - класс NabTelemetryDataSourceFactory - изменена сигнатура конструктора; в методе wrap удалена обработка флага `opentelemetry.jdbc.enabled`

15. Модуль nab-testbase:
    - класс OverrideNabApplication - удален
    - аннотация @NabJunitWebConfig - удалена
    - аннотация @NabTestServer - удалена
    - класс NabTestConfig - удален
    - класс NabDataSourceTestBaseConfig - удален
    - класс NabHibernateTestBaseConfig - удален
    - класс TestLogbackBaseConfigurator - удален
    - файл hibernate-test.properties - удален
    - файл project.properties - удален
    - файл service-test.properties - удален
    - класс EmbeddedPostgresDataSourceFactory - изменена сигнатура метода createDataSource
    - класс ResourceHelper - переехал в пакет `ru.hh.nab.testbase.web`. Также изменена сигнатура конструктора
    - добавлен класс WebTestBase, в котором подключается SpringExtensionWithFailFast и инжектится ResourceHelper. В сервисах можно наследовать
      тестовые классы от WebTestBase

16. Модуль nab-example - удален.

## Instructions

1. Для старта приложения, поднятия веб сервера, регистрации сервлетов, фильтров и тд использовать иной фреймворк (например Spring Boot) и api,
   который он предоставляет.
2. Для конфигурации вебсокетов необходимо использовать api, предоставляемый фреймворком, который используется для поднятия
   веб сервера (например, Spring Boot'ом).
3. Для поднятия веб сервера в юинт тестах использовать api, предоставляемый фреймворком (например, Spring Boot'ом).
4. Подключить в помнике новые артефакты (nab-web, nab-consul, nab-sentry), если в них есть необходимость.
5. Поправить импорты.
6. При создании объекта LogLevelOverrideApplier в конструктор необходимо передавать:
    - LogLevelOverrideExtension extension, Properties properties
7. При вызове метода LogLevelOverrideApplier#run не нужно ничего передавать.
8. Скопировать метод NabLoggingConfiguratorTemplate#loadPropertiesFile, если в нем есть необходимость.
9. Самостоятельно реализовать LogbackBaseConfigurator (в том числе для тестов), унаследовавшись от NabLoggingConfiguratorTemplate,
   если в этом есть необходимость.
10. Для конфигурации sentry использовать SentryInitializer.
11. При создании объекта StatusResource в конструктор необходимо передавать:
    - String serviceName, String serviceVersion, Supplier<Duration> upTimeSupplier.
12. При создании объекта HHServerConnector в конструктор необходимо передавать:
    - Server server, int acceptors, int selectors, StatsDSender statsDSender, String serviceName
    - Server server, Executor executor, Scheduler scheduler, ByteBufferPool bufferPool, int acceptors,
      int selectors, StatsDSender statsDSender, String serviceName, ConnectionFactory... factories
      Также самостоятельно заоверрайдить метод shutdown, если в этом есть необходимость.
13. При создании объекта CacheFilter в конструктор необходимо передавать:
    - String serviceName, DataSize size, StatsDSender statsDSender
14. При создании объекта MarshallerContextResolver в конструктор необходимо передавать:
    - Properties properties, String serviceName, StatsDSender statsDSender
15. При создании объекта ConsulFetcher в конструктор необходимо передавать:
    - HealthClient healthClient, String serviceName, List<String> datacenters
16. При создании объекта ConsulService в конструктор необходимо передавать:
    - AgentClient agentClient, KeyValueClient kvClient, String serviceName, String serviceVersion, String nodeName, int applicationPort, Properties
      properties, Set<String> tags
17. Самостоятельно обрабатывать флаг `consul.registration.enabled` и не вызывать методы ConsulService#register и ConsulService#deregister,
    если у флага значение false.
18. При вызове метода MonitoredThreadPoolExecutor#create необходимо передавать:
    - Properties threadPoolProperties, String threadPoolName, StatsDSender statsDSender, String serviceName
    - Properties threadPoolProperties, String threadPoolName, StatsDSender statsDSender, String serviceName, RejectedExecutionHandler
      rejectedExecutionHandler
19. При вызове метода StatsDClientFactory#createNonBlockingClient необходимо передавать:
    - Properties properties
20. Удалить вызов метода JvmMetricsSender#create - вместо этого необходимо создавать объект JvmMetricsSender с помощью конструктора.
21. Если используются константы из StatsDConstants, то необходимо поправить импорты, так как константы переехали в класс StatsDClientFactory.
22. Если у вас есть классы, которые имплементируют интерфейс MetricsTrackerFactoryProvider, то необходимо поправить сигнатуру метода create: вместо
    FileSettings использовать Properties.
23. При вызове метода NabMetricsTrackerFactoryProvider#create необходимо передавать:
    - Properties dataSourceProperties
24. При создании объекта NabMetricsTrackerFactory в конструктор необходимо передавать:
    - String serviceName, StatsDSender statsDSender, Properties dataSourceProperties
25. При вызове метода DataSourceFactory#create необходимо передавать:
    - String dataSourceName, boolean isReadonly, Properties properties
    - String databaseName, String dataSourceType, boolean isReadonly, Properties properties
    - HikariConfig hikariConfig, Properties dataSourceProperties, boolean isReadonly
26. При вызове метода DataSourceFactory#createDataSource необходимо передавать:
    - String dataSourceName, boolean isReadonly, Properties dataSourceProperties
27. При создании объекта ConfigProvider в конструктор необходимо передавать:
    - String serviceName, String kafkaClusterName, Properties properties, StatsDSender statsDSender
28. При создании объекта NabTelemetryDataSourceFactory в конструктор необходимо передавать:
    - Instrumenter<NabDataSourceInfo, Void> connectionInstrumenter, Instrumenter<NabDbRequest, Void> statementInstrumenter
29. Самостоятельно обрабатывать флаг `opentelemetry.jdbc.enabled` и не вызывать метод NabTelemetryDataSourceFactory#wrap, если у флага значение false.
30. При вызове метода EmbeddedPostgresDataSourceFactory#createDataSource необходимо передавать:
    - String dataSourceName, boolean isReadonly, Properties dataSourceSettings
31. При создании объекта ResourceHelper в конструктор необходимо передавать:
    - int serverPort
    - Supplier<Integer> serverPort
32. Если в тестах выполняется чтение настроек из файлов service-test.properties, project.properties, hibernate-test.properties, необходимо добавить
    данные файлы с нужными пропертями в src/test/resources, либо использовать api, предоставляемый фреймворком (например, Spring Boot'ом) для работы с
    пропертями.
33. Скопировать класс FileSystemUtils, если в нем есть необходимость.
34. При необходимости скопировать бины из следующих конфигураций:
    - NabCommonConfig
    - NabProdConfig
    - NabDataSourceCommonConfig
    - NabDataSourceProdConfig
    - NabJpaCommonConfig
    - NabJpaProdConfig
    - NabHibernateCommonConfig
    - NabHibernateProdConfig
    - NabJClientConfig
    - NabTelemetryConfig
    - NabTelemetryJdbcProdConfig
    - NabTestConfig (в тестах)
    - NabDataSourceTestBaseConfig (в тестах)
    - NabHibernateTestBaseConfig (в тестах)

# 11.0.0

## Description

### **API**

#### **NabKafkaException**

* Добавлена новая иерархия RuntimeException для nab-kafka.

#### **ConfigurationException**

* Добавлена новая ошибка, сообщающая о нерабочих конфигурациях.

#### **ConsumerBuilder**

* Добавлен новый метод `ConsumerBuilder<T> withDlq(String destination, KafkaProducer producer);`

#### **KafkaConsumer**

* Метод `ConsumerConsumingState<T> getConsumingState()` переименован в `ConsumerContext<T> getConsumerContext()` и более не находится в публичном
  доступе.

#### **Ack**

* Добавлен новый метод `void nAck(ConsumerRecord<String, T> message);`
* Добавлен новый метод `void nAck(Collection<ConsumerRecord<String, T>> messages);`

* Изменен метод `void retry(ConsumerRecord<String, T> message, Throwable error);`

#### **ConsumerConsumingState**

* Класс переименован в ConsumerContext и более не находится в публичном доступе.

### ПОВЕДЕНИЕ

#### **KafkaConsumer**

* Научился пропускать corrupted сообщения
* Шлет новую метрику `nab.kafka.errors.records.deserialization.count`, которая показывает кол-во corrupted сообщений пришедших в сервис

#### **ConsumeStrategy**

* Удалено явное смещение offset-а после процессинга одного сообщения, теперь это делает acknowledge в конце batch-а

#### **ConsumerBuilder#withRetries**

* В случае исчерпания попыток для retry-ев сообщения отправляется в DLQ, если конфигурация для DLQ была включена

#### **DefaultConsumerBuilder**

* Метод `build()` теперь кидает новые ConfigurationException, если конфигурация не является рабочей.

## Instructions

1. Прекратить использовать KafkaConsumer#getConsumingState, это внутреннее api
2. Прекратить использовать возвращаемое значение от Ack#retry, оно не означало конец исполнения retry операции
3. Прекратить использовать ConsumerConsumingState, это внутреннее api
4. Если у вас были собственные решения для пропуска corrupted сообщений, то их можно и нужно удалить

# 8.0.0

Date: '2024-11-18T19:35:48.114744+03:00'

Description:

1. KafkaConsumer теперь умеет в ретраи. TLDR: используйте ConsumerBuilder::withRetries, см. доку к этому методу. Пример в ConsumerRetriesTest.
2. KafkaConsumer теперь корректно стартует и останавливается Spring контекстом, если создан как самостоятельный bean.
3. Удален SimpleKafkaConsumer.
4. Удален метод ConsumerBuilder::withAckProvider.

Compatible: false

Instructions:

1. Заменяем kafkaConsumerFactory.subscribe(...) на kafkaConsumerFactory.builder(...).build().start()

   было
    ```java
    kafkaConsumerFactory.subscribe(
        topicName,
        operationName,
        Message.class,
        this::processMessages
    )
    ```
   стало
    ```java
     kafkaConsumerFactory
         .builder(topicName, Message.class)
         .withOperationName(operationName)
         .withConsumeStrategy(this::processMessages)
         .build()
         .start();
    ```
2. Заменяем SimpleKafkaConsumer на kafkaConsumerFactory.builder(...).build().start(). Можно выставить консумер бином в контекст, тогда старт/стоп он
   вызовет сам

   было
    ```java
    public class SomeMessageProcessor extends SimpleKafkaConsumer<Message> {
      @Inject
      public SomeMessageProcessor(@KafkaTasks KafkaConsumerFactory kafkaTasksConsumerFactory) {
        super(
            kafkaTasksConsumerFactory,
            topicName,
            operationName,
            ConsumeStrategy::atLeastOnceWithBatchAck,
            Message.class
        );
    ```
   стало
    ```java
    public class SomeMessageProcessor {
      @Bean
      public KafkaConsumer<Message> buildKafkaConsumer(@KafkaTasks KafkaConsumerFactory kafkaTasksConsumerFactory) {
        return kafkaTasksConsumerFactory
            .builder(topicName, Message.class)
            .withOperationName(operationName)
            .withConsumeStrategy(ConsumeStrategy.atLeastOnceWithBatchAck(this::process))
            .build();
      }
    ```



