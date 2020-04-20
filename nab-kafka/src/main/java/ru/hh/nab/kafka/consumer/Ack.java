package ru.hh.nab.kafka.consumer;

public interface Ack {
  /**
   * Invoked when the record or batch for which the acknowledgment has been created has
   * been processed. Calling this method implies that all the previous messages in the
   * partition have been processed already.
   */
  void acknowledge();

  /**
   * Negatively acknowledge the record at an index in a batch - commit the offset(s) of
   * records before the index and re-seek the partitions so that the record at the index
   * and subsequent records will be redelivered after the sleep time. Must be called on
   * the consumer thread.
   * <p>
   * <b>When using group management,
   * {@code sleep + time spent processing the records before the index} must be less
   * than the consumer {@code max.poll.interval.ms} property, to avoid a rebalance.</b>
   *
   * @param index the index of the failed record in the batch.
   * @param sleepMs the time to sleep.
   * @since 2.3
   */
  void nack(int index, long sleepMs);
}
