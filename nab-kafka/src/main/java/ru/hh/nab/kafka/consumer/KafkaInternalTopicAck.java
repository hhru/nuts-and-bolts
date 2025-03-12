package ru.hh.nab.kafka.consumer;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.InterruptException;
import ru.hh.nab.kafka.exception.ConfigurationException;
import ru.hh.nab.kafka.util.AckUtils;

class KafkaInternalTopicAck<T> implements Ack<T> {

  private final ConsumerContext<T> consumerContext;
  private final Consumer<?, ?> nativeKafkaConsumer;
  private final RetryQueue<T> retryQueue;
  private final DeadLetterQueue<T> deadLetterQueue;

  public KafkaInternalTopicAck(
      KafkaConsumer<T> kafkaConsumer,
      Consumer<?, ?> nativeKafkaConsumer
  ) {
    this.deadLetterQueue = kafkaConsumer.getDeadLetterQueue();
    this.consumerContext = kafkaConsumer.getConsumerContext();
    this.nativeKafkaConsumer = nativeKafkaConsumer;
    this.retryQueue = kafkaConsumer.getRetryQueue();
  }

  @Override
  public void acknowledge() {
    awaitFutureMessages();
    nativeKafkaConsumer.commitSync();
    consumerContext.setWholeBatchCommited(true);
  }

  @Override
  public void acknowledge(ConsumerRecord<String, T> message) {
    TopicPartition partition = AckUtils.getMessagePartition(message);
    OffsetAndMetadata offsetOfNextMessageInPartition = AckUtils.getOffsetOfNextMessage(message);

    awaitFutureMessages();
    nativeKafkaConsumer.commitSync(Map.of(partition, offsetOfNextMessageInPartition));
    consumerContext.seekOffset(partition, offsetOfNextMessageInPartition);
  }

  @Override
  public void sendToDlq(ConsumerRecord<String, T> message) {
    if (deadLetterQueue == null) {
      throw new ConfigurationException("Attempt to send message into DLQ without proper configuration. See ConsumerBuilder#withDlq.");
    }

    consumerContext.addFutureMessage(deadLetterQueue.send(message), message);
  }

  @Override
  public void acknowledge(Collection<ConsumerRecord<String, T>> messages) {
    Map<TopicPartition, OffsetAndMetadata> latestOffsetsForEachPartition = AckUtils.getLatestOffsetForEachPartition(messages);

    awaitFutureMessages();
    nativeKafkaConsumer.commitSync(latestOffsetsForEachPartition);
    latestOffsetsForEachPartition.forEach(consumerContext::seekOffset);
  }

  @Override
  public void sendToDlq(Collection<ConsumerRecord<String, T>> messages) {
    if (deadLetterQueue == null) {
      throw new ConfigurationException("Attempt to send message into DLQ without proper configuration. See ConsumerBuilder#withDlq.");
    }

    for (ConsumerRecord<String, T> record : messages) {
      consumerContext.addFutureMessage(deadLetterQueue.send(record), record);
    }
  }

  @Override
  public void commit(Collection<ConsumerRecord<String, T>> messages) {
    awaitFutureMessages();
    nativeKafkaConsumer.commitSync(AckUtils.getLatestOffsetForEachPartition(messages));
  }

  @Override
  public void seek(ConsumerRecord<String, T> message) {
    TopicPartition partition = AckUtils.getMessagePartition(message);

    awaitFutureMessages();
    consumerContext.seekOffset(partition, AckUtils.getOffsetOfNextMessage(message));
  }

  @Override
  public void retry(ConsumerRecord<String, T> message, Throwable error) {
    if (retryQueue == null) {
      throw new ConfigurationException("Attempt to retry message without proper configuration. See ConsumerBuilder#withRetries.");
    }

    consumerContext.addFutureMessage(retryQueue.retry(message, error), message);
  }

  private void awaitFutureMessages() {
    try {
      consumerContext.getAllBatchFuturesAsOne().get();
      AckUtils.getLatestOffsetForEachPartition(consumerContext.getBatchFutureMessages()).forEach(consumerContext::seekOffset);
    } catch (InterruptedException e) {
      throw new InterruptException(e);
    } catch (ExecutionException | CancellationException e) {
      throw e.getCause() instanceof KafkaException ? (KafkaException) e.getCause() : new KafkaException(e);
    }
  }
}
