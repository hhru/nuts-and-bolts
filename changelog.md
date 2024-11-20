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



