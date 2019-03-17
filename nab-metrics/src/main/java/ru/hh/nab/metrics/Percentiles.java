package ru.hh.nab.metrics;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Percentiles {

  private final int[] percentiles;

  public Percentiles(int... percentiles) {
    Arrays.sort(percentiles);
    this.percentiles = percentiles;
  }

  public Map<Integer, Integer> compute(Map<Integer, Integer> valueToCount) {
    int totalObservations = 0;
    for (int count : valueToCount.values()) {
      totalObservations += count;
    }

    Iterator<Map.Entry<Integer, Integer>> increasingValueToCountIterator = valueToCount.entrySet().stream()
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

}
