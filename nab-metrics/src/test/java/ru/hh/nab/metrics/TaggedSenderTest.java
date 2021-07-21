package ru.hh.nab.metrics;

import com.timgroup.statsd.NoOpStatsDClient;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.anyLong;
import org.mockito.Mockito;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;

public class TaggedSenderTest {

  @Test
  public void testGetTagStringOneTag() {
    var metricNameCaptor = ArgumentCaptor.forClass(String.class);
    var tagCaptor = ArgumentCaptor.forClass(Tag.class);
    StatsDSender statsDSender = Mockito.spy(new StatsDSender(new NoOpStatsDClient(), Executors.newSingleThreadScheduledExecutor()));
    doNothing().when(statsDSender).sendCount(metricNameCaptor.capture(), anyLong(), tagCaptor.capture());
    var metricName = "metricName";
    Tag tag = new Tag("test", "value");
    new TaggedSender(statsDSender, Set.of(tag)).sendCount(metricName, 123);
    assertEquals(metricName, metricNameCaptor.getValue());
    assertEquals(tag, tagCaptor.getValue());
  }

  @Test
  public void testGetTagStringTwoTags() {
    var metricNameCaptor = ArgumentCaptor.forClass(String.class);
    var tagCaptor = ArgumentCaptor.forClass(Tag.class);
    StatsDSender statsDSender = Mockito.spy(new StatsDSender(new NoOpStatsDClient(), Executors.newSingleThreadScheduledExecutor()));
    doCallRealMethod().when(statsDSender).sendCount(metricNameCaptor.capture(), anyLong(), tagCaptor.capture());
    var metricName = "metricName";
    var tags = List.of(new Tag("test1", "value"), new Tag("test2", "value"));
    new TaggedSender(statsDSender, Set.copyOf(tags)).sendCount(metricName, 123);
    assertEquals(metricName, metricNameCaptor.getValue());
    assertEquals(Set.copyOf(tags), Set.copyOf(tagCaptor.getAllValues()));
  }
}
