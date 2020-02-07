package ru.hh.nab.metrics;

import java.util.function.IntSupplier;

/**
 * Maintains a separate {@link Histogram} for each combination of tags
 * Changes max allowed size dynamically
 */
public class AdjustingHistograms extends Histograms {

  private final IntSupplier maxNumOfHistogramsProvider;
  /**
   * @param maxHistogramSize   an upper limit on the number of different metric values. See {@link Histogram#Histogram(int)}.
   * @param numOfHistogramLimit an upper limit on the number of histograms to avoid memory overflow<br/>
   * @param maxNumOfHistogramsProvider a dynamic limit on the number of histograms. should work fast, because will be called on each save
   *
   */
  public AdjustingHistograms(int maxHistogramSize, int numOfHistogramLimit, IntSupplier maxNumOfHistogramsProvider) {
    super(maxHistogramSize, numOfHistogramLimit);
    this.maxNumOfHistogramsProvider = maxNumOfHistogramsProvider;
  }

  @Override
  protected int getMaxNumOfHistograms() {
    return Math.min(maxNumOfHistogramsProvider.getAsInt(), super.getMaxNumOfHistograms());
  }
}
