package ru.hh.nab.kafka.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

public class AckUtils {

  public static <T> Map<TopicPartition, OffsetAndMetadata> getLatestOffsetForEachPartition(Collection<ConsumerRecord<String, T>> messages) {
    return messages.stream().collect(
        Collectors.toMap(
            AckUtils::getMessagePartition,
            AckUtils::getOffsetOfNextMessage,
            BinaryOperator.maxBy(Comparator.comparingLong(OffsetAndMetadata::offset))
        )
    );
  }

  public static <T> OffsetAndMetadata getOffsetOfNextMessage(ConsumerRecord<String, T> message) {
    return new OffsetAndMetadata(message.offset() + 1);
  }

  public static <T> TopicPartition getMessagePartition(ConsumerRecord<String, T> message) {
    return new TopicPartition(message.topic(), message.partition());
  }
}
