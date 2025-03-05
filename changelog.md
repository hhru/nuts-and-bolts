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

* Метод `ConsumerConsumingState<T> getConsumingState()` переименован в `ConsumerContext<T> getConsumerContext()` и более не находится в публичном доступе.

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

## INSTRUCTIONS

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



