package ru.hh.nab.kafka.producer;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

public class KafkaSendResult<V> {

  private final ProducerRecord<String, V> producerRecord;
  private final RecordMetadata recordMetadata;

  public KafkaSendResult(ProducerRecord<String, V> producerRecord, RecordMetadata recordMetadata) {
    this.producerRecord = producerRecord;
    this.recordMetadata = recordMetadata;
  }

  public ProducerRecord<String, V> getProducerRecord() {
    return this.producerRecord;
  }

  public RecordMetadata getRecordMetadata() {
    return this.recordMetadata;
  }

  @Override
  public String toString() {
    return '{' +
        "producerRecord: " + producerRecord +
        ", recordMetadata: " + recordMetadata +
        '}';
  }
}
