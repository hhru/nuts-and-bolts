package ru.hh.nab.metrics;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Percentiles {
  private Percentiles() {}

  public static Map<Integer, Integer> computePercentiles(Map<Integer, Integer> valueToCount, int... percentiles) {
    Arrays.sort(percentiles);

    int totalObservations = 0;
    for (int count : valueToCount.values()) {
      totalObservations += count;
    }

    Iterator<Map.Entry<Integer, Integer>> increasingValueToCountIterator = valueToCount
        .entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey())
        .iterator();
    int currentObservations = 0;
    int percentileIndex = 0;
    Map<Integer, Integer> percentileToValue = new HashMap<>();
    while (increasingValueToCountIterator.hasNext()) {
      Map.Entry<Integer, Integer> valueAndCount = increasingValueToCountIterator.next();
      currentObservations += valueAndCount.getValue();
      for (; percentileIndex < percentiles.length
          && totalObservations * percentiles[percentileIndex] / 100.0 <= currentObservations;
           percentileIndex++) {

        percentileToValue.put(percentiles[percentileIndex], valueAndCount.getKey());
      }
      if (percentileIndex == percentiles.length) {
        break;
      }
    }
    return percentileToValue;
  }

  public static Map<Integer, Long> computePercentiles(long[] values, int... percentiles) {
    Map<Integer, Long> result = new HashMap<>();
    Arrays.sort(values);

    for (int percentile : percentiles) {
      if (values.length == 0) {
        continue;
      }

      int index = (int) Math.rint((percentile / 100.0 * (values.length)) - 1);

      if (index < 0) {
        index = 0;
      } else if (index >= values.length) {
        index = values.length - 1;
      }

      result.put(percentile, values[index]);
    }

    return result;
  }
}
