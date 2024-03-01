package ru.hh.nab.metrics;

/**
 * Округляет значения до степени двойки. Нужно для уменьшения количество значений в гистограмме
 */
public class RangedHistogram extends Histogram {

  public RangedHistogram(int maxHistogramSize) {
    super(maxHistogramSize);
  }

  @Override
  protected int calculateValue(int value) {
    return Integer.highestOneBit(value - 1) << 1;
  }

}

