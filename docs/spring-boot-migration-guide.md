# Spring Boot migration guide [DRAFT]

[//]: # (TODO: Версия указана для того, чтобы было проще дорабатывать инструкцию при выпуске последующих "alpha" версий. Необходимо удалить указание версии наба после выпуска финальной версии)
Инструкция актуальна для версии наба 10.1.0.jakarta-rc1.spring-boot-rc1.

### pom.xml

1. Обновить `common-pom` до версии `2.1.0.jakarta-rc3.spring-boot-rc1` или выше. В enforcer добавлено правило для проверки того, что в сервис не
   тянутся никакие tomcat зависимости. Если сборка проходит успешно, то ничего делать не нужно. Если сборка падает из-за того, что в ваш сервис все же
   тянутся какие-то tomcat зависимости, необходимо их удалить.

   [//]: # (TODO: после финализации работ указать актуальную версию common-pom)

2. Если в сервисе используется `service-common-config`, то обновить его до версии `14.0.0.jakarta-rc1.spring-boot-rc1` или выше.

   Если в сервисе не используется `service-common-config`, то:
    - обновить `nuts-and-bolts` до версии `10.1.0.jakarta-rc1.spring-boot-rc1` или выше
    - вместо зависимости `nab-starter` подключить `nab-web-spring-boot-starter`

   [//]: # (TODO: после финализации работ указать актуальную версию service-common-config и nuts-and-bolts)

3. Вместо зависимости `nab-websocket` подключить `nab-websocket-spring-boot-starter`.

4. В помнике сервиса подключить плагин `spring-boot-maven-plugin`. Плагин генерит файл `build-info.properties`, который содержит различную информацию
   о билде, и кладет его в директорию `target/classes/META-INF`. Значение всех пропертей из файла `build-info.properties` в коде можно получить с
   помощью бина [BuildProperties](https://docs.spring.io/spring-boot/docs/3.1.0/api/org/springframework/boot/info/BuildProperties.html).

   <details>
        <summary>Пример подключения</summary>

   ```xml
   <build>
       <plugins>
           ...
           <plugin>
               <groupId>org.springframework.boot</groupId>
               <artifactId>spring-boot-maven-plugin</artifactId>
           </plugin>
           ...
       </plugins>
   </build>
   ```
   </details>

   <details>
        <summary>Пример содержимого файла build-info.properties</summary>

    ```properties
    build.artifact=hh-app-service
    build.group=ru.hh.app
    build.name=hh-app-service
    build.time=2024-12-06T12\:10\:40.426Z
    build.version=1.0.0-SNAPSHOT
    ```
   </details>

   <details>
        <summary>Как добавить кастомные проперти в файл build-info.properties</summary>

   Чтобы добавить кастомные проперти в файл `build-info.properties`, необходимо перечислить их в конфигурации плагина в `additionalProperties`.
   Подробнее о конфигурации плагина можно почитать в
   документации [10.1. spring-boot:build-info](https://docs.spring.io/spring-boot/docs/3.1.0/maven-plugin/reference/htmlsingle/#goals-build-info).

    ```xml
    <build>
        <plugins>
            ...
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <additionalProperties>
                        <build.outputDirectory>${project.build.outputDirectory}</build.outputDirectory>
                        <custom.property>foo</custom.property>
                    </additionalProperties>
                </configuration>
            </plugin>
            ...
        </plugins>
    </build>
    ```
   </details>

### Код

1. Из `src/main/resources` удалить файл `project.properties`, вместо него теперь генерится `build-info.properties`. Если `project.properties` содержит
   проперти, которые по дефолту не добавляются в `build-info.properties`, необходимо эти проперти перечислить в
   конфигурации `spring-boot-maven-plugin`.

2. В `src/main/resources` добавить файл `application.properties` со следующими пропертями:

    - `spring.config.import` - нужно через `,` перечислить все `*.properties` файлы из deploy репозитория, необходимые для работы вашего
      сервиса (`service.properties`, `hibernate.properties`, `db-settings.properties` и т.д.). При указании пути к файлу необходимо использовать
      системную пропертю `settingsDir` (она уже добавляется в конфигурации `jib-maven-plugin` в помнике вашего сервиса, дополнительно для ее
      использования ничего делать не нужно). Все проперти из перечисленных файлов будут добавлены в спринговый `Environment`.
    - `spring.profiles.active` - нужно активировать профиль `main`

   <details>
        <summary>Пример</summary>

   ```properties
   spring.config.import=file:${settingsDir}/service.properties,file:${settingsDir}/hibernate.properties,file:${settingsDir}/db-settings.properties
   spring.profiles.active=main
   ```
   </details>

3. На main class вашего приложения повесить аннотацию `@NabApplication`. Данная аннотация - это по большей части
   копипаста [@SpringBootApplication](https://docs.spring.io/spring-boot/docs/3.1.0/api/org/springframework/boot/autoconfigure/SpringBootApplication.html).
   Единственное отличие заключается в том, что при использовании `@NabApplication` не работает component scan.

   <details>
        <summary>Пример</summary>

   ```java
   @NabApplication
   public class HhApp {
     public static void main(String[] args) {
       ...
     }
   }
   ```
   </details>

4. На main class вашего приложения повесить аннотацию `@Import` и указать в ней `HhAppCommonConfig.class` и `HhAppProdConfig.class`. Причем необходимо
   явно указывать оба config класса, даже несмотря на то, что `HhAppCommonConfig` импортируется в `HhAppProdConfig`. В противном случае могут падать
   тесты, так как не поднимется контекст.

   <details>
        <summary>Пример</summary>

   ```java
   @NabApplication
   @Import({
       HhAppCommonConfig.class,
       HhAppProdConfig.class,
   })
   public class HhApp {
     public static void main(String[] args) {
       ...
     }
   }
   ```
   </details>

   [//]: # (TODO: Вообще хотелось бы уйти от этих Common и Prod конфигов в сервисах, как будто лучше раскладывать бины по @Configuration классам по логическому признаку. Возможно это получится сделать после того, когда во всех либах выпилим Common и Prod конфиги. Нужно пересмотреть данный пункт после финализации всех работ)

5. В методе `main` для старта приложения вместо `NabApplication`/`NabApplicationBuilder`
   использовать [SpringApplication](https://docs.spring.io/spring-boot/docs/3.1.0/api/org/springframework/boot/SpringApplication.html) / [SpringApplicationBuilder](https://docs.spring.io/spring-boot/docs/3.1.0/api/org/springframework/boot/builder/SpringApplicationBuilder.html).
   Более подробную информацию можно посмотреть в
   документации [1. SpringApplication](https://docs.spring.io/spring-boot/docs/3.1.0/reference/html/features.html#features.spring-application).

   <details>
        <summary>Пример</summary>

   ```java
   @NabApplication
   @Import({
       HhAppCommonConfig.class,
       HhAppProdConfig.class,
   })
   public class HhApp {
     public static void main(String[] args) {
       SpringApplication.run(HhApp.class, args);
     }
   }
   ```
   </details>

6. Для конфигурации context path вместо вызова метода `NabApplicationBuilder.setContextPath` добавить настройку `server.servlet.context-path`
   в `application.properties`.

   <details>
        <summary>Пример</summary>

   Было:

   ```java
   public class HhApp {
     public static void main(String[] args) {
       NabApplication
           .builder()
           .setContextPath("/hh-app")
           .build()
           .run(HhAppProdConfig.class);
     }
   }
   ```
   Стало:
    ```properties
    server.servlet.context-path=/hh-app
    ```
   </details>

7. Для конфигурации [WebAppContext](https://javadoc.jetty.org/jetty-11/org/eclipse/jetty/webapp/WebAppContext.html) вместо вызова
   метода `NabApplicationBuilder.configureWebapp`
   реализовать [Configuration](https://javadoc.jetty.org/jetty-11/org/eclipse/jetty/webapp/Configuration.html)
   и добавить бин в контекст спринга. При реализации `Configuration` можно наследоваться
   от [AbstractConfiguration](https://javadoc.jetty.org/jetty-11/org/eclipse/jetty/webapp/AbstractConfiguration.html), чтобы не реализовывать все
   методы.

   <details>
        <summary>Пример</summary>

   Было:

   ```java
   public class HhApp {
     public static void main(String[] args) {
       NabApplication
           .builder()
           .configureWebapp(((webAppContext, applicationContext) -> {
             ...
           }))
           .build()
           .run(HhAppProdConfig.class);
     }
   }
   ```

   Стало:

   ```java
   public class WebAppConfiguration extends AbstractConfiguration {
    
     @Override
     public void configure(WebAppContext context) {
       ...
     }
   }
   ```

   ```java
   @Configuration
   @Import({
       WebAppConfiguration.class,
   })
   public class HhAppCommonConfig {
   }
   ```
   </details>

8. Для
   конфигурации [ServletContext](https://javadoc.io/doc/jakarta.servlet/jakarta.servlet-api/6.0.0/jakarta.servlet/jakarta/servlet/ServletContext.html)
   вместо вызова метода `NabApplicationBuilder.onWebAppStarted`
   реализовать [ServletContextInitializer](https://docs.spring.io/spring-boot/docs/3.1.0/api/org/springframework/boot/web/servlet/ServletContextInitializer.html)
   и добавить бин в контекст спринга.

   <details>
        <summary>Пример</summary>

   Было:

   ```java
   public class HhApp {
     public static void main(String[] args) {
       NabApplication
           .builder()
           .onWebAppStarted((servletContext, applicationContext) -> {
             ...
           })
           .build()
           .run(HhAppProdConfig.class);
     }
   }
   ```

   Стало:

   ```java
   public class SomeServletContextInitializer implements ServletContextInitializer {
     @Override
     public void onStartup(ServletContext servletContext) {
       ...
     }
   }
   ```

   ```java
   @Configuration
   @Import({
       SomeServletContextInitializer.class,
   })
   public class HhAppCommonConfig {
   }
   ```
   </details>

9. Для
   регистрации [ServletContextListener](https://javadoc.io/doc/jakarta.servlet/jakarta.servlet-api/6.0.0/jakarta.servlet/jakarta/servlet/ServletContextListener.html)
   вместо вызова метода `NabApplicationBuilder.addListener`/`NabApplicationBuilder.addListenerBean` добавить в контекст спринга
   бин `ServletContextListener`.

   <details>
        <summary>Пример</summary>

   Было:

   ```java
   public class HhApp {
     public static void main(String[] args) {
       NabApplication
           .builder()
           .addListener(new HhAppServletContextListener())
           .build()
           .run(HhAppProdConfig.class);
     }
   }
   ```

   Стало:

   ```java
   @Configuration
   @Import({
       HhAppServletContextListener.class,
   })
   public class HhAppCommonConfig {
   }
   ```
   </details>

10. Для регистрации сервлета вместо вызова метода `NabApplicationBuilder.addServlet` добавить в контекст спринга
    бин [ServletRegistrationBean](https://docs.spring.io/spring-boot/docs/3.1.0/api/org/springframework/boot/web/servlet/ServletRegistrationBean.html).

    <details>
        <summary>Пример</summary>

    Было:

    ```java
    public class HhApp {
      public static void main(String[] args) {
        NabApplication
            .builder()
            .addServlet(context -> new HhAppServlet())
            .bindTo("/hh-app/*")
            .build()
            .run(HhAppProdConfig.class);
      }
    }
    ```

    Стало:

    ```java
    @Configuration
    public class HhAppCommonConfig {
    
      @Bean
      public ServletRegistrationBean<HhAppServlet> hhAppServlet() {
        ServletRegistrationBean<HhAppServlet> registration = new ServletRegistrationBean<>(new HhAppServlet());
        registration.addUrlMappings("/hh-app/*");
        return registration;
      }
    }
    ```
    </details>

11. Для регистрации фильтра вместо вызова
    метода `NabApplicationBuilder.addFilter`/`NabApplicationBuilder.addFilterBean`/`NabApplicationBuilder.addFilterHolderBean` добавить в контекст
    спринга
    бин [FilterRegistrationBean](https://docs.spring.io/spring-boot/docs/3.1.0/api/org/springframework/boot/web/servlet/FilterRegistrationBean.html).
    При конфигурации бина `FilterRegistrationBean` нужно иметь в виду, что дефолтное значение dispatcherTypes в набе и спринг буте отличается. В набе
    по дефолту `dispatcherTypes=EnumSet.allOf(DispatcherType.class)`. В спринг буте:

    - Если класс фильтра наследуется
      от [OncePerRequestFilter](https://docs.spring.io/spring-framework/docs/6.0.9/javadoc-api/org/springframework/web/filter/OncePerRequestFilter.html),
      то по дефолту `dispatcherTypes=EnumSet.allOf(DispatcherType.class)`
    - Если класс фильтра НЕ наследуется от `OncePerRequestFilter`, то по дефолту `dispatcherTypes=EnumSet.of(DispatcherType.REQUEST)`

    <details>
        <summary>Пример 1</summary>

    Было:

    ```java
    public class HhApp {
      public static void main(String[] args) {
        NabApplication
            .builder()
            .addFilter(HhAppFilter.class)
            .bindToRoot()
            .build()
            .run(HhAppProdConfig.class);
      }
    }
    ```

    Стало:

    ```java
    @Configuration
    public class HhAppCommonConfig {
    
      @Bean
      public FilterRegistrationBean<HhAppFilter> hhAppFilter() {
        FilterRegistrationBean<HhAppFilter> registration = new FilterRegistrationBean<>(new HhAppFilter()); 
        // Если HhAppFilter НЕ является наследником OncePerRequestFilter, то по дефолту dispatcherTypes=EnumSet.of(DispatcherType.REQUEST). 
        // Если нужно, чтобы использовались те же dispatcherTypes, что и раньше в набе, то необходимо явно указать их. 
        registration.setDispatcherTypes(EnumSet.allOf(DispatcherType.class));
        return registration;
      }
    }
    ```

    ```java
    @Configuration
    public class HhAppCommonConfig {
    
      @Bean
      public FilterRegistrationBean<HhAppFilter> hhAppFilter() {
        // Если HhAppFilter является наследником OncePerRequestFilter, то по дефолту dispatcherTypes=EnumSet.allOf(DispatcherType.class).
        // В этом случае используются те же dispatcherTypes, что и раньше в набе, поэтому можно явно не конфигурировать dispatcherTypes. 
        return new FilterRegistrationBean<>(new HhAppFilter());
      }
    }
    ```
    </details>

    <details>
        <summary>Пример 2</summary>

    Было:

    ```java
    public class HhApp {
      public static void main(String[] args) {
        NabApplication
            .builder()
            .addFilter(HhAppFilter.class)
            .setDispatchTypes(EnumSet.of(DispatcherType.REQUEST))
            .addInitParameter("someParameter", "someValue")
            .bindTo("/hh-app/*")
            .build()
            .run(HhAppProdConfig.class);
      }
    }
    ```

    Стало:

    ```java
    @Configuration
    public class HhAppCommonConfig {
    
      @Bean
      public FilterRegistrationBean<HhAppFilter> hhAppFilter() {
        FilterRegistrationBean<HhAppFilter> registration = new FilterRegistrationBean<>(new HhAppFilter());
        registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
        registration.addUrlPatterns("/hh-app/*");
        registration.addInitParameter("someParameter", "someValue");
        return registration;
      }
    }
    ```
    </details>

    Также следует иметь в виду, что в набе фильтры отрабатывали в том порядке, в котором они регистрировались. В спринг буте у всех фильтров,
    зарегистрированных с помощью `FilterRegistrationBean`, `order=Ordered.LOWEST_PRECEDENCE`, и нет никаких гарантий касательно того, в каком порядке
    будут отрабатывать зарегистрированные фильтры. Если требуется, чтобы фильтры гарантированно отрабатывали в определенном порядке, тогда необходимо
    явно настроить порядок с помощью метода `FilterRegistrationBean.setOrder`. При настройке order можно использовать константы
    из `ru.hh.nab.common.servlet.ServletFilterPriorities`.

    <details>
        <summary>Пример</summary>

    Было:

    ```java
    // В данном примере fooFilter будет гарантированно отрабатывать раньше, чем barFilter, так как fooFilter в коде регистрируется раньше, чем barFilter
    public class HhApp {
      public static void main(String[] args) {
        NabApplication
            .builder()
            .addFilter(FooFilter.class)
            .bindToRoot()
            .addFilter(BarFilter.class)
            .bindToRoot()
            .build()
            .run(HhAppProdConfig.class);
      }
    }
    ```

    Стало:

    ```java
    // В данном примере fooFilter будет гарантированно отрабатывать раньше, чем barFilter, так как fooFilter order < barFilter order
    @Configuration
    public class HhAppCommonConfig {
    
      @Bean
      public FilterRegistrationBean<FooFilter> fooFilter() {
        FilterRegistrationBean<FooFilter> registration = new FilterRegistrationBean<>(new FooFilter());
        registration.setOrder(ServletFilterPriorities.USER); // order=5000
        return registration;
      }
    
      @Bean
      public FilterRegistrationBean<BarFilter> barFilter() {
        FilterRegistrationBean<BarFilter> registration = new FilterRegistrationBean<>(new BarFilter());
        registration.setOrder(ServletFilterPriorities.USER + 100); // order=5100
        return registration;
      }
    }
    ```
    </details>

12. Для конфигурации jersey вместо вызова методов `NabApplicationBuilder.configureJersey`:

    - добавить класс, который наследуется
      от [ResourceConfig](https://www.javadoc.io/doc/org.glassfish.jersey.core/jersey-server/3.1.0/org/glassfish/jersey/server/ResourceConfig.html) и
      зарегистрировать в нем все необходимые ресурсы. Для конфигурации application path вместо вызова метода `bindTo` повесить на добавленный класс
      аннотацию [@ApplicationPath](https://jakarta.ee/specifications/restful-ws/3.1/apidocs/jakarta.ws.rs/jakarta/ws/rs/applicationpath)
    - добавить бин в контекст спринга

    <details>
        <summary>Пример</summary>

    Было:

    ```java
    public class HhApp {
      public static void main(String[] args) {
        NabApplication
            .builder()
            .configureJersey()
            .registerResources(FooResource.class)
            .registerResourceBean(applicationContext -> applicationContext.getBean(BarResource.class))
            .registerResourceWithContracts(BazResource.class, BazContract.class)
            .executeOnConfig(resourceConfig -> resourceConfig.register(QuxResource.class))
            .registerProperty("someProperty", "someValue")
            .bindTo("/rs/*")
            .build()
            .run(HhAppProdConfig.class);
      }
    }
    ```

    Стало:

    ```java
    @ApplicationPath("/rs")
    public class HhAppJerseyConfig extends ResourceConfig {
    
      public HhAppJerseyConfig(BarResource barResource) {
        register(FooResource.class);
        register(barResource);
        register(BazResource.class, BazContract.class);
        register(QuxResource.class);
        property("someProperty", "someValue");
      }
    }
    ```

    ```java
    @Configuration
    @Import({
        HhAppJerseyConfig.class,
    })
    public class HhAppCommonConfig {
    }
    ```
    </details>

13. В `@Configuration` классах удалить импорт `NabCommonConfig.class` и `NabProdConfig.class`.

14. Вместо бина `FileSettings` (помечен как deprecated) использовать
    бин [Environment](https://docs.spring.io/spring-framework/docs/6.0.9/javadoc-api/org/springframework/core/env/Environment.html) / [ConfigurableEnvironment](https://docs.spring.io/spring-framework/docs/6.0.9/javadoc-api/org/springframework/core/env/ConfigurableEnvironment.html) (
    в том числе в тестах). В наб также добавлен утилитный класс `EnvironmentUtils`, позволяющий получить необходимые настройки в
    виде `Properties`, `Map`, `List`, получить настройки по префиксу и т.д.

    <details>
        <summary>Пример</summary>

    Было:

    ```java
    @Bean
    public Object someBean(FileSettings fileSettings) {
      int optionalPropertyValue = fileSettings.getInteger("optional.property", 0);
      int requiredPropertyValue = Optional.ofNullable(fileSettings.getInteger("required.property")).orElseThrow();
      String notEmptyPropertyValue = fileSettings.getNotEmptyOrThrow("not.empty.property");
      List<String> listPropertyValue = fileSettings.getStringList("list.property");
      Properties subProperties = fileSettings.getSubProperties("some.prefix");
      return new Object();
    }
    ```

    Стало:

    ```java
    @Bean
    public Object someBean(ConfigurableEnvironment environment) {
      int optionalPropertyValue = environment.getProperty("optional.property", Integer.class, 0);
      int requiredPropertyValue = environment.getRequiredProperty("required.property", Integer.class);
      String notEmptyPropertyValue = EnvironmentUtils.getNotEmptyPropertyOrThrow(environment, "not.empty.property");
      List<String> listPropertyValue = EnvironmentUtils.getPropertyAsStringList(environment, "list.property");
      Properties subProperties = EnvironmentUtils.getSubProperties(environment, "some.prefix");
      return new Object();
    }
    ```
    </details>

    Также для получения настроек можно
    использовать [@Value](https://docs.spring.io/spring-framework/docs/6.0.9/javadoc-api/org/springframework/beans/factory/annotation/Value.html)
    и [@ConfigurationProperties](https://docs.spring.io/spring-boot/docs/3.1.0/api/org/springframework/boot/context/properties/ConfigurationProperties.html).
    Подробнее можно почитать в
    документации [2. Externalized Configuration](https://docs.spring.io/spring-boot/docs/3.1.0/reference/html/features.html#features.external-config).

15. Вместо бинов `String serviceName`, `String nodeName`, `String datacenter` использовать бин `InfrastructureProperties` (в том числе в тестах).

    <details>
        <summary>Пример</summary>

    Было:

    ```java
    @Bean
    public Object someBean(
        @Named(SERVICE_NAME) String serviceName,
        @Named(NODE_NAME) String nodeName,
        @Named(DATACENTER) String datacenter
    ) {
      return new Object();
    }
    ```

    Стало:

    ```java
    @Bean
    public Object someBean(InfrastructureProperties infrastructureProperties) {
      String serviceName = infrastructureProperties.getServiceName();
      String nodeName = infrastructureProperties.getNodeName();
      String datacenter = infrastructureProperties.getDatacenter();
      return new Object();
    }
    ```
    </details>

16. Вместо бина `Properties serviceProperties` (удален) использовать бины `InfrastructureProperties` и `Environment`/`ConfigurableEnvironment` (в том
    числе в тестах):

    - `InfrastructureProperties` - если требуется получить serviceName / nodeName / datacenter / datacenters
    - `Environment`/`ConfigurableEnvironment` - для получения остальных настроек

    <details>
        <summary>Пример</summary>

    Было:

    ```java
    @Bean
    public Object someBean(Properties serviceProperties) {
      String serviceName = serviceProperties.getProperty("serviceName");
      String nodeName = serviceProperties.getProperty("nodeName");
      String datacenter = serviceProperties.getProperty("datacenter");
      String datacenters = serviceProperties.getProperty("datacenters");
      String otherProperty = serviceProperties.getProperty("otherProperty");
      return new Object();
    }
    ```

    Стало:

    ```java
    @Bean
    public Object someBean(InfrastructureProperties infrastructureProperties, ConfigurableEnvironment environment) {
      String serviceName = infrastructureProperties.getServiceName();
      String nodeName = infrastructureProperties.getNodeName();
      String datacenter = infrastructureProperties.getDatacenter();
      List<String> datacenters = infrastructureProperties.getDatacenters();
      String otherProperty = environment.getProperty("otherProperty");
      return new Object();
    }
    ```
    </details>

    Если в вашем сервисе делается оверрайд бина `Properties serviceProperties`, его также необходимо удалить (в том числе в тестах).

17. Вместо бина `Properties projectProperties` (удален) использовать бин `BuildProperties`.

    <details>
        <summary>Пример</summary>

    Было:

    ```java
    @Bean
    public Object someBean(Properties projectProperties) {
      String projectName = projectProperties.getProperty("project.name");
      String projectVersion = projectProperties.getProperty("project.version");
      String customProperty = projectProperties.getProperty("custom.property");
      return new Object();
    }
    ```

    Стало:

    ```java
    @Bean
    public Object someBean(BuildProperties buildProperties) {
      String projectName = buildProperties.getName();
      String projectVersion = buildProperties.getVersion();
      String customProperty = buildProperties.get("custom.property");
      return new Object();
    }
    ```
    </details>

    Если в вашем сервисе делается оверрайд бина `Properties projectProperties`, его также необходимо удалить (в том числе в тестах).

18. Вместо бина `AppMetadata` (удален) использовать бины `InfrastructureProperties` и `BuildProperties`.

    <details>
        <summary>Пример</summary>

    Было:

    ```java
    @Bean
    public Object someBean(AppMetadata appMetadata) {
      String serviceName = appMetadata.getServiceName();
      String projectVersion = appMetadata.getVersion();
      long upTimeSeconds = appMetadata.getUpTimeSeconds();
      return new Object();
    }
    ```

    Стало:

    ```java
    @Bean
    public Object someBean(InfrastructureProperties infrastructureProperties, BuildProperties buildProperties) {
      String serviceName = infrastructureProperties.getServiceName();
      String projectVersion = buildProperties.getVersion();
      long upTimeSeconds = infrastructureProperties.getUpTime().toSeconds();
      return new Object();
    }
    ```
    </details>

    Если в вашем сервисе делается оверрайд бина `AppMetadata`, его также необходимо удалить (в том числе в тестах).

19. Удалить получение пропертей из файла с помощью метода `PropertiesUtils.fromFilesInSettingsDir` (помечен как deprecated).
    Необходимые `*.properties` файлы должны быть перечислены в `application.properties` в настройке `spring.config.import`. Все проперти из
    перечисленных файлов будут добавлены в спринговый `Environment`. Далее их можно получать в коде, используя предоставляемые спрингом
    механизмы. `Properties` бины, при создании которых вызывается метод `PropertiesUtils.fromFilesInSettingsDir`, также необходимо удалить.

    <details>
        <summary>Пример бина, который необходимо удалить</summary>

    ```java
    @Bean
    public Properties dbSettingsProperties() throws IOException {
      return PropertiesUtils.fromFilesInSettingsDir("db-settings.properties");
    }
    ```
    </details>

20. На класс `HhAppProdConfig` повесить аннотацию `@Profile(Profiles.MAIN)` или `@MainProfile`. Таким образом, "prod" бины будут созданы только в том
    случае, если активирован профиль `main`. Подробнее почитать о профилях можно в
    документации [3. Profiles](https://docs.spring.io/spring-boot/docs/3.1.0/reference/html/features.html#features.profiles).

    <details>
        <summary>Пример</summary>

    ```java
    @Configuration
    @MainProfile
    public class HhAppProdConfig {
    }
    ```
    </details>

    [//]: # (TODO: если после финализации всех работ получится избавиться в сервисах от Common и Prod конфигов, необходимо поправить данный пункт - если в импортах не будет никаких Prod конфигов из либ, то аннотацию можно не вешать на весь класс)

21. Удалить вызов метода `NabWebsocketConfigurator.configureWebsocket`, теперь для работы вебсокетов достаточно
    подключить `nab-websocket-spring-boot-starter`.

22. В классе, который указан в файле `src/main/resources/META-INF/services/ch.qos.logback.classic.spi.Configurator`, сконфигурировать логгер для main
    class'а, указав для него уровень логирования `INFO`.

    <details>
        <summary>Пример</summary>

    ```java
    public class HhAppLogbackConfigurator extends NabLogbackBaseConfigurator {
      @Override
      public void configure(LoggingContextWrapper context, HhMultiAppender service, HhMultiAppender libraries, SentryAppender sentry) {
        createLogger(context, HhApp.class, Level.INFO, false, List.of(service, sentry));
      }
    }
    ```
    </details>

23. Поправить импорты (часть классов из `nab-starter` переехала в `nab-web`):

    <details>
        <summary>Список изменившихся пакетов</summary>

    - `ru.hh.nab.starter.consul` -> `ru.hh.nab.web.consul`
    - `ru.hh.nab.starter.exceptions` -> `ru.hh.nab.web.exceptions`
    - `ru.hh.nab.starter.http` -> `ru.hh.nab.web.http`
    - `ru.hh.nab.starter.server` -> `ru.hh.nab.web.http`
    - `ru.hh.nab.starter.server.logging` -> `ru.hh.nab.web.http`
    - `ru.hh.nab.starter.server.cache` -> `ru.hh.nab.web.jersey.filter.cache`
    - `ru.hh.nab.starter.filters` -> сервлетные фильтры переехали в `ru.hh.nab.web.servlet.filter`; джерсевые фильтры переехали
      в `ru.hh.nab.web.jersey.filter`
    - `ru.hh.nab.starter.jersey` -> все классы, кроме `NabPriorities`, переехали в `ru.hh.nab.web.jersey.resolver`; класс `NabPriorities` переехал
      в `ru.hh.nab.web.jersey`
    - `ru.hh.nab.starter.server.logging` -> `ru.hh.nab.web.jetty`
    - `ru.hh.nab.starter.logging` + класс `NabLogbackBaseConfigurator` -> `ru.hh.nab.web.logging`
    </details>

24. Поправить юнит тесты.

    Вместо аннотации `@NabJunitWebConfig(TestConfig.class)` использовать:

    - `@ExtendWith(SpringExtensionWithFailFast.class)`
    - `@SpringBootTest(classes = TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`

    Для инжекта `ResourceHelper` вместо аннотации `@NabTestServer` использовать аннотацию `@Inject`.

    Удалить класс, имплементирующий интерфейс `OverrideNabApplication`.

    <details>
        <summary>Пример</summary>

    Было:

    ```java
    @NabJunitWebConfig({TestConfig.class})
    public abstract class HhAppTestBase {
    
      @NabTestServer(overrideApplication = SpringCtxForJersey.class)
      protected ResourceHelper resourceHelper;
    
      @Configuration
      public static class SpringCtxForJersey implements OverrideNabApplication {
        @Override
        public NabApplication getNabApplication() {
          return App.buildApplication();
        }
      }
    }
    ```

    Стало:

    ```java
    @ExtendWith(SpringExtensionWithFailFast.class)
    @SpringBootTest(classes = TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    public abstract class HhAppTestBase {
    
      @Inject
      protected ResourceHelper resourceHelper;
    }
    ```
    </details>

    Также можно унаследоваться от класса `WebTestBase`, тогда `@ExtendWith(SpringExtensionWithFailFast.class)` и инжект `ResourceHelper` можно
    удалить.

    <details>
        <summary>Пример</summary>

    ```java
    @SpringBootTest(classes = TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    public abstract class HhAppTestBase extends WebTestBase {
    }
    ```
    </details>

    Если аннотация `@NabJunitWebConfig` висела на классе `HhAppTestBase`, и в этом классе больше ничего нет (как показано в примере выше), то его
    можно удалить, а аннотацию `@SpringBootTest` повесить на тестовые классы, которые от него наследуются.

    <details>
        <summary>Пример</summary>

    Было:

    ```java
    public class HhAppTest extends HhAppTestBase {
      // tests
    }
    ```

    Стало:

    ```java
    @SpringBootTest(classes = TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    public class HhAppTest extends WebTestBase {
      // tests
    }
    ```
    </details>

    На класс `TestConfig` вместо аннотации `@Configuration` повесить аннотацию `@TestConfiguration`. Также из аннотации `@Import` можно
    удалить `HhAppCommonConfig.class`. В импорте данного класса нет необходимости, так как он уже импортируется в main class'е приложения.

    <details>
        <summary>Пример</summary>

    Было:

    ```java
    @Configuration
    @Import({
        NabTestConfig.class,
        HhAppCommonConfig.class,
    })
    public class TestConfig {
      // beans
    }
    ```

    Стало:

    ```java
    @TestConfiguration
    @Import({
        NabTestConfig.class,
    })
    public class TestConfig {
      // beans
    }
    ```
    </details>

    В `src/test/resources` добавить файл `application.properties` и скопипастить в него все проперти (кроме `log.*`) из `service-test.properties`.
    Если в файле `service-test.properties` нет никаких `log.*` пропертей, то можно просто переименовать его в `application.properties`. Далее в
    файле `application.properties` необходимо переименовать настройки.

    <details>
        <summary>Список настроек, которые нужно переименовать</summary>

    - `serviceName` -> `spring.application.name`
    - `http.cache.sizeInMB` -> `http.cache.sizeInMb`
    - `consul.wait.after.deregistration.millis` -> `consul.wait-after-deregistration-millis`
    - `statsd.queue.size` -> `statsd.queue-size`
    - `statsd.buffer.pool.size` -> `statsd.buffer-pool-size`
    - `jetty.host` -> `server.address`
    - `jetty.port` -> `server.port`
    - `jetty.maxThreads` -> `server.jetty.threads.max`
    - `jetty.minThreads` -> `server.jetty.threads.min`
    - `jetty.queueSize` -> `server.jetty.threads.max-queue-capacity`
    - `jetty.threadPoolIdleTimeoutMs` -> `server.jetty.threads.idle-timeout`
    - `jetty.acceptors` -> `server.jetty.threads.acceptors`
    - `jetty.selectors` -> `server.jetty.threads.selectors`
    - `jetty.connectionIdleTimeoutMs` -> `server.jetty.connection-idle-timeout`
    - `jetty.acceptQueueSize` -> `server.jetty.accept-queue-size`
    - `jetty.requestHeaderSize` -> `server.max-http-request-header-size`
    - `jetty.responseHeaderSize` -> `server.jetty.max-http-response-header-size`
    - `jetty.outputBufferSize` -> `server.jetty.output-buffer-size`
    - `jetty.stopTimeoutMs` -> `spring.lifecycle.timeout-per-shutdown-phase`
    </details>

    Часть настроек с дефолтными значениями подтягивается из nab-testbase (если в вашем `TestConfig` классе импортится `NabTestConfig`), и данные
    настройки добавляются в спринговый `Environment`. Если в ваших тестах не требуется оверрайд данных настроек, то их можно удалить
    из `application.properties`.

    <details>
        <summary>Список настроек, которые подтягиваются из nab-testbase</summary>

    ```properties
    spring.application.name=testService
    datacenter=test1
    datacenters=test1
    nodeName=testNode
    
    hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
    hibernate.hbm2ddl.auto=create
    hibernate.show_sql=false
    hibernate.format_sql=false
    
    kafka.common.security.protocol=PLAINTEXT
    kafka.producer.default.retries=3
    kafka.producer.default.linger.ms=10
    kafka.producer.default.batch.size=1
    kafka.consumer.default.auto.offset.reset=earliest
    kafka.consumer.default.fetch.max.wait.ms=250
    kafka.consumer.default.max.poll.interval.ms=5000
    kafka.consumer.default.max.poll.records=25
    kafka.consumer.default.nab_setting.poll.timeout.ms=500
    kafka.consumer.default.nab_setting.backoff.initial.interval=1
    kafka.consumer.default.nab_setting.backoff.max.interval=1
    ```
    </details>

    Также следует проанализировать остальные настройки в `application.properties` на предмет их необходимости. Возможно, часть настроек (например,
    связанных с конфигурацией сервера) можно удалить.

    <details>
        <summary>Пример</summary>

    Было:

    ```properties
    spring.application.name=test-hh-app
    datacenter=test-dc
    datacenters=test-dc
    nodeName=some-test-node
    
    allowCrossDCRequests=false
    
    server.port=0
    server.jetty.threads.max=15
    server.jetty.threads.min=15
    
    master.pool.maximumPoolSize=2
    readonly.pool.maximumPoolSize=2
    
    hh.app.foo=foo
    hh.app.bar=bar
    ```

    Стало:

    ```properties
    master.pool.maximumPoolSize=2
    readonly.pool.maximumPoolSize=2
    
    hh.app.foo=foo
    hh.app.bar=bar
    ```
    </details>

    Более подробную информацию о написании тестов в спринг бут приложениях можно почитать в
    документации [8.3. Testing Spring Boot Applications](https://docs.spring.io/spring-boot/docs/3.1.0/reference/html/features.html#features.testing.spring-boot-applications).

### Конфиги

1. Переименовать настройки в файле `service.properties`.

    <details>
        <summary>Список настроек, которые нужно переименовать</summary>

    - `serviceName` -> `spring.application.name`
    - `http.cache.sizeInMB` -> `http.cache.sizeInMb`
    - `consul.wait.after.deregistration.millis` -> `consul.wait-after-deregistration-millis`
    - `statsd.queue.size` -> `statsd.queue-size`
    - `statsd.buffer.pool.size` -> `statsd.buffer-pool-size`
    - `jetty.host` -> `server.address`
    - `jetty.port` -> `server.port`
    - `jetty.maxThreads` -> `server.jetty.threads.max`
    - `jetty.minThreads` -> `server.jetty.threads.min`
    - `jetty.queueSize` -> `server.jetty.threads.max-queue-capacity`
    - `jetty.threadPoolIdleTimeoutMs` -> `server.jetty.threads.idle-timeout`
    - `jetty.acceptors` -> `server.jetty.threads.acceptors`
    - `jetty.selectors` -> `server.jetty.threads.selectors`
    - `jetty.connectionIdleTimeoutMs` -> `server.jetty.connection-idle-timeout`
    - `jetty.acceptQueueSize` -> `server.jetty.accept-queue-size`
    - `jetty.requestHeaderSize` -> `server.max-http-request-header-size`
    - `jetty.responseHeaderSize` -> `server.jetty.max-http-response-header-size`
    - `jetty.outputBufferSize` -> `server.jetty.output-buffer-size`
    - `jetty.stopTimeoutMs` -> `spring.lifecycle.timeout-per-shutdown-phase`
    </details>

2. Удалить настройку `jetty.session-manager.enabled` из файла `service.properties`.

3. После миграции на Spring Boot немного увеличивается потребление метаспейса (приблизительно на 10-15 мб). В связи с этим необходимо удостовериться,
   что у приложения есть запас метаспейса. Если запаса нет - рекомендуется увеличить размер метаспейса.

[//]: # (TODO: поизучать какие еще есть настройки в буте для конфигурации веб приложений: webAppContext, servletContext, jettyServer и тд. Возможно в буте есть настройки, которых не было в набе, и в сервисах мб можно будет удалить какие-то куски кода)

### Автоконфигурации

В набе выключено подавляющее большинство автоконфигураций, предоставляемых спринг бутом из коробки. Это было сделано для того, чтобы облегчить
миграцию сервисов на спринг бут и снизить вероятность появления неожиданных неочевидных ошибок (с небольшим количеством включенных автоконфигураций
поведение сервисов при миграции становится более прогнозируемым). В дальнейшем планируется постепенное включение остальных автоконфигураций. Список
включенных автоконфигураций можно посмотреть на странице [Spring Boot Auto Configurations](spring-boot-auto-configurations.md).

Если при разработке вашего сервиса вам потребуется включить какие-то автоконфигурации, которые выключены на уровне наба, вы можете сделать это,
добавив файл `ru.hh.nab.web.starter.autoconfigure.AutoConfigurationWhitelist.imports` в директорию `src/main/resources/META-INF/spring` и перечислив в
нем необходимые вам автоконфигурации.

<details>
    <summary>Пример</summary>

```
org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration
org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration
```

</details>
