package ru.hh.nab.metrics;

import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.StatsDClient;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.anyLong;
import org.mockito.Mockito;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
    StatsDSender statsDSender = Mockito.spy(new StatsDSender(new NoOpStatsDClient(), Executors.newSingleThreadScheduledExecutor()));
    var metricName = "metricName";
    var tags = List.of(new Tag("test1", "value"), new Tag("test2", "value"));
    new TaggedSender(statsDSender, Set.copyOf(tags)).sendCount(metricName, 123);

    var metricNameCaptor = ArgumentCaptor.forClass(String.class);
    var tagCaptor1 = ArgumentCaptor.forClass(Tag.class);
    var tagCaptor2 = ArgumentCaptor.forClass(Tag.class);
    verify(statsDSender).sendCount(metricNameCaptor.capture(), anyLong(), tagCaptor1.capture(), tagCaptor2.capture());

    assertEquals(metricName, metricNameCaptor.getValue());
    assertNotEquals(tagCaptor1.getValue(), tagCaptor2.getValue());
    assertTrue(tags.contains(tagCaptor1.getValue()));
    assertTrue(tags.contains(tagCaptor2.getValue()));
  }

  @Test
  public void testCombinedTags() {
    var metricName = "metricName";
    var commonTag = new Tag("commonTag", "commonValue");

    var counterTagName = "counterTag";
    var counterTag1 = new Tag(counterTagName, "counterValue1");
    var counterTag2 = new Tag(counterTagName, "counterValue2");
    int value1 = 12;
    int value2 = 21;
    var counters = new Counters(2);
    counters.add(value1, counterTag1);
    counters.add(value2, counterTag2);

    var statsDClient = Mockito.mock(StatsDClient.class);
    var statsDSender = new StatsDSender(statsDClient, Executors.newSingleThreadScheduledExecutor());
    new TaggedSender(statsDSender, Set.of(commonTag)).sendCounters(metricName, counters);

    var expectedMetricName1 = "%s.%s_is_%s.%s_is_%s".formatted(metricName, counterTag1.name, counterTag1.value, commonTag.name, commonTag.value);
    var expectedMetricName2 = "%s.%s_is_%s.%s_is_%s".formatted(metricName, counterTag2.name, counterTag2.value, commonTag.name, commonTag.value);
    var expectedMap = Map.of(
        expectedMetricName1, (long) value1,
        expectedMetricName2, (long) value2
    );

    var metricNameCaptor = ArgumentCaptor.forClass(String.class);
    var valueCaptor = ArgumentCaptor.forClass(Long.class);
    verify(statsDClient, times(2)).count(metricNameCaptor.capture(), valueCaptor.capture());
    var metricNameCaptorValues = metricNameCaptor.getAllValues();
    var valueCaptorValues = valueCaptor.getAllValues();
    var actualMap = Map.of(
        metricNameCaptorValues.get(0), valueCaptorValues.get(0),
        metricNameCaptorValues.get(1), valueCaptorValues.get(1)
    );
    assertEquals(expectedMap, actualMap);
  }
}
