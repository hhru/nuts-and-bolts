# Руководство по быстрому старту
**Java библиотека**: `nab-kafka`  
**Версия**: 11.0.0

---

## 🚀 Начните работу за 3 шага

### 1. **Требования**
Убедитесь, что у вас установлено следующее:
- **Java 17** или выше
- Инструмент сборки (например, Maven или Gradle)
- Apache Kafka (работающий локально или удаленно)
- **Spring Boot 3.1.0** или выше

---

### 2. **Добавление зависимости**
Добавьте `nab-kafka` в ваш проект:

#### **Maven**
Добавьте это в ваш `pom.xml`:
```xml  
<dependency>  
    <groupId>ru.hh.nab</groupId>  
    <artifactId>nab-kafka</artifactId>  
    <version>11.0.0</version>  
</dependency>  
```   

---

### 3. **Конфигурация Spring**
Бин `FileSettings` предоставляется библиотекой `nab-starter`. Вам нужно только настроить `ConfigProvider` и использовать его в вашем приложении.

#### **Шаг 1: Определение `service.properties`**
Создайте файл `service.properties` в вашем каталоге `deploy` репозитория:

```properties  
# SASL аутентификация  
kafka.site.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="USERNAME" password="PASSWORD";  
kafka.site.sasl.mechanism=PLAIN  
kafka.site.security.protocol=SASL_PLAINTEXT  

# Конфигурация consumer  
kafka.site.consumer.topic.area_state.default.auto.offset.reset=latest  
kafka.site.consumer.default.enable.auto.commit=false  
kafka.site.consumer.topic.area_state.default.fetch.max.wait.ms=5000  
kafka.site.consumer.topic.area_state.default.max.poll.interval.ms=75000  
kafka.site.consumer.topic.area_state.default.max.poll.records=50  
kafka.site.consumer.topic.area_state.default.enable.auto.commit=false  

# Конфигурация producer  
kafka.site.producer.default.client.id={{ name }}  
kafka.site.producer.default.retries=2  
kafka.site.producer.default.linger.ms=500  
kafka.site.producer.default.acks=all  
```  

#### **Шаг 2: Определение бинов `ConfigProvider` и `KafkaConsumerFactory`**
Создайте класс конфигурации Spring для инициализации `ConfigProvider` и `KafkaConsumerFactory`:

```java  
import org.springframework.context.annotation.Bean;  
import org.springframework.context.annotation.Configuration;  
import ru.hh.nab.kafka.consumer.DefaultConsumerFactory;  
import ru.hh.nab.kafka.consumer.KafkaConsumerFactory;  
import ru.hh.nab.kafka.util.ConfigProvider;  
import ru.hh.metrics.MetricsSender;  

@Configuration  
public class KafkaConfig {  

    @Bean  
    public ConfigProvider configProvider(FileSettings fileSettings, MetricsSender metricsSender) {  
        return new ConfigProvider(  
            "my-service", // Замените на имя вашего сервиса  
            "kafka-site", // Замените на имя вашего Kafka-кластера  
            fileSettings,  
            metricsSender // Передайте экземпляр MetricsSender для метрик  
        );  
    }  

    @Bean  
    public TelemetryAwareConsumerFactory kafkaSiteConsumerFactory(  
        ConfigProvider configProvider,  
        MetricsSender metricsSender,
        OpenTelemetry openTelemetry,
        KafkaHostsFetcher kafkaHostsFetcher  
    ) {  
        return new DefaultConsumerFactory(  
            configProvider,  
            new JacksonDeserializerSupplier(KafkaSiteObjectMapperFactory.createObjectMapper()),  
            metricsSender,
            openTelemetry,
            // Этот параметр является опциональным. Без него конфигурация bootstrap.servers должна быть явно задана в service.properties.
            // https://kafka.apache.org/documentation/#consumerconfigs_bootstrap.servers
            () -> kafkaHostsFetcher.get("KAFKA_SITE") // Получение bootstrap-серверов из Consul  
        );  
    }  
}  
```  

#### **Шаг 3: Создание и управление Kafka consumer**
Используйте `KafkaConsumerFactory` для создания Kafka consumer и управления их жизненным циклом с помощью `@PostConstruct` и `@PreDestroy`:

```java  
import org.springframework.stereotype.Service;  
import ru.hh.nab.kafka.consumer.KafkaConsumer;  
import ru.hh.nab.kafka.consumer.KafkaConsumerFactory;  
import ru.hh.nab.kafka.consumer.ConsumeStrategy;  
import ru.hh.nab.kafka.consumer.MessageProcessor;  

import javax.annotation.PostConstruct;  
import javax.annotation.PreDestroy;  

@Service  
public class KafkaService {  

    private final KafkaConsumerFactory kafkaConsumerFactory;  
    private final String topicName = "mailer-topic";  
    private KafkaConsumer<MailMessage> kafkaConsumer;  

    public KafkaService(KafkaConsumerFactory kafkaConsumerFactory) {  
        this.kafkaConsumerFactory = kafkaConsumerFactory;  
    }  

    @PostConstruct  
    public void subscribe() {  
        kafkaConsumer = kafkaConsumerFactory  
            .builder(topicName, MailMessage.class)  
            .withOperationName("mailer_message_listener")  
            .withConsumeStrategy(ConsumeStrategy.atLeastOnceWithBatchAck(this::processMessage))  
            .build();  
        kafkaConsumer.start();  
    }  

    @PreDestroy  
    public void stop() {  
        if (kafkaConsumer != null) {  
            kafkaConsumer.stop();  
        }  
    }  

    private void processMessage(MailMessage message) throws InterruptedException {  
        // Обработка одного сообщения  
        System.out.println("Получено: " + message);  
    }  
}  
```  

---

## ⚙️ Настройка `service.properties`
Файл `service.properties` используется для настройки конфигураций Kafka. Смотри [пример](public_api.md#создание-configprovider) из документации API.

---

## 🛠️ Устранение неполадок
| Проблема                     | Решение                                                       |  
|-------------------------------|---------------------------------------------------------------|  
| `ClassNotFoundException`      | Убедитесь, что зависимость `nab-kafka` добавлена правильно.    |  
| `NoSuchMethodError`           | Проверьте наличие конфликтов версий в ваших зависимостях.      |  
| Проблемы с подключением Kafka | Убедитесь, что Kafka работает и `bootstrap.servers` указан правильно. |  
| Ошибка SASL-аутентификации    | Проверьте `username` и `password` в `kafka.site.sasl.jaas.config`. |  
| Недопустимые ключи конфигурации | Убедитесь, что все ключи в `service.properties` допустимы и поддерживаются. |  

---

## ❓ Нужна помощь?
- **Документация API**: [public_api.md](public_api.md)
- **Поддержка**: platform-team@hh.ru