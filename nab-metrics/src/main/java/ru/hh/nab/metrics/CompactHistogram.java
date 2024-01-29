package ru.hh.nab.metrics;

/**
 * This class represents a histogram which compacts recorded metrics of a certain interval into a single value
 * to avoid bloating memory with unique values by sacrificing precision.
 */
public class CompactHistogram extends Histogram {

  /**
   * Controls the size of a cluster and precision. Value must be power of 2.
   */
  private final int compactionRatio;

  public CompactHistogram(int histogramSize, int compactionRatio) {
    super(histogramSize);
    this.compactionRatio = Integer.highestOneBit(compactionRatio);
  }

  /**
   * Returns value after compaction procedure.
   * <p>
   * Recorded value 30 with compactionRatio of 32 will be compacted into cluster point 16.
   *
   * @param value recorded value
   * @return compacted value
   */
  @Override
  protected int calculateValue(int value) {
    return value == 0 ? 0 : ((value - 1) | compactionRatio - 1) + 1;
  }

}

