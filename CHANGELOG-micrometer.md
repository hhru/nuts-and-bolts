# Changelog

Этот формат соответствует [Keep a Changelog](https://keepachangelog.com/ru/1.1.0/). Проект
придерживается [Семантического Версионирования](https://semver.org/lang/ru/spec/v2.0.0.html).

## [39.0.1.micrometer-alpha1] - 2026-08-24

### Изменено

- Обновлены библиотеки `hh-metrics` и `timing-logger`.
- В классах изменена сигнатура всех конструкторов и методов, которые принимали `StatsDSender` - теперь все они принимают `MetricsSender`.
- Класс `KafkaStatsDReporter` переименован в `KafkaMetricsReporter`.
- Класс `StatsDMetricsConsumer` переименован в `MetricsConsumerImpl`. Также из конструктора удален параметр `sendIntervalInSeconds`.
- В MetricsConsumerFactory настройка `sendIntervalSec` больше никак не обрабатывается.

### Инструкции

- В конструкторы и методы классов необходимо вместо `StatsDSender` передавать `MetricsSender`.
- Если в объект Properties, который передается в ConfigProvider, добавляется настройка `kafka.<cluster-name>.common.metric.reporters`, то необходимо в
  ней вместо класса `ru.hh.nab.kafka.monitoring.KafkaStatsDReporter` указывать `ru.hh.nab.kafka.monitoring.KafkaMetricsReporter`.
- Вместо класса `StatsDMetricsConsumer` необходимо использовать `MetricsConsumerImpl`. При создании объекта `MetricsConsumerImpl` параметр
  `sendIntervalInSeconds` передавать не нужно.
- Если в объект Properties, который передается в MetricsConsumerFactory, добавляется настройка `sendIntervalSec`, ее необходимо удалить.
