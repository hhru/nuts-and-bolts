package ru.hh.nab.kafka.consumer;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.InterruptException;
import ru.hh.nab.kafka.exception.ConfigurationException;
import ru.hh.nab.kafka.util.AckUtils;

class InMemorySeekOnlyAck<T> implements Ack<T> {

  private final ConsumerContext<T> consumerContext;
  private final DeadLetterQueue<T> deadLetterQueue;

  public InMemorySeekOnlyAck(KafkaConsumer<T> kafkaConsumer) {
    this.consumerContext = kafkaConsumer.getConsumerContext();
    this.deadLetterQueue = kafkaConsumer.getDeadLetterQueue();
  }

  @Override
  public void acknowledge() {
    consumerContext.setWholeBatchCommited(true);
    acknowledge(consumerContext.getCurrentBatch());
  }

  @Override
  public void acknowledge(ConsumerRecord<String, T> message) {
    awaitFutureMessages();
    seek(message);
  }

  @Override
  public void nAck(ConsumerRecord<String, T> message) {
    if (deadLetterQueue == null) {
      throw new ConfigurationException("Attempt to send message into DLQ without proper configuration. See ConsumerBuilder#withDlq.");
    }

    consumerContext.addFutureMessage(deadLetterQueue.send(message), message);
  }

  @Override
  public void acknowledge(Collection<ConsumerRecord<String, T>> messages) {
    Map<TopicPartition, OffsetAndMetadata> offsets = AckUtils.getLatestOffsetForEachPartition(messages);

    awaitFutureMessages();
    offsets.forEach((partition, offset) -> consumerContext.seekOffset(partition, offset));
  }

  @Override
  public void nAck(Collection<ConsumerRecord<String, T>> messages) {
    if (deadLetterQueue == null) {
      throw new ConfigurationException("Attempt to send message into DLQ without proper configuration. See ConsumerBuilder#withDlq.");
    }

    for (ConsumerRecord<String, T> record : messages) {
      consumerContext.addFutureMessage(deadLetterQueue.send(record), record);
    }
  }

  @Override
  public void commit(Collection<ConsumerRecord<String, T>> messages) {
    throw new UnsupportedOperationException("commit is not supported for InMemorySeekOnlyAck");
  }

  @Override
  public void seek(ConsumerRecord<String, T> message) {
    awaitFutureMessages();
    consumerContext.seekOffset(AckUtils.getMessagePartition(message), AckUtils.getOffsetOfNextMessage(message));
  }

  @Override
  public void retry(ConsumerRecord<String, T> message, Throwable error) {
    throw new UnsupportedOperationException("Retry is not supported by InMemorySeekOnlyAck");
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
