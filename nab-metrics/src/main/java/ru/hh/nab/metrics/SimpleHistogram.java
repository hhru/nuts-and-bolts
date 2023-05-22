package ru.hh.nab.metrics;

/**
 * Простая реализация. Сохраняет значения "как есть"
 */
public class SimpleHistogram extends Histogram {

  public SimpleHistogram(int maxHistogramSize) {
    super(maxHistogramSize);
  }

  @Override
  protected int calculateValue(int value) {
    return value;
  }

}

