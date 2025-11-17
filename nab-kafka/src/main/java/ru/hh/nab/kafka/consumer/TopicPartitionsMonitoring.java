package ru.hh.nab.kafka.consumer;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.kafka.common.PartitionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicPartitionsMonitoring {

  private static final Logger LOGGER = LoggerFactory.getLogger(TopicPartitionsMonitoring.class);

  private final ClusterMetadataProvider clusterMetadataProvider;
  private final ScheduledExecutorService executor;

  public TopicPartitionsMonitoring(ClusterMetadataProvider clusterMetadataProvider) {
    this(clusterMetadataProvider, Executors.newSingleThreadScheduledExecutor());
  }

  public TopicPartitionsMonitoring(
      ClusterMetadataProvider clusterMetadataProvider, ScheduledExecutorService executor
  ) {
    this.clusterMetadataProvider = clusterMetadataProvider;
    this.executor = executor;
  }

  /**
   * @param actualPartitionsProvider should provide actual list in multi-thread environment
   */
  public ScheduledFuture<?> subscribeOnPartitionsChange(
      String topic, Duration checkInterval, Supplier<List<PartitionInfo>> actualPartitionsProvider, Consumer<List<PartitionInfo>> onPartitionsChange
  ) {
    List<PartitionInfo>[] partitionsSnapshot = new List[]{actualPartitionsProvider.get()};
    return executor.scheduleAtFixedRate(
        () -> {
          try {
            List<PartitionInfo> newPartitions = clusterMetadataProvider.getPartitionsInfo(topic);
            if (newPartitions.size() == partitionsSnapshot[0].size()) {
              return;
            }
            LOGGER.info("Got partitions change for topic prev={}, new={}", actualPartitionsProvider.get().size(), newPartitions.size());
            onPartitionsChange.accept(newPartitions);
            partitionsSnapshot[0] = actualPartitionsProvider.get();
          } catch (RuntimeException e) {
            LOGGER.error("Error while running partitions monitoring", e);
          }
        },
        checkInterval.toMillis(),
        checkInterval.toMillis(),
        TimeUnit.MILLISECONDS
    );
  }
}
