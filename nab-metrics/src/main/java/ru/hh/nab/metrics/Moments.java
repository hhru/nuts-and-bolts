package ru.hh.nab.metrics;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Central moments aggregator.
 *
 * References:
 * "Simpler Online Updates for Arbitrary-Order Central Moments." http://arxiv.org/abs/1510.04923
 */
public class Moments {

  private static final AtomicReferenceFieldUpdater<Moments, MomentsData> UPDATER =
      AtomicReferenceFieldUpdater.newUpdater(Moments.class, MomentsData.class, "data");

  private volatile MomentsData data = new MomentsData();

  public void update(double value) {
    data.n += 1;
    data.max = data.n > 1 ? Double.max(data.max, value) : value;
    data.min = data.n > 1 ? Double.min(data.min, value) : value;
    double delta = value - data.mean;
    double deltaN = delta / data.n;
    data.mean += deltaN;
    data.variance += delta * (delta - deltaN);
  }

  public void merge(Moments moments) {
    MomentsData newData = new MomentsData();
    MomentsData left, right;

    do {
      left = UPDATER.get(this);
      right = UPDATER.get(moments);

      newData.n = left.n + right.n;
      if (left.n != 0 || right.n != 0) {
        newData.max = left.n == 0 ? right.max : right.n == 0 ? left.max : Double.max(left.max, right.max);
        newData.min = left.n == 0 ? right.min : right.n == 0 ? left.min : Double.min(left.min, right.min);
      }
      double delta = right.mean - left.mean;
      double deltaN = newData.n == 0 ? 0.0f : delta / newData.n;
      newData.mean = left.mean + deltaN * right.n;
      newData.variance = left.variance + right.variance + delta * deltaN * left.n * right.n;
    } while (!UPDATER.compareAndSet(this, left, newData));
  }

  public MomentsData getAndReset() {
    return UPDATER.getAndSet(this, new MomentsData());
  }

  public static class MomentsData {
    private int n;
    private double max;
    private double min;
    private double mean;
    private double variance;

    public int getNumber() {
      return n;
    }

    public double getMax() {
      return max;
    }

    public double getMin() {
      return min;
    }

    public double getMean() {
      return mean;
    }

    public double getVariance() {
      return n == 0 ? 0.0f : variance / n;
    }
  }
}
