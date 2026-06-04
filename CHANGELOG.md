# Changelog

Этот формат соответствует [Keep a Changelog](https://keepachangelog.com/ru/1.1.0/).
Проект придерживается [Семантического Версионирования](https://semver.org/lang/ru/spec/v2.0.0.html).

## [32.0.0] - 2026-06-04

### Добавлено

- Для некоторых span-атрибутов OpenTelemetry раньше не было констант, которые теперь добавлены. Рекомендуется поискать их текстовым поиском
  и заменить на константы.
  - Вместо `destination.address` рекомендуется использовать константу `DestinationIncubatingAttributes.DESTINATION_ADDRESS`
  - Вместо `user_agent.original` рекомендуется использовать константу `UserAgentAttributes.USER_AGENT_ORIGINAL`.
  - Вместо `http.request.original.timeout` рекомендуется использовать константу `NabHttpAttributes.HTTP_REQUEST_ORIGINAL_TIMEOUT`.
  - Вместо `http.request.timeout` рекомендуется использовать константу `NabHttpAttributes.HTTP_REQUEST_TIMEOUT`.

### Устарело

- Начиная с этой версии некоторые span атрибуты OpenTelemetry помечаются как deprecated и будут удалены в будущих версиях. Вместе со старыми 
  атрибутами будут добавлены новые аналоги, что приведёт к увеличению потребляемого места в вашем хранилище OpenTelemetry.
  Если вы не знаете зачем вам могли бы понадобиться сразу 2 набора атрибутов в этой переходной версии - сразу обновляйтесь на последующие
  версии, с целью экономии места в хранилища.
  Изменения в атрибутах:
  - Вместо `SemanticAttributes.CODE_FUNCTION` (здесь и далее в скобках будет приводиться текстовое значение, которое нужно поискать в коде
    текстовым поиском помимо константы; если вы нашли такое текстовое значение, то рекомендуется заменить его на константу; `code.function`)
    и `SemanticAttributes.CODE_NAMESPACE` (`code.namespace`) нужно использовать 1 общий атрибут
    `CodeAttributes.CODE_FUNCTION_NAME` (`code.function.name`). Смотри подробнее в Javadoc `SemanticAttributesForRemoval.CODE_FUNCTION`,
    `SemanticAttributesForRemoval.CODE_NAMESPACE` как они соотносятся.  
  - Вместо `SemanticAttributes.HTTP_CLIENT_IP` (`http.client_ip`) нужно использовать `ClientAttributes.CLIENT_ADDRESS` (`client.address`).
  - Вместо `SemanticAttributes.HTTP_HOST` (`http.host`) нужно использовать `ServerAttributes.SERVER_ADDRESS` (`server.address`).
  - Вместо `SemanticAttributes.HTTP_METHOD` (`http.method`) нужно использовать `HttpAttributes.HTTP_REQUEST_METHOD` (`http.request.method`).
  - Вместо текстового атрибута `http.request.cloud.region` (константы раньше не было) нужно использовать
    `NabPeerAttributes.PEER_CLOUD_AVAILABILITY_ZONE` (`peer.cloud.availability_zone`).
  - Вместо `SemanticAttributes.HTTP_SCHEME` (`http.scheme`) нужно использовать UrlAttributes.URL_SCHEME (`url.scheme`).
  - Вместо `SemanticAttributes.HTTP_STATUS_CODE` (`http.status_code`) нужно использовать
    `HttpAttributes.HTTP_RESPONSE_STATUS_CODE` (`http.response.status_code`).
  - Вместо `SemanticAttributes.HTTP_TARGET` (`http.target`) нужно использовать 2 атрибута: `UrlAttributes.URL_PATH` (`url.path`) и
    `UrlAttributes.URL_QUERY` (`url.query`). Смотри подробнее в Javadoc `SemanticAttributesForRemoval.HTTP_TARGET` как они соотносятся.
  - Вместо `SemanticAttributes.HTTP_URL` (`http.url`) нужно использовать `UrlAttributes.URL_FULL` (`url.full`).
  - Вместо `SemanticAttributes.MESSAGING_KAFKA_CLIENT_ID` (`messaging.kafka.client_id`) нужно использовать
    `MessagingIncubatingAttributes.MESSAGING_CLIENT_ID` (`messaging.client.id`).
  - Вместо `SemanticAttributes.MESSAGING_KAFKA_CONSUMER_GROUP` (`messaging.kafka.consumer.group`) нужно использовать
    `MessagingIncubatingAttributes.MESSAGING_CONSUMER_GROUP_NAME` (`messaging.consumer.group.name`).
  - Вместо `SemanticAttributes.MESSAGING_OPERATION` (`messaging.operation`) нужно использовать
    `MessagingIncubatingAttributes.MESSAGING_OPERATION_TYPE` (`messaging.operation.type`).
    При этом константа значений тоже меняется: вместо `SemanticAttributes.MessagingOperationValues` надо использовать
    `MessagingIncubatingAttributes.MessagingOperationTypeIncubatingValues`.

### Удалено

- Удаление TopicPartitionsMonitoring.

### Инструкции

1. Обновление Spring Framework до 6.2.10. Мигрировать в соответствии с Release notes:
   - [Spring Framework 6.1 Release Notes](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-6.1-Release-Notes),
   - [Spring Framework 6.2 Release Notes](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-6.2-Release-Notes).

   Из основных изменений:
   - Если использовался статический метод `org.springframework.http.HttpMethod.resolve`, то его надо заменить на
     `org.springframework.http.HttpMethod.valueOf`. Разница в том, что в последнем больше нельзя передавать `null` - сделайте проверку
     самостоятельно если требуется.
   - Если использовался метод `org.springframework.http.HttpRequest.getMethodValue()`, то его надо заменить на
     `org.springframework.http.HttpRequest.getMethod().name()`.
   - Если использовали `org.springframework.http.client.HttpComponentsClientHttpRequestFactory.setReadTimeout(int)`, то теперь вместо него
     надо передавать предварительно настроенный `HttpClient` в конструкторе `HttpComponentsClientHttpRequestFactory(HttpClient)`. В цепочке
     настройки необходимо вызвать `SocketConfig.Builder.setSoTimeout(Timeout)` для правильного `SocketConfig`, после чего пробросить его в
     `org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder.setDefaultSocketConfig(SocketConfig)`, затем `ConnectionManager`
     в `org.apache.hc.client5.http.impl.classic.HttpClientBuilder.setConnectionManager(HttpClientConnectionManager)`.
   - Если использовался метод `org.springframework.http.client.reactive.ClientHttpResponse.getRawStatusCode()`, то его надо заменить на
     `org.springframework.http.client.reactive.ClientHttpResponse.getStatusCode().value()`.
   - Удалено несколько конструкторов в `org.springframework.web.HttpRequestMethodNotSupportedException`; если использовали их, то замените
     на `HttpRequestMethodNotSupportedException(String, Collection)`.
   - Замена аннотаций `@MockBean` и `@SpyBean` на `@MockitoBean` и `@MockitoSpyBean` соответственно.
2. Обновление Jetty до 12.0.25. Мигрировать в соответствии с [гайдом](https://jetty.org/docs/jetty/12/programming-guide/migration/11-to-12.html).
   Из основных изменений:
   - Если использовалась зависимость `org.eclipse.jetty.websocket:websocket-jakarta-server`, то её надо заменить на
     `org.eclipse.jetty.ee10.websocket:jetty-ee10-websocket-jakarta-server`.
   - Если использовалась зависимость `org.eclipse.jetty.websocket:websocket-jetty-server`, то вместо неё надо добавить 2 артефакта
     `org.eclipse.jetty.websocket:jetty-websocket-jetty-server` и `org.eclipse.jetty.ee10.websocket:jetty-ee10-websocket-jetty-server`.
   - Если использовалась зависимость `org.eclipse.jetty:apache-jsp`, то её надо заменить на
     `org.eclipse.jetty.ee10:jetty-ee10-apache-jsp`.
   - Если использовалась зависимость `org.eclipse.jetty:jetty-annotations`, то её надо заменить на `org.eclipse.jetty.ee10:jetty-ee10-annotations`.
   - Если использовалась зависимость `org.eclipse.jetty:jetty-jspc-maven-plugin`, то её надо заменить на
     `org.eclipse.jetty.ee10:jetty-ee10-jspc-maven-plugin`.
   - Если использовалась зависимость `org.eclipse.jetty:jetty-maven-plugin`, то её надо заменить на `org.eclipse.jetty.ee10:jetty-ee10-maven-plugin`.
   - Если использовалась зависимость `org.eclipse.jetty:jetty-plus`, то её надо заменить на `org.eclipse.jetty.ee10:jetty-ee10-plus`.
   - Если использовалась зависимость `org.eclipse.jetty:jetty-runner`, то её надо заменить на `org.eclipse.jetty.ee10:jetty-ee10-runner`.
   - Если использовалась зависимость `org.eclipse.jetty:jetty-servlet`, то её надо заменить на `org.eclipse.jetty.ee10:jetty-ee10-servlet`.
   - Если использовалась зависимость `org.eclipse.jetty:jetty-webapp`, то её надо заменить на `org.eclipse.jetty.ee10:jetty-ee10-webapp`.
   - Обновить import'ы.
   - Поменялись сигнатуры классов `AbstractConfiguration`, `SecurityHandler`, `SessionHandler` - обновить на аналоги.
   - Вместо метода `WebAppContext.setResourceBase` теперь нужно использовать `WebAppContext.setBaseResourceAsString`.
3. Обновление Logback до 1.5.18. Если в тестах появились ошибки, связанные с MDCAdapter, то необходимо
   - Убедиться, что в LoggingEvent установлен LoggerContext -> `event.setLoggerContext(context)`;
   - Убедиться, что в LoggerContext установлен MDCAdapter -> адаптер по умолчанию из старых версий:
     `context.setMDCAdapter((LogbackMDCAdapter) MDC.getMDCAdapter())`;

     [Подробнее]( https://github.com/spring-projects/spring-boot/issues/36177).
4. Обновление Hibernate до 6.6.42.Final. Мигрировать в соответствии с гайдами:
   - [6.4 Migration Guide](https://docs.jboss.org/hibernate/orm/6.4/migration-guide/migration-guide.html),
   - [6.5 Migration Guide](https://docs.jboss.org/hibernate/orm/6.5/migration-guide/migration-guide.html),
   - [6.6 Migration Guide](https://docs.jboss.org/hibernate/orm/6.6/migration-guide/migration-guide.html)

   Из основных изменений:
   - Если аннотация `@GeneratedValue` навешивается на поле, на котором нет аннотации `@Id`, то `@GeneratedValue` нужно удалить.
   - Перейти на новый стиль id generator'ов

     Было:
     ```java
     @GeneratedValue(generator = "my_seq_gen")
     @GenericGenerator(
         name = "my_seq_gen",
         strategy = "sequence",
         parameters = {
             @Parameter(name = SequenceStyleGenerator.SEQUENCE_PARAM, value = "my_sequence"),
             @Parameter(name = SequenceStyleGenerator.INCREMENT_PARAM, value = "1")
         }
     )
     ```
     Стало:
     ```java
     @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "my_seq_gen")
     @SequenceGenerator(name = "my_seq_gen", sequenceName = "my_sequence", allocationSize = 1)
     ```
     Было:
     ```java
     @GeneratedValue(generator = "uuid")
     @GenericGenerator(name = "uuid", strategy = "org.hibernate.id.UUIDGenerator")
     ```
     Стало:
     ```java
     @UuidGenerator
     ```
     Для остальных стратегий смотри Javadoc IdGeneratorType
   - Найти использования ResultCheckStyle и заменить на Expectation

     Было: `@SQLInsert(check=ResultCheckStyle.COUNT)`

     Стало: `@SQLInsert(verify=Expectation.RowCount.class)`
   - `ReflectHelper.classForName` теперь возвращает `Class<?>` вместо `Class`. Если вам требуется продолжать использовать raw-типы, присвойте
      возвращаемое значение промежуточной переменной:
      ```java
      Class clazz = ReflectHelper.classForName(enumClassName, this.getClass());
      var enumClass = clazz.asSubclass(Enum.class);
      ```
   - Hibernate в целом стал намного строже и теперь уведомляет об ошибках, на которые раньше закрывал глаза. Среди таких ошибок:
     - Двойной ManyToMany bidirectional-маппинг через `@JoinTable` вместо `mappedBy` на одной из сторон.
     - Двойной маппинг foreign key: через Entity и ещё раз id отдельно (может быть неочевидная ошибка вида
       `persistent instance references an unsaved transient instance`).
     - `@Batch` на не-коллекциях.
     - Использование `persist` вместо `merge` для новых сущностей, где идентификатор назначается вручную (Ошибки могут выглядеть так:
       `org.hibernate.StaleObjectStateException: Row was updated or deleted by another transaction (or unsaved-value mapping was incorrect))`.
5. Обновление Jackson с 2.15.4 до 2.19.2.
   - Jackson стал строже и может выдавать ошибку
     `com.fasterxml.jackson.databind.exc.InvalidDefinitionException: Problem deserializing 'setterless' property`, если у вас название поля в
     сеттере (часть имени сеттера после "set") называлось не так как в геттере. Небходимо переименовать такие сеттеры.
   - Порядок полей при сериализации мог изменится. Если вы полагались на этот порядок (например, в equals/hashcode), то могут возникнуть
     ошибки. Необходимо обновить код, чтобы он учитывал новый порядок (**НЕ рекомендуется**) или отрефакторить код, чтобы он не зависел от
     порядка (**РЕКОМЕНДУЕТСЯ**).
6. Вместо `SemanticAttributes.HTTP_ROUTE` нужно использовать `HttpAttributes.HTTP_ROUTE`.
   
   Внутреннее значение спана при этом **НЕ меняется**.
7. Вместо `SemanticAttributes.MESSAGING_DESTINATION_NAME` нужно использовать `MessagingIncubatingAttributes.MESSAGING_DESTINATION_NAME`.
   
   Внутреннее значение спана при этом **НЕ меняется**.
8. Вместо `SemanticAttributes.MESSAGING_KAFKA_MESSAGE_KEY` нужно использовать `MessagingIncubatingAttributes.MESSAGING_KAFKA_MESSAGE_KEY`.

   Внутреннее значение спана при этом **НЕ меняется**.
9. Вместо `SemanticAttributes.MESSAGING_SYSTEM` нужно использовать `MessagingIncubatingAttributes.MESSAGING_SYSTEM`.

   Внутреннее значение спана при этом **НЕ меняется**.
   
   Для значений атрибута теперь нужно использовать константу `MessagingIncubatingAttributes.MessagingSystemIncubatingValues`.
10. Вместо `SemanticAttributes.PEER_SERVICE` нужно использовать `PeerIncubatingAttributes.PEER_SERVICE`.

    Внутреннее значение спана при этом **НЕ меняется**.
11. `Вместо ResourceAttributes.SERVICE_NAME` нужно использовать `ServiceAttributes.SERVICE_NAME`.

    Внутреннее значение спана при этом **НЕ меняется**.
12. Для всех остальных deprecated атрибутов вида `SemanticAttributes.CODE_FUNCTION` нужно поменять префикс констант на
    `SemanticAttributesForRemoval.CODE_FUNCTION`. Внутренние значение спанов при этом **НЕ меняются**. Это нужно делать только если вы не можете
    мигрировать с этих deprecated атрибутов и вам нужно временно сохранить старые значения для обратной совместимости.
13. Меняется формат значения в span атрибуте `SemanticAttributesForRemoval.CODE_NAMESPACE` (`code.namespace`). Вместо `class.getCanonicalName()`
    теперь используется `class.getName()`. Т.е. вместо `ru.hh.nab.MyClass.MyInnerStaticClass` будет `ru.hh.nab.MyClass$MyInnerStaticClass`
    Адаптируйте код если у вас были завязки на старый формат.
14. Вместо `.addAttributesExtractor(NetClientAttributesExtractor.create(new NabJdbcNetAttributesGetter()))` используйте
    `.addAttributesExtractor(ServerAttributesExtractor.create(new NabJdbcNetworkAttributesGetter()))`
15. `TopicPartitionsMonitoring` удалён. Если использовали его явно - реализуйте его самостоятельно. Также `TopicPartitionsMonitoring` больше
    не нужно передавать в конструктор `KafkaConsumer`.

## [17.0.0] - 2025-08-01

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

### Инструкции

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

## [11.0.0] - 2025-03-05

### Добавлено

- Добавлена новая иерархия RuntimeException для nab-kafka.
- Добавлена новая ошибка, сообщающая о нерабочих конфигурациях.
- Добавлен новый метод `ConsumerBuilder<T> withDlq(String destination, KafkaProducer producer);`.
- Добавлен новый метод `void nAck(ConsumerRecord<String, T> message);`.
- Добавлен новый метод `void nAck(Collection<ConsumerRecord<String, T>> messages);`.

### Изменено

- Метод `ConsumerConsumingState<T> getConsumingState()` переименован в `ConsumerContext<T> getConsumerContext()` и более не находится в публичном
  доступе.
- Изменен метод `void retry(ConsumerRecord<String, T> message, Throwable error);`.
- Класс ConsumerConsumingState переименован в ConsumerContext и более не находится в публичном доступе.
- KafkaConsumer научился пропускать corrupted сообщения.
- KafkaConsumer шлет новую метрику `nab.kafka.errors.records.deserialization.count`, которая показывает кол-во corrupted сообщений пришедших в сервис.
- ConsumerBuilder#withRetries в случае исчерпания попыток для retry-ев сообщения отправляется в DLQ, если конфигурация для DLQ была включена.
- Метод `DefaultConsumerBuilder#build()` теперь кидает новые ConfigurationException, если конфигурация не является рабочей.

### Удалено

- Удалено явное смещение offset-а после процессинга одного сообщения, теперь это делает acknowledge в конце batch-а.

## Инструкции

1. Прекратить использовать KafkaConsumer#getConsumingState, это внутреннее api
2. Прекратить использовать возвращаемое значение от Ack#retry, оно не означало конец исполнения retry операции
3. Прекратить использовать ConsumerConsumingState, это внутреннее api
4. Если у вас были собственные решения для пропуска corrupted сообщений, то их можно и нужно удалить

## [8.0.0] - 2024-11-18

1. KafkaConsumer теперь умеет в ретраи. TLDR: используйте ConsumerBuilder::withRetries, см. доку к этому методу. Пример в ConsumerRetriesTest.
2. KafkaConsumer теперь корректно стартует и останавливается Spring контекстом, если создан как самостоятельный bean.
3. Удален SimpleKafkaConsumer.
4. Удален метод ConsumerBuilder::withAckProvider.

## Инструкции

1. Заменяем kafkaConsumerFactory.subscribe(...) на kafkaConsumerFactory.builder(...).build().start()

   было
   ```java
   kafkaConsumerFactory.subscribe(
       topicName,
       operationName,
       Message.class,
       this::processMessages
   );
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
