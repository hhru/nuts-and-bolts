package ru.hh.nab.kafka.consumer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import static java.util.stream.Collectors.toMap;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.listener.ContainerAwareBatchErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;

class SeekToFirstNotAckedMessageErrorHandler<T> implements ContainerAwareBatchErrorHandler {

  private final ThreadLocal<BackOffExecution> backOffs = new ThreadLocal<>();

  private final ThreadLocal<Long> lastInterval = new ThreadLocal<>();

  private final BackOff backOff;
  private final KafkaConsumer<T> kafkaConsumer;

  public SeekToFirstNotAckedMessageErrorHandler(BackOff backOff, KafkaConsumer<T> kafkaConsumer) {
    this.backOff = backOff;
    this.kafkaConsumer = kafkaConsumer;
  }

  @Override
  public void handle(Exception thrownException, ConsumerRecords<?, ?> data, Consumer<?, ?> consumer,
                     MessageListenerContainer container) {

    List<ConsumerRecord<String, T>> currentBatch = kafkaConsumer.getCurrentBatch();
    if (!currentBatch.isEmpty()) {
      LinkedHashMap<TopicPartition, OffsetAndMetadata> offsetsToSeek = currentBatch.stream().collect(toMap(
          record -> new TopicPartition(record.topic(), record.partition()),
          record -> new OffsetAndMetadata(record.offset()),
          (offset1, offset2) -> offset1,
          LinkedHashMap::new
      ));

      Optional.ofNullable(kafkaConsumer.getLastAckedBatchRecord()).ifPresent(lastAckedBatchRecord -> {
        for (ConsumerRecord<String, T> record : kafkaConsumer.getCurrentBatch()) {
          offsetsToSeek.put(new TopicPartition(record.topic(), record.partition()), new OffsetAndMetadata(record.offset() + 1));
          if (record == lastAckedBatchRecord) {
            break;
          }
        }
      });

      offsetsToSeek.forEach(consumer::seek);
    }

    if (this.backOff != null) {
      BackOffExecution backOffExecution = this.backOffs.get();
      if (backOffExecution == null) {
        backOffExecution = this.backOff.start();
        this.backOffs.set(backOffExecution);
      }
      Long interval = backOffExecution.nextBackOff();
      if (interval == BackOffExecution.STOP) {
        interval = this.lastInterval.get();
        if (interval == null) {
          interval = 0L;
        }
      }
      this.lastInterval.set(interval);
      if (interval > 0) {
        try {
          Thread.sleep(interval);
        } catch (@SuppressWarnings("unused") InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }

    throw new KafkaException("Seek to current after exception", thrownException);
  }

  @Override
  public void clearThreadState() {
    this.backOffs.remove();
    this.lastInterval.remove();
  }

}
