package ru.hh.nab.kafka.consumer;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.common.PartitionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicPartitionsMonitoring {


  private static final Logger LOGGER = LoggerFactory.getLogger(TopicPartitionsMonitoring.class);

  private final ClusterMetaInfoProvider clusterMetaInfoProvider;
  private final Map<Object, CallbackConfiguration> partitionsChangesCallbacks;
  private final ScheduledExecutorService executor;
  private ScheduledFuture<?> scheduledFuture;
  private Duration schedulingInterval;

  public TopicPartitionsMonitoring(ClusterMetaInfoProvider clusterMetaInfoProvider) {
    this(clusterMetaInfoProvider, Executors.newSingleThreadScheduledExecutor());
  }

  public TopicPartitionsMonitoring(
      ClusterMetaInfoProvider clusterMetaInfoProvider, ScheduledExecutorService executor
  ) {
    this.clusterMetaInfoProvider = clusterMetaInfoProvider;
    this.partitionsChangesCallbacks = new ConcurrentHashMap<>();
    this.executor = executor;
    schedulingInterval = Duration.ofMinutes(1);
    this.scheduledFuture = null;
  }

  public synchronized void startScheduling() {
    if (scheduledFuture != null) {
      return;
    }

    scheduledFuture = executor.scheduleAtFixedRate(
        this::tryRunCallbacks,
        schedulingInterval.toMillis(),
        schedulingInterval.toMillis(),
        TimeUnit.MILLISECONDS
    );
  }

  public void changeSchedulingInterval(Duration period) {
    boolean stopped = stopScheduledFuture();
    schedulingInterval = period;
    if (stopped) {
      startScheduling();
    }
  }

  private void tryRunCallbacks() {
    synchronized (this) {
      for (CallbackConfiguration callback : partitionsChangesCallbacks.values()) {
        try {
          Instant currentTime = Instant.now();
          if (callback.lastCheckAt.plus(callback.checkInterval).isAfter(currentTime)) {
            continue;
          }
          LOGGER.info("Check if partitions changed for {}", callback.topic);

          List<PartitionInfo> currentPartitions = clusterMetaInfoProvider.getPartitionsInfo(callback.topic);
          if (callback.prevPartitionsState.size() != currentPartitions.size()) {
            LOGGER.info("Got partitions change for topic {}", callback.topic);
            callback.callback.onConfigurationChanged(callback.prevPartitionsState, currentPartitions);
            callback.setPrevPartitionsState(currentPartitions);
          }
          callback.setLastCheckAt(currentTime);
        } catch (RuntimeException e) {
          LOGGER.error("Error while running callbacks for topic {}", callback.topic, e);
        }
      }
    }
  }


  void trackPartitionsChanges(Object registeredBy, String topic, Duration interval, List<PartitionInfo> currentPartitions, Callback callback) {
    this.partitionsChangesCallbacks.put(registeredBy, new CallbackConfiguration(topic, callback, interval, currentPartitions, Instant.now()));
  }

  void clearCallback(Object registeredBy) {
    this.partitionsChangesCallbacks.remove(registeredBy);
  }

  private boolean stopScheduledFuture() {
    if (scheduledFuture == null) {
      return false;
    }

    scheduledFuture.cancel(false);
    scheduledFuture = null;
    return true;
  }

  private static class CallbackConfiguration {
    public final String topic;
    public final Callback callback;
    public final Duration checkInterval;
    public List<PartitionInfo> prevPartitionsState;
    public Instant lastCheckAt;


    public CallbackConfiguration(
        String topic, Callback callback, Duration checkInterval, List<PartitionInfo> prevPartitionsState, Instant lastCheckAt
    ) {
      this.topic = topic;
      this.callback = callback;
      this.checkInterval = checkInterval;
      this.prevPartitionsState = prevPartitionsState;
      this.lastCheckAt = lastCheckAt;
    }

    public void setPrevPartitionsState(List<PartitionInfo> prevPartitionsState) {
      this.prevPartitionsState = prevPartitionsState;
    }

    public void setLastCheckAt(Instant nextCheckAfter) {
      this.lastCheckAt = nextCheckAfter;
    }
  }


  public interface Callback {
    void onConfigurationChanged(List<PartitionInfo> prevPartitions, List<PartitionInfo> newPartitions);
  }

}
